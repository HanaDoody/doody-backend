# Spring Boot 프로젝트에 AI 테스트·문서화 에이전트 적용하기

> 안드로이드와 API를 연결하며 직접 오류를 확인하던 프로젝트에 자동 테스트를 추가하고, 실행 결과와 실제 코드를 참고해 API 명세를 작성하는 CLI 에이전트를 만들었다. 기존 Doody 프로젝트의 테스트와 문서화를 개선한 후속 개발 기록이다.

## 1. 시작은 API를 연결하며 반복하던 확인 작업이었다

Doody의 백엔드는 Spring Boot로 개발했다. 당시에는 안드로이드 화면에 기능을 하나씩 연결하면서 API가 정상적으로 동작하는지, 오류 상황에서는 어떤 응답이 오는지 직접 확인했다.

API 문서도 개발하면서 작성했다. Notion에 기능별 HTTP 메서드, 경로, 개발 상태를 정리하고 프론트엔드와 공유하는 방식이었다.

실제로 사용했던 [Notion API 명세서](https://merciful-dance-b7d.notion.site/API-32b5fa1834ae81308e52ccd29b032d68)에는 다음과 같은 API가 등록돼 있다.

| 기능 | 메서드 | 경로 |
| --- | --- | --- |
| 오늘의 미션 | GET | `/api/users/{userId}/missions/today` |
| 주변 두디 핀 조회 | GET | `/api/collection/pins` |
| 두디 잡기 | POST | `/api/collection/capture` |
| 자립ON 미션 완료 | POST | `/api/users/{userId}/autonomy/missions/{missionId}/complete` |

문서에는 백엔드 API와 AI 엔진의 엔드포인트 목록도 따로 정리돼 있다. API가 늘어날수록 구현, 앱 연결 확인, 오류 확인, 명세 작성을 함께 챙겨야 했다.

코드를 다시 열어보니 AI 클라이언트와 텍스트 처리에 관한 테스트는 일부 있었지만, 포인트와 수집 보상을 담당하는 서비스에는 테스트가 없었다. 예를 들어 화면에서 포인트가 지급되는 것을 확인해도 다음 조건까지 함께 확인했다고 보기는 어려웠다.

- 0이나 음수의 포인트는 저장되지 않는가?
- 이미 가진 두디를 다시 저장하지 않는가?
- 보상 금액뿐 아니라 출처와 출처 ID도 맞게 저장되는가?
- 이미 해금한 연락처의 정보를 덮어쓰지 않는가?

이 조건을 코드로 남겨두면 같은 기능을 수정했을 때 다시 확인할 수 있다. 여기에 API 문서 작성까지 연결해보기로 했다.

## 2. AI에게 맡길 작업의 경계를 정했다

목표는 다음 과정을 하나의 흐름으로 연결하는 것이었다.

```text
관련 코드와 기존 명세 읽기
           ↓
검증할 조건에 맞는 테스트 작성
           ↓
Gradle 실행 및 JUnit 결과 수집
           ↓
결과를 모델에 전달하고 다음 행동 결정
           ↓
Controller·DTO·예외 처리 확인
           ↓
검증 범위를 표시한 API 문서 작성
```

구현에는 Codex의 도움을 받았다. 모델이 선택한 도구를 로컬 실행기가 수행하고, 실행 결과를 다시 모델에 전달하도록 구성했다.

이번 실제 실행은 **현재 대화 모델과 JSON 파일로 연결하는 세션 모드**로 진행했다. 모델 응답을 CLI에 전달하는 과정이 있다. 별도의 모델 API를 무인으로 호출한 실행은 아직 하지 않았으며, OpenAI Responses API 어댑터는 모의 응답으로 검증했다.

초기 범위는 `RewardPersistenceService`의 테스트와 `POST /api/collection/capture` 문서 한 개로 잡았다. 전체 프로젝트의 모든 API를 한 번에 처리하는 범용 도구까지 구현한 것은 아니다.

## 3. 먼저 실행 가능한 테스트 환경을 확보했다

프로젝트는 Java 21을 사용하지만 기본 PATH의 Java는 17이었다. 설치돼 있던 JDK 21을 지정해 빌드를 실행했다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
.\gradlew.bat clean build --no-daemon --console=plain
```

기존 테스트 11개 중 10개가 통과했고, 전체 애플리케이션을 초기화하는 테스트 1개가 실패했다. 원인은 DB 설정값이 없는 상태에서 PostgreSQL에 접속하려 했기 때문이었다.

테스트용 H2를 추가하고, 컨텍스트 테스트에서 별도의 프로필을 사용하도록 했다.

```java
@SpringBootTest(properties = "AI_ENGINE_BASE_URL=")
@ActiveProfiles("test")
class ApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:doody-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        hbm2ddl:
          halt_on_error: true
```

위 YAML은 DB 설정 부분을 발췌한 것이다. 실제 테스트 설정에는 AWS 리전과 빈 버킷 설정도 포함했다. AI 주소는 테스트 속성으로 비워 외부 서버 warmup 요청을 막았다.

이 설정으로 기존 테스트 11개를 모두 통과시켰다. H2에서 초기화됐다는 사실과 실제 PostgreSQL의 동작 검증은 구분했다.

## 4. 모델에 코드를 읽고 실행할 도구를 제공했다

실행기는 Node.js 표준 라이브러리로 작성했다. 모델이 선택할 수 있는 행동을 정해두고, 하나의 행동을 실행한 결과를 다음 입력에 포함했다.

| 도구 | 역할 |
| --- | --- |
| `list_files` | 관련 Java 파일 찾기 |
| `read_file` | 서비스·엔티티·DTO·명세 읽기 |
| `write_test` | 지정된 테스트 파일 작성 |
| `run_tests` | Gradle 실행 후 결과 수집 |
| `write_api_doc` | 테스트 실행과 소스 확인 후 API 문서 작성 |
| `finish` | 결과와 한계를 정리하고 종료 |

모델이 반환하는 행동의 형식은 다음과 같다.

```json
{
  "tool": "read_file",
  "reason": "보상 서비스의 실제 분기와 저장 경로를 확인한다",
  "path": "src/main/java/doody/spring/common/service/RewardPersistenceService.java",
  "content": "",
  "query": "",
  "summary": ""
}
```

API 자동 모드의 루프를 발췌하면 다음과 같다. 세션 모드도 같은 실행 함수를 사용하며, 모델 응답을 파일로 전달한다.

```javascript
const next = await requestAction(state, {
  key: process.env.OPENAI_API_KEY
});

state.input.push(...next.output);

const result = await step(state, next.action, next.call);
```

`step()`에서는 선택한 도구를 실행하고 결과를 모델 입력에 추가한다.

```javascript
result = await executeAction(root, runDir, state, action);

state.input.push({
  type: 'function_call_output',
  call_id: call.call_id,
  output: JSON.stringify(result)
});
```

함수 호출 형식은 [OpenAI 공식 문서](https://developers.openai.com/api/docs/guides/function-calling)를 참고했다. 이 글의 자동 모드 코드는 구현 설명이며, 실제 API 호출 성공 기록을 의미하지 않는다.

쓰기 대상은 지정된 테스트 파일과 API 문서로 제한했다. 서비스 구현을 자동으로 수정하는 도구는 제공하지 않았다. 생성·수정은 최대 3회, 테스트 실행은 최대 3회, 전체 도구 행동은 최대 20회로 제한했다.

다만 이 경로 제한이 Java 실행 샌드박스를 제공하는 것은 아니다. 생성된 테스트와 Gradle은 로컬 코드 권한으로 실행되므로, 본인 저장소에서 검토하며 사용하는 구조다.

## 5. 저장된 값까지 확인하는 테스트를 만들었다

먼저 서비스에서 확인한 규칙을 R01~R08로 정리했다. 제품 정책이 불명확한 항목은 별도로 남겼다. 현재 코드가 그렇게 동작한다는 사실만으로 요구사항까지 확정하지 않기 위해서다.

모델은 서비스와 실제 엔티티·Repository를 읽은 뒤 테스트를 생성했다. Repository는 Mockito 대역으로 바꿔 실제 DB 없이 서비스의 분기와 저장 요청을 확인했다.

예를 들어 포인트 금액 검증은 다음과 같다. 아래 코드는 생성한 테스트에서 발췌했다.

```java
@ParameterizedTest
@NullSource
@ValueSource(ints = {0, -1, -100})
void R01_rejectsNullZeroAndNegativeAmounts(Integer amount) {
    service.earnPoint(user, amount, "mission", "MISSION", 17L);
    verifyNoInteractions(points);
}
```

양수일 때는 저장 호출뿐 아니라 저장 객체의 값도 검증했다.

```java
service.earnPoint(user, 30, "completed mission", "MISSION", 17L);

var captor = ArgumentCaptor.forClass(PointTransaction.class);
verify(points).save(captor.capture());

var saved = captor.getValue();
assertThat(saved.getUser()).isSameAs(user);
assertThat(saved.getAmount()).isEqualTo(30);
assertThat(saved.getTransactionType()).isEqualTo("EARN");
assertThat(saved.getReason()).isEqualTo("completed mission");
assertThat(saved.getSourceType()).isEqualTo("MISSION");
assertThat(saved.getSourceId()).isEqualTo(17L);
```

포인트가 지급됐더라도 출처 ID를 빠뜨리면 이후 적립 내역 추적에 문제가 생길 수 있다. 그래서 주요 필드를 함께 확인했다.

생성한 보상 서비스 테스트는 매개변수별 실행을 포함해 20개였다. 첫 생성본도 모두 통과했다. 이번 실행에서 실패한 테스트를 모델이 자동으로 고쳤다는 성과는 없다.

## 6. 실행 결과를 구조화해 모델에 돌려줬다

테스트 실행은 고정된 Gradle 명령과 클래스 필터로 수행했다. 아래는 실행 코드의 일부다.

```javascript
const args = [
  '-classpath',
  path.join(root, 'gradle/wrapper/gradle-wrapper.jar'),
  'org.gradle.wrapper.GradleWrapperMain',
  'test',
  '--no-daemon',
  '--console=plain',
  '--rerun-tasks'
];

if (!all) args.push('--tests', TEST_CLASS);
if (!all && includeApi) args.push('--tests', API_TEST_CLASS);
```

표준 출력과 종료 코드, JUnit XML을 함께 읽어 결과를 분류했다.

```javascript
export function classifyRun(exitCode, output, suites, timedOut = false) {
  if (timedOut) return 'timeout';
  if (/Execution failed for task ':compile(?:Test)?Java'|Compilation failed|error: cannot find symbol/.test(output)) return 'compilation';
  if (suites.some(s => s.failures || s.errors)) return 'test_failure';
  if (exitCode === 0 && suites.reduce((n, s) => n + s.tests - (s.skipped || 0), 0) > 0) return 'passed';
  if (exitCode === 0) return 'no_tests';
  return 'environment_or_build';
}
```

컴파일 실패와 테스트 단언 실패는 다음에 확인해야 할 내용이 다르다. 실행된 테스트가 없거나 모두 건너뛴 경우도 정상 검증으로 집계하지 않았다. 이전 실행 XML이 다음 실행의 결과로 섞이지 않도록 실행 전에 기존 XML을 제거했다.

## 7. 오류를 직접 넣어 테스트의 탐지 능력을 확인했다

정상 코드에서 통과하는 것만으로 테스트의 품질을 판단하기 어려웠다. 그래서 별도 코드 복사본에 오류를 하나씩 넣고 같은 테스트를 실행했다.

| 주입한 오류 | 결과 |
| --- | --- |
| 0·음수 포인트 거절 조건 제거 | 테스트 3개 실패 |
| 중복 수집 방지 조건 제거 | 테스트 1개 실패 |
| 기본 등급을 잘못된 상수로 변경 | 테스트 1개 실패 |
| 포인트 출처 ID 누락 | 테스트 1개 실패 |
| 연락처 축을 잘못된 값으로 변경 | 테스트 1개 실패 |

예를 들어 실제 코드의 다음 조건을 복사본에서 `amount == null`로 바꿨다.

```java
if (amount == null || amount <= 0) {
    return;
}
```

그 결과 0, -1, -100을 입력하는 테스트가 실패했다. 정상 코드 복사본에서는 20개가 모두 통과했다.

평가를 준비하며 중복 수집 테스트도 개선했다. 저장 대역이 기본값인 null을 반환하면, 조건을 제거했을 때 우연한 NullPointerException으로 실패할 수 있었다. 그래서 잘못된 저장이 발생하더라도 대역은 엔티티를 반환하도록 설정했다.

```java
lenient().when(collections.save(any(DoodyCollection.class)))
    .thenAnswer(call -> call.getArgument(0));
```

이제 중복을 허용했을 때 빈 결과를 기대한 단언이 실패한다. 테스트 대역의 우연한 예외보다, 확인하려는 규칙이 깨졌다는 근거가 명확해졌다.

결과 집계에서도 문제가 있었다. XML의 self-closing 성공 testcase와 다음 실패 testcase를 묶어 실패 이름을 잘못 표시하는 경우가 있어 파서를 수정했다. Mockito의 `NoInteractionsWanted`를 검증 실패로 집계하도록 보완한 뒤, 이미 실행한 XML을 다시 읽어 결과를 확정했다.

선택한 오류 5종은 모두 탐지됐다. 이 결과는 실제 운영 버그 5개를 발견했다는 의미도, 일반적인 오류 탐지율이 100%라는 의미도 아니다.

## 8. API 문서에 사용할 HTTP 계약 테스트를 추가했다

보상 서비스 테스트만으로 API 요청·응답 형식을 확인했다고 쓰기는 어렵다. 그래서 두디 잡기 API에 MockMvc 테스트 4개를 추가했다.

이 테스트에서는 실제 Controller와 공통 예외 처리기를 사용하고, `CollectionCaptureService`는 대역으로 바꿨다.

```java
mvc = MockMvcBuilders
    .standaloneSetup(new CollectionCaptureController(service))
    .setControllerAdvice(new GlobalExceptionHandler())
    .build();
```

성공 응답 테스트의 일부는 다음과 같다.

```java
mvc.perform(post("/api/collection/capture")
        .contentType(MediaType.APPLICATION_JSON)
        .content(BODY))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.status").value(200))
    .andExpect(jsonPath("$.isSuccess").value(true))
    .andExpect(jsonPath("$.result.reward.hanaMoney").value(30))
    .andExpect(jsonPath("$.result.dudy.id").value("d01"));
```

요청 필드의 역직렬화, 성공 응답의 주요 필드, 서비스가 던진 예외의 400·404 매핑, 내부 예외의 500 응답을 확인했다.

위 테스트의 30포인트는 대역이 반환한 예시 값이다. 실제로 모든 수집 요청에서 30포인트를 지급한다는 뜻은 아니다. 거리 제한과 실제 저장 흐름 역시 이 HTTP 테스트에서 통합 검증하지 않았다.

## 9. 테스트 결과와 실제 소스를 참고해 API 문서를 생성했다

문서 생성에는 테스트 결과와 함께 다음 소스를 읽도록 했다.

- Controller: HTTP 메서드, 경로, 요청 전달 방식.
- 요청·응답 DTO: 필드 이름과 타입.
- 서비스: 입력 확인과 오류 발생 조건.
- 공통 응답·예외 처리: 응답 구조와 메시지.

실행기는 테스트가 통과했는지, 관련 소스를 읽었는지 확인한 뒤 문서를 쓰도록 했다. 실제 구현을 발췌하면 다음과 같다.

```javascript
if (action.tool === 'write_api_doc') {
  if (state.lastRun?.classification !== 'passed') {
    throw new Error('API documentation requires a passing test execution');
  }
  if (API_SOURCES.some(file => !state.readFiles.includes(file))) {
    throw new Error('Read controller, service, DTOs, response envelope and exception handler first');
  }
  // 이후 필수 섹션과 출력 경로를 확인하고 문서를 저장한다.
}
```

필수 섹션은 요청, 응답, 오류, 검증 범위, 미검증 범위로 정했다. 문서 끝에는 실행 테스트 수, 실패 수, 테스트 소스 해시, 실행 종료 시각을 첨부했다.

이 조건은 문서를 작성할 근거와 형식을 확인하는 장치다. 모든 설명의 정확성을 자동으로 보장하는 것은 아니므로, 생성 결과를 검토하는 단계는 남는다.

생성한 문서의 요청 예시는 다음과 같다.

```http
POST /api/collection/capture
Content-Type: application/json
```

```json
{
  "userId": "user-1",
  "pinId": "pin-1",
  "lat": 37.5,
  "lng": 127.0
}
```

오류 설명도 근거를 함께 표시했다.

| HTTP | 메시지 예시 | 확인한 범위 |
| --- | --- | --- |
| 400 | `pin is too far to capture.` | 서비스의 400 예외를 HTTP 400으로 매핑 |
| 404 | `user not found.` | 서비스의 404 예외를 HTTP 404로 매핑 |
| 500 | `internal server error.` | 내부 예외 세부 정보를 노출하지 않는 응답 |

100m를 초과하면 거절하는 규칙은 서비스 소스에서 확인했다. 이번에 추가한 HTTP 테스트가 실제 거리 계산까지 확인한 것은 아니므로 문서에서도 이를 구분했다.

현재 자동 생성한 명세는 Markdown 파일 한 개다. 기존 Notion 전체 명세를 자동으로 갱신하거나 동기화하는 기능까지 구현하지는 않았다.

## 10. 결과: 반복 가능한 검증과 문서 작성 흐름을 확보했다

| 항목 | 결과 |
| --- | --- |
| 보상 서비스 테스트 | 20개 통과 |
| 새 HTTP 계약 테스트 | 4개 통과 |
| 기존 테스트 포함 전체 Java 테스트 | 35개 통과 |
| 실행기·API 어댑터 테스트 | 7개 통과, API는 모의 응답 |
| 선택한 결함 주입 평가 | 오류 5종 모두 탐지 |
| 생성한 API 문서 | 두디 잡기 API 1개 |

문서 생성 직전 실행한 보상·HTTP 테스트 24개는 실행기 기록 기준 약 15.4초에 완료됐다. 이 값은 해당 테스트 실행 단계의 소요 시간이며, 모델의 코드 분석·테스트 작성·검토·문서 작성 시간을 모두 합한 값은 아니다.

기존에는 앱에 API를 연결하며 오류를 확인하고, 명세도 개발자가 작성했다. 이번에는 보상 규칙을 반복 실행할 수 있는 테스트로 남겼고, 코드 탐색부터 테스트 실행·결과 취합·문서 초안 작성까지 연결했다. 이후 수정 작업에서도 같은 테스트를 다시 실행할 기반을 확보했다.

### 시간 단축률을 숫자로 쓰지 않은 이유

당시 수동 테스트와 문서 작성에 걸린 시간을 기록하지 않아, 이번 결과와 같은 조건으로 비교할 수 없었다. 따라서 “테스트·문서화 시간을 60% 단축했다”거나 “전체 개발 시간이 절반으로 줄었다”는 수치는 쓰지 않았다.

다음에는 같은 변경 과제를 대상으로 다음 시간을 각각 기록하려 한다.

| 비교 항목 | 수동 작업 | AI 활용 작업 |
| --- | --- | --- |
| 테스트 | 코드 조사 + 작성 + 실행 + 수정 | 모델 처리 + 실행 + 사람 검토 + 수정 |
| 문서 | 명세 조사 + 작성 + 검토 | 초안 생성 + 사람 검토 + 수정 |

계산식은 다음과 같다.

```text
단축률(%) = (수동 작업 시간 - AI 활용 후 시간) / 수동 작업 시간 × 100
```

테스트 개수와 문서 항목, 정확성 기준도 같게 맞춰야 한다. 모델 응답 시간만 수동 작업 전체 시간과 비교하면 검토·수정 시간을 누락하게 된다. 같은 과제를 반복하며 생기는 학습 효과도 구분할 필요가 있다.

현재 확인한 성과는 자동화 범위와 테스트 실행 결과다. 전체 개발 시간 절감률은 후속 측정 과제로 남겼다.

## 11. 돌아보며

처음에는 AI가 테스트 코드를 작성해주는 기능을 생각했다. 구현하면서 테스트를 실행하는 것, 실패 원인을 읽는 것, 결과를 정확하게 집계하는 것, 문서에 검증 범위를 표시하는 것까지 함께 다뤄야 한다는 점을 알게 됐다.

이번에는 테스트 대역의 null 반환과 XML 파서 문제처럼, 자동화 도구 자체에도 검증이 필요하다는 것을 확인했다. 테스트와 문서의 초안을 빠르게 만들 수 있도록 역할을 나누면서도, 무엇을 근거로 확인했다고 말할 수 있는지 기록하는 데 집중했다.

앞으로는 실제 모델 API를 연결한 자동 실행을 검증하고, PostgreSQL 연동 테스트와 동일 과제의 작업 시간 비교를 추가하려 한다. 이를 바탕으로 정확성과 작업 비용을 함께 평가할 계획이다.

---

### 코드 위치

기존 프로젝트: [HanaDoody/doody-backend](https://github.com/HanaDoody/doody-backend)

아래 파일은 이번 후속 작업에서 로컬에 추가·수정한 코드다. GitHub에 업로드하기 전까지 위 저장소의 main에 포함돼 있다고 가정하면 안 된다.

```text
tools/test-agent/
├── cli.mjs                 # 세션/API 모드와 실행 상태 관리
├── core.mjs                # 도구 실행, 테스트 결과 분석, 문서 저장
├── providers.mjs           # OpenAI Responses API 어댑터
├── evaluate.mjs            # 별도 복사본의 결함 주입 평가
├── core.test.mjs
└── providers.test.mjs

src/test/java/doody/spring/
├── agentgenerated/RewardPersistenceAgentTest.java
└── collection/controller/CollectionCaptureApiContractTest.java

docs/api/collection-capture.md
```

태그: `Spring Boot`, `AI`, `AI Agent`, `JUnit5`, `Mockito`, `테스트자동화`, `API문서화`
