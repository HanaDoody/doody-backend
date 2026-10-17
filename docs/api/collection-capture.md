# 두디 잡기 API

`POST /api/collection/capture`

주변 핀에 대한 수집 요청을 처리한다. 이 문서는 에이전트가 Controller, DTO, 공통 응답/예외 처리, 서비스 소스와 테스트 실행 결과를 함께 참고해 작성했다. 확인된 HTTP 계약과 소스로만 관찰한 서비스 규칙을 구분한다.

## 요청

- Content-Type: `application/json`
- 실제 서버 주소는 배포 환경에 따라 설정한다. 아래 예시는 로컬 서버 기준이며 실행된 네트워크 요청 기록이 아니다.
- 이 엔드포인트가 요청 본문의 userId를 받는 것은 코드로 확인했으나, 인증·인가의 적절성은 이번에 검증하지 않았다.

| 필드 | 코드 타입 | 서비스가 확인하는 조건 |
| --- | --- | --- |
| userId | String | null·공백 불가, 존재하는 사용자 |
| pinId | String | null·공백 불가, 활성 핀 존재 |
| lat | BigDecimal | null 불가 |
| lng | BigDecimal | null 불가 |

```json
{
  "userId": "user-1",
  "pinId": "pin-1",
  "lat": 37.5,
  "lng": 127.0
}
```

```bash
curl -X POST http://localhost:8080/api/collection/capture \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-1","pinId":"pin-1","lat":37.5,"lng":127.0}'
```

MockMvc 테스트에서 위 필드가 서비스 요청 객체로 전달되는 것을 확인했다. 필드의 업무상 유효성 검사는 실제 수집 서비스를 실행해 검증한 것은 아니다.

## 응답

성공 시 HTTP 200이다. 다음 값은 **HTTP 계약 테스트에서 지정한 예시 데이터**다. 모든 실제 요청에서 포인트가 30이거나 ARI가 아래 값이라는 의미는 아니다.

```json
{
  "status": 200,
  "isSuccess": true,
  "message": "collection capture success.",
  "result": {
    "captureId": 1,
    "dudy": {
      "id": "d01",
      "tier": "common",
      "axis": "connection",
      "earnedReason": "nearby"
    },
    "reward": {"hanaMoney": 30},
    "updatedAri": {"rhythm": 0.8, "autonomy": 0.3, "connection": 0.4},
    "appliedDelta": {"rhythm": 0.0, "autonomy": 0.0, "connection": 0.05},
    "collectedDudy": [
      {"id":"d01","tier":"common","axis":"connection","earnedReason":"nearby"}
    ],
    "unlockedContacts": []
  }
}
```

| result 필드 | 의미 | 근거 |
| --- | --- | --- |
| captureId | 수집 기록 ID | DTO·응답 테스트 |
| dudy | 대표 두디 정보 | DTO·ID 응답 테스트 |
| reward.hanaMoney | 반환된 보상 금액 | DTO·응답 테스트 |
| updatedAri | 갱신된 rhythm/autonomy/connection | DTO·rhythm 응답 테스트 |
| appliedDelta | 변화량 | DTO·connection 응답 테스트 |
| collectedDudy | 수집한 두디 목록 | DTO·목록 ID 응답 테스트 |
| unlockedContacts | 해금한 연락처 목록 (id/title/axis) | DTO·빈 목록 응답 테스트 |

공통 응답의 JSON 필드는 `success`가 아니라 `isSuccess`다. MockMvc에서 실제 직렬화 결과를 확인했다. 표의 일부 중첩 필드는 DTO에서 확인한 것으로, 모든 필드 조합을 단언한 것은 아니다.

## 오류

| HTTP | message 예시 | 근거 |
| --- | --- | --- |
| 400 | pin is too far to capture. | 서비스가 400 예외를 던질 때 HTTP 400으로 매핑되는 계약 테스트 |
| 404 | user not found. | 서비스가 404 예외를 던질 때 HTTP 404로 매핑되는 계약 테스트 |
| 500 | internal server error. | 내부 예외 내용이 노출되지 않는 응답 계약 테스트 |

```json
{
  "status": 400,
  "isSuccess": false,
  "message": "pin is too far to capture.",
  "result": null
}
```

추가로 소스에서는 userId/pinId/위치 누락 시 400, 사용자 또는 활성 핀 부재 시 404, 계산 거리 100m 초과 시 400을 발생시킨다. 이번 HTTP 테스트는 실제 수집 서비스 대신 대역을 사용하므로 해당 업무 조건이 실제로 예외를 발생시키는지까지 확인하지 않는다. 잘못된 JSON과 본문 누락의 상태 코드는 이번 문서에서 확정하지 않는다.

## 검증 범위

이번 문서 생성 직전에 보상 서비스 테스트 20개와 HTTP 계약 테스트 4개, 총 24개를 실행했고 모두 통과했다.

- 보상 서비스: 0·음수 금액 저장 방지, 양수 금액과 출처 기록, 두디 중복 수집 방지, 기본값/명시 값, 기존·신규 연락처, 저장 실패 전파.
- HTTP 계약: 요청 역직렬화와 성공 응답 필드, 서비스 예외의 400/404 매핑, 예상하지 못한 예외의 500 응답.
- 보상 서비스와 수집 API 사이의 전체 호출 연결은 이번 테스트에서 통합 검증하지 않았다.

근거 파일:

- `src/main/java/doody/spring/collection/controller/CollectionCaptureController.java`
- `src/main/java/doody/spring/collection/service/CollectionCaptureService.java`
- `src/main/java/doody/spring/collection/dto/CollectionCaptureRequest.java`
- `src/main/java/doody/spring/collection/dto/CollectionCaptureResponse.java`
- `src/main/java/doody/spring/common/dto/ApiResponse.java`
- `src/main/java/doody/spring/common/exception/GlobalExceptionHandler.java`
- `src/test/java/doody/spring/agentgenerated/RewardPersistenceAgentTest.java`
- `src/test/java/doody/spring/collection/controller/CollectionCaptureApiContractTest.java`

## 미검증 범위

- 실제 DB·AI 서버·S3·안드로이드와의 통합, 동시 요청, 인증·인가.
- 수집 서비스의 거리 계산과 경계값, 좌표 범위 검증.
- 일부 응답 필드가 null인 경우와 여러 AI 응답 조합.
- 실제 배포 서버의 동작과 성능.
- 기존 수동 작업과 비교한 시간 단축률. 이번 작업에서 비교 측정하지 않았다.

OpenAPI 전체 스키마 자동 추출이나 전체 Notion 명세 동기화 기능은 구현하지 않았다. 현재 문서는 지정한 API 한 개에 대한 모델 생성 결과이며, 필수 섹션과 실행 근거를 실행기가 확인·첨부한다.


<!-- agent-evidence: {"tests":24,"failures":0,"testHash":"2447d6bb856dc7433c2a89526d75daeaf0affe6c1cfe2d629eb877e64105f5b3","runEndedAt":"2026-10-17T16:12:22.839Z"} -->
