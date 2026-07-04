# AI 메시지 응답 가이드

## 공통 톤
- 언어는 한국어를 사용한다.
- 말투는 따뜻하고 간결한 반말을 사용한다.
- 사용자를 다그치지 않고, 지금 할 수 있는 작은 행동을 제안한다.
- 한 문장은 보통 20-45자 정도로 짧게 쓴다.
- 카드, 홈 화면, 토스트에 노출되는 문구는 1-2문장 안으로 줄인다.
- 금지 톤: 명령조, 훈계조, 과한 감탄, 차갑거나 기계적인 표현.
- 권장 어미: `해`, `됐어`, `괜찮아`, `해볼까?`, `이어가자`, `충분해`.

## 1. 온보딩 인트로
엔드포인트: `POST /onboarding/intro`

```json
{
  "direction_promise": "오늘 상태에 맞춰서 천천히 시작해볼게.",
  "message": "작은 리듬부터 붙잡으면 충분해."
}
```

길이 가이드:
- `direction_promise`: 20-45자
- `message`: 20-60자

## 2. 온보딩 추천 기간
엔드포인트: `POST /onboarding/recommend-period`

```json
{
  "recommended_period": "1w",
  "options": ["today", "1w", "1m", "3m"],
  "message": "처음엔 일주일 정도가 부담 없이 이어가기 좋아."
}
```

길이 가이드:
- `recommended_period`: `today`, `1w`, `1m`, `3m` 중 하나
- `message`: 25-70자

## 3. 온보딩 완료
엔드포인트: `POST /onboarding/complete`

```json
{
  "initial_ari": {
    "rhythm": 0.2,
    "autonomy": 0.3,
    "connection": 0.1
  },
  "goal": {
    "rhythm": 0.8,
    "autonomy": 0.8,
    "connection": 0.4
  },
  "period": "1w",
  "start_axis": "rhythm",
  "plan_summary": "먼저 아침 리듬을 만들고, 작은 자율 행동으로 이어가자.",
  "diagnostics": {}
}
```

길이 가이드:
- `start_axis`: `rhythm`, `autonomy`, `connection` 중 하나
- `plan_summary`: 35-90자

## 4. 아침 리듬
엔드포인트: `POST /rhythm/morning`

```json
{
  "reward": {
    "hanaMoney": 20
  },
  "greeting": "좋아. 오늘 아침 리듬이 기록됐어.",
  "collected_dudy": []
}
```

길이 가이드:
- `greeting`: 20-50자

## 5. 저녁 리듬
엔드포인트: `POST /rhythm/evening`

```json
{
  "reward": {
    "hanaMoney": 30
  },
  "reply": "오늘도 잘 마무리했어. 이 기록이 내일의 기준이 될 거야.",
  "signals": "{\"mood\":\"calm\",\"fatigue\":\"medium\"}",
  "collected_dudy": []
}
```

길이 가이드:
- `reply`: 35-90자
- `signals`: JSON 문자열 또는 `null`

## 6. 오늘의 미션 추천
엔드포인트: `POST /mission/recommend`

### 미션이 있는 경우

```json
{
  "mission_state": "active",
  "mission": {
    "id": "mission_id",
    "mission_id": "mission_id",
    "axis": "AUTONOMY",
    "stage": 1,
    "waypoint": "start",
    "difficulty": 1,
    "delta": {
      "rhythm": 0.0,
      "autonomy": 0.01,
      "connection": 0.0
    },
    "title": "책상 위 한 가지만 정리하기",
    "description": "지금 보이는 물건 하나만 제자리로 옮겨봐.",
    "mission_type": "ACTION",
    "required_count": 1,
    "is_signature": false,
    "is_fallback": false,
    "fallback_mission_id": null,
    "goal_tags": [],
    "how_to": [
      "가장 가까운 물건 하나 고르기",
      "제자리로 옮기기"
    ],
    "reason": "작은 정리는 오늘의 자율감을 다시 켜는 데 도움이 돼."
  },
  "fallback": null,
  "rest_message": null,
  "unlocked_contacts": [],
  "v_rhythm": 0.6,
  "diagnostics": {}
}
```

길이 가이드:
- `mission.title`: 12-28자
- `mission.description`: 30-80자
- `mission.reason`: 30-80자
- `how_to[]`: 2-4개, 각 15-35자

### 미션을 쉬어야 하는 경우

```json
{
  "mission_state": "gated",
  "mission": null,
  "fallback": null,
  "rest_message": "오늘은 행동 미션보다 리듬만 붙잡아도 괜찮아.",
  "unlocked_contacts": [],
  "v_rhythm": 0.24,
  "diagnostics": {
    "gate": 0
  }
}
```

길이 가이드:
- `rest_message`: 25-60자

## 7. 미션 완료
엔드포인트: `POST /mission/complete`

```json
{
  "updated_ari": {
    "rhythm": 0.8,
    "autonomy": 0.35,
    "connection": 0.1
  },
  "applied_delta": {
    "rhythm": 0.0,
    "autonomy": 0.03,
    "connection": 0.0
  },
  "eta": 1.0,
  "completed": true,
  "signature_available": false,
  "signature_completed": false,
  "contact_unlocked": false,
  "collected_dudy": [],
  "unlocked_contacts": [],
  "message": "좋아. 작은 행동 하나가 오늘의 자율감을 조금 열었어."
}
```

길이 가이드:
- `message`: 30-70자

## 8. 미션 거절
엔드포인트: `POST /mission/reject`

```json
{
  "action": "downshift",
  "message": "괜찮아. 오늘은 더 작은 버전으로 바꿔볼게.",
  "rest_option": true,
  "candidates": [],
  "diagnostics": {
    "rejected_mission_id": "mission_id"
  }
}
```

길이 가이드:
- `action`: `downshift`, `replace`, `rest` 사용 권장
- `message`: 25-60자

## 9. 채팅 메시지
엔드포인트: `POST /chat/message`

```json
{
  "reply": "오늘은 기준을 낮춰도 괜찮아. 지금 할 수 있는 가장 작은 행동 하나만 골라볼까?",
  "signals": {},
  "suggested_action": "offer_downshift",
  "source": "AI"
}
```

길이 가이드:
- `reply`: 40-120자
- 2-3문장 이내로 쓴다.
- `suggested_action`: `offer_downshift`, `suggest_rest`, `encourage_start`, `none` 사용 권장

## 10. 리포트 요약
엔드포인트: `POST /report/summary`

```json
{
  "summary": "이번 기간에는 리듬 기록이 꾸준히 쌓였고, 작은 회복 행동도 함께 이어졌어.",
  "highlights": [
    "총 8개의 기록을 남겼어.",
    "활동한 날은 5일이야.",
    "리듬 축이 가장 안정적으로 이어졌어."
  ],
  "generatedAt": "2026-06-30T16:00:00"
}
```

길이 가이드:
- `summary`: 60-140자
- `highlights[]`: 2-4개, 각 20-50자

## 가장 중요한 UI 노출 필드

홈 화면과 미션 카드에서 가장 눈에 띄는 필드는 아래다.

- `mission.title`
- `mission.description`
- `mission.reason`
- `rest_message`

이 필드들은 짧고, 자연스럽고, 반말 톤이 일관되게 내려오도록 맞춘다.
