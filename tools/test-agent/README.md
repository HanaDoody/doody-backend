# Doody Reward Test Agent

기존 Spring Boot 서비스에서 테스트 대상을 탐색하고, 테스트를 생성·실행한 뒤 실패 정보를 모델에 돌려주는 Node.js 기반 에이전트 실행기다. 현재 지원 범위는 `RewardPersistenceService`와 고정된 테스트 클래스 하나다. 임의 프로젝트를 자동 검증하는 범용 제품은 아니다.

## 구조

```text
모델 → action 선택 → 실행기에서 허용 여부 확인 → 도구 실행
  ↑                                             ↓
  └──────── 코드/Gradle 로그/JUnit 결과 ───────────┘
```

- `cli.mjs`: 세션 시작, 모델 응답 입력, API 자동 루프, 상태와 보고서 저장.
- `core.mjs`: 파일 탐색/읽기, 테스트 작성, Gradle 실행, JUnit 파싱과 실패 분류.
- `write_api_doc`: 테스트 통과와 관련 API 소스 확인 후 고정 경로에 명세를 저장하고 실행 근거를 첨부한다.
- `providers.mjs`: OpenAI Responses API 함수 호출 어댑터.
- `evaluate.mjs`: 정상 복사본과 선택한 결함 5종을 각각 실행하는 평가 도구.
- `*.test.mjs`: 실행기와 API 어댑터의 로컬 검증.

별도 npm 패키지는 필요 없다. Node.js 20 이상, JDK 21과 Gradle Wrapper를 사용한다. 첫 빌드는 Maven/Gradle 다운로드가 필요하다.

## 이번에 실제 수행한 방식

`session` 모드에서 현재 대화의 모델이 파일 내용을 확인하고 각 도구와 인수를 선택했다. CLI가 선택을 실행하고 결과를 파일로 돌려줬다. 테스트 소스는 고정 템플릿을 복사한 것이 아니라 이 세션에서 실제 엔티티와 저장소 인터페이스를 읽은 뒤 생성했다.

세션 모드는 파일로 모델을 연결하므로 모델 응답을 CLI에 전달하는 과정이 필요하다. API 키 없이 무인으로 모델이 호출되는 기능은 아니다. 실제 요청·응답 기록은 `.agent-runs/reward-demo/state.json`, 요청 자료는 `request.json`, 요약은 `report.md`에 있다.

이번 API 어댑터 검증은 가짜 HTTP 응답을 사용하는 테스트다. 실제 OpenAI API를 호출한 통합 검증은 수행하지 않았다.

## 실행: API 자동 모드

