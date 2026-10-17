# 빌드 및 기존 테스트 확인 결과

작성일: 2026-10-18 (Asia/Seoul)

이 문서는 에이전트 구현 전 기준점을 기록한 것이다. 후속 구현과 검증은 `../test-agent/implementation-log.md` 및 `../../tools/test-agent/README.md`에 정리한다.

## 결과

Java 21로 컴파일과 실행 JAR 생성에 성공했다. 원본 코드의 테스트는 11개 중 10개 통과, 1개 실패였다. 실패 원인을 분리하기 위해 테스트 전용 DB 설정을 추가한 뒤 `clean build`를 실행했고, 11개 모두 통과했다. 테스트를 삭제하거나 건너뛰지 않았다.

| 구분 | 결과 |
| --- | --- |
| 원본 커밋 | `232f9028c73f98bf78449216e88d68973e0ddd6e` |
| Java | 설치된 JDK 21.0.10 사용 (기본 PATH는 Java 17) |
| Gradle | 저장소 Wrapper의 9.5.1 |
| Spring Boot | 4.0.6 |
| 원본 전체 빌드 | 컴파일·패키징 성공, 테스트 단계 실패 |
| 원본 테스트 | 총 11개: 성공 10, 실패 1 |
| 테스트 설정 적용 후 | `clean build` 성공, 총 11개 성공 |

## 원인 및 변경

원본 `ApplicationTests.contextLoads`는 기본 `local` 프로필을 사용했다. DB_HOST가 설정되지 않아 PostgreSQL 접속에 실패했고, Hibernate가 DB 메타데이터를 읽지 못해 애플리케이션 초기화가 중단됐다. AWS 삭제가 이 실패의 직접 원인이라고 판정할 근거는 없었다.

변경한 파일:

- `build.gradle`: H2를 `testRuntimeOnly`로 추가했다. 배포 JAR에 H2를 추가하는 변경이 아니다.
- `src/test/resources/application-test.yml`: 메모리 H2 DB, 테스트 스키마 생성·제거 및 테스트용 S3 설정을 추가했다. 스키마 생성 실패를 놓치지 않도록 `halt_on_error`를 활성화했다.
- `src/test/java/doody/spring/ApplicationTests.java`: `test` 프로필을 명시하고 AI 주소를 테스트 속성으로 비워 warmup 외부 요청을 막았다.

프로덕션 서비스 로직과 운영 설정은 변경하지 않았다. 기존 개별 테스트 10개는 DB 접속 없이 통과했고, 나머지 컨텍스트 테스트 1개는 H2로 초기화됐다.

## 재실행

PowerShell에서 저장소 폴더로 이동한 다음 실행한다. 처음 실행할 때는 Gradle 및 Maven 의존성 다운로드가 필요하다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
.\gradlew.bat clean build --no-daemon --console=plain
```

다른 PC에서는 JAVA_HOME을 해당 PC의 JDK 21 경로로 바꾼다.

테스트 리포트: `build/reports/tests/test/index.html`

실행 JAR: `build/libs/spring-0.0.1-SNAPSHOT.jar`

## 증거와 해석의 한계

- `original-test-results.json`: 변경 전 테스트 XML에서 집계한 결과와 관찰한 실패 원인.
- `isolated-test-results.json`: 테스트 설정 적용 후 테스트 XML에서 집계한 결과.
- `behavior-spec.md`: 소스로 확인한 동작, 미확정 정책, 서비스 테스트 및 결함 탐지 평가 계획.

통과한 테스트는 앱 초기화 1개, 텍스트 정규화 7개, 리포트 JSON 직렬화 1개, 저녁 리듬 AI 클라이언트 2개다. 보상·수집·미션 완료의 서비스 테스트를 새로 구현하거나 해당 기능이 정상이라고 검증한 결과는 아직 아니다.

H2의 PostgreSQL 모드는 실제 PostgreSQL과 동일하지 않다. 현재 결과는 PostgreSQL 제약·동시성·롤백, Redis 기능, S3 업로드, 실제 AI 서버 연동, 안드로이드와의 통합 또는 배포 성공을 증명하지 않는다. JAR 생성 성공과 서버 운영 가능 여부도 구분한다.

현재 단계에서는 별도의 AI 에이전트 앱을 구현하지 않았다. 이번 작업은 에이전트 개발 전에 실행 가능한 기준점과 평가 명세를 확보한 단계다.

## 이어서 구현할 작업

1. `behavior-spec.md`의 R/C/M 시나리오 중 제품 의도와 일치하는 항목을 확정한다. P 항목은 미확정으로 유지한다.
2. 우선 보상 저장 기능에서 의미 있는 서비스 테스트와 검증 기준을 만든다.
3. 에이전트의 파일 탐색, 테스트 작성, Gradle 실행, XML 결과 분석 도구를 연결한다.
4. 실패 분류와 최대 2회의 테스트 수정 루프를 구현한다. 구현 오류로 의심되는 경우는 보고하고 자동 수정하지 않는다.
5. 정상 코드 및 별도 복사본에 주입한 결함에 대해 탐지 성능과 잘못된 지적을 평가한다.

최종 경험에는 직접 확정한 요구사항, 에이전트의 잘못된 가정, 수정한 설계, 실제 평가 결과를 함께 남긴다.