환경에 `JAVA_HOME`, `OPENAI_API_KEY`, `OPENAI_MODEL`을 설정한다. 키는 채팅이나 Git 파일에 넣지 않는다. 모델은 함수 호출을 지원하는 계정 내 모델 ID를 명시한다. 이 앱은 모델을 임의로 선택하지 않는다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
# OPENAI_API_KEY와 OPENAI_MODEL은 로컬 환경에서 설정
node tools/test-agent/cli.mjs auto --run my-reward-run --with-docs
```

자동 모드는 읽은 코드와 명세, 생성한 테스트, 도구 결과를 OpenAI API로 보낸다. 명시적으로 `auto`를 실행할 때만 API를 호출한다. 실행 전 전송할 저장소의 사용 권한과 내용을 확인한다. `store:false`를 사용하지만 로컬 실행 기록은 저장된다.

공식 요청 형식 참고: [OpenAI 함수 호출 문서](https://developers.openai.com/api/docs/guides/function-calling). 한 번에 한 도구를 실행하고 전체 `function_call`/`function_call_output` 이력을 다음 요청에 전달한다. 실제 API 비용·사용 가능 모델은 실행 계정에 따른다.

## 실행: 현재 채팅과 파일로 연결

```powershell
node tools/test-agent/cli.mjs start --run my-session --with-docs
```

모델이 `.agent-runs/my-session/request.json`을 읽고 다음 형식의 응답 JSON을 작성한다.

```json
{
  "tool": "read_file",
  "reason": "보상 서비스의 실제 분기를 확인한다",
  "path": "src/main/java/doody/spring/common/service/RewardPersistenceService.java",
  "content": "",
  "query": "",
  "summary": ""
}
```

```powershell
node tools/test-agent/cli.mjs step --run my-session --action .agent-runs/my-session/action.json
```

갱신된 `request.json`에는 이전 행동과 도구 결과가 포함된다. 모델은 이를 읽어 다음 행동을 정한다. 세션 응답 전달을 쉽게 하기 위한 동일한 단축 입력도 있다.

```powershell
node tools/test-agent/cli.mjs step --run my-session --tool read_file --path build.gradle --reason '의존성을 확인한다'
node tools/test-agent/cli.mjs step --run my-session --tool write_test --source .agent-runs/my-session/candidate.java.txt --reason '명세에 맞는 테스트를 생성한다'
```

`write_test` 전에 서비스와 동작 명세를 읽어야 한다. 위 명령은 예시이며 이 두 명령만으로 전 과정이 완료되지는 않는다. 모델은 엔티티 API도 조사하고 테스트를 작성한 뒤 `run_tests`를 호출한다. `--with-docs`를 지정하면 기존 HTTP 계약 테스트도 실행하고, API 관련 소스를 읽은 뒤 `write_api_doc`을 호출해야 종료할 수 있다. 생성 문서 경로는 `docs/api/collection-capture.md`다.

## 실행 규칙과 한계

- 모델의 쓰기 대상은 `src/test/java/doody/spring/agentgenerated/RewardPersistenceAgentTest.java`와 지정 API 문서다. 기존 파일이 있으면 덮어쓰기 전 내용을 실행 폴더에 백업한다.
- 서비스 구현은 수정하지 않는다. 경로 이탈과 외부로 이어지는 심볼릭 링크를 검사한다.
- 모델에는 임의 셸 실행 도구를 제공하지 않는다. 테스트 실행은 고정된 Gradle 명령과 클래스 필터를 사용한다.
- 최초 생성과 최대 2회 수정, 테스트 실행 최대 3회, 전체 행동 최대 20회다. Gradle 실행 제한은 회당 180초다.
- 실패 분류: 컴파일, 테스트 실패, 환경/빌드, 시간 초과, 실행 테스트 없음, 통과.
- 실행 결과를 읽기 전에는 완료할 수 없다. 테스트를 수정하면 이전 실행 결과는 무효화한다.
- 한 작업 폴더에서는 한 실행만 순차적으로 진행한다. 동시 세션과 프로세스 중단 후 재개는 지원하지 않는다.
- **파일 경로 제한은 Java 실행 샌드박스가 아니다.** 생성된 테스트와 Gradle은 로컬 코드 권한으로 실행된다. 신뢰할 수 있는 본인 저장소에서 사용한다.
- 테스트 실패가 제품 버그인지 잘못 생성된 테스트인지는 모델 보고와 사람이 확정한 요구사항을 함께 검토한다.

## 재현과 검증

```powershell
node --test tools/test-agent/core.test.mjs tools/test-agent/providers.test.mjs
node tools/test-agent/evaluate.mjs my-evaluation
.\gradlew.bat clean build --no-daemon --console=plain
```

평가 명령은 `.agent-runs/my-evaluation/` 아래 별도 프로젝트 복사본 6개를 만든다. 각 결함은 하나씩 독립적으로 적용된다. 기존 실행 이름은 재사용하지 않는다. 이미 실행한 평가의 XML을 다시 집계할 때만 다음 명령을 사용한다.

```powershell
node tools/test-agent/evaluate.mjs my-evaluation --refresh
```

API 자동 호출과 실패 후 모델의 자동 수정 경로는 실제 API 호출로 검증하지 않았다. 현재 실행에서는 처음 생성한 테스트부터 통과했고, 결함 평가를 더 정확히 하기 위해 대화 모델이 한 번 개선했다. 자동으로 실패를 고쳤다는 성과로 해석하면 안 된다.
