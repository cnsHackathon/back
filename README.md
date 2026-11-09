# 안심콜 백엔드

모의 보이스피싱 통화 서버예요. Spring Boot 4.1 · Java 17 · H2(메모리) · Gemini API를 써요.
음성 인식과 합성은 프론트(브라우저)가 하고, 서버는 텍스트만 주고받아요.

## 실행

1. **Gemini 키 넣기.** `src/main/resources/application-local.yaml`을 만들고 아래처럼 적어요. 이 파일은 `.gitignore`에 있어서 깃에 올라가지 않아요.

   ```yaml
   gemini:
     api-key: 발급받은-키
   ```

   환경변수 `GEMINI_API_KEY`로 넣어도 돼요.

2. **서버 켜기.** `./gradlew bootRun`을 실행하거나 IntelliJ에서 `CnsApplication`을 실행해요. 주소는 `http://localhost:8080`이에요.
   - Swagger: `http://localhost:8080/swagger-ui.html`
   - 바로 호출해 보기: `http/api.http` (IntelliJ HTTP Client)

3. **프론트 연결.** front 폴더의 `.env.local`에 아래 두 줄을 넣고 `npm run dev`를 다시 켜요.

   ```
   VITE_USE_MOCK=false
   BACKEND_URL=http://localhost:8080
   ```

DB는 메모리 H2라서 서버를 끄면 기록이 사라져요.

## API

| Method | Path | 요청 | 응답 |
| --- | --- | --- | --- |
| POST | `/api/sessions` | 프로필 | `sessionId`, `timeLimitSec`, `firstTurn` |
| POST | `/api/sessions/{id}/start` | 없음 | `startedAt`, `deadlineAt`(ISO 문자열), `remainingSec` |
| POST | `/api/sessions/{id}/turns` | `{ userText, isFinal }` | `turnNo`, `speaker`(C/B), `text`, `ended`, `endReason`, `remainingSec` |
| POST | `/api/sessions/{id}/end` | `{ reason: HANG_UP \| TIMEOUT }` | `{ ended: true, endReason }` |
| GET | `/api/sessions/{id}/report` | 없음 | 리포트 (첫 요청 때 생성, 이후 저장본) |

오류는 모두 `{ "code", "message" }` 형식이고, `message`는 프론트 화면에 그대로 떠요.

| code | HTTP | 언제 |
| --- | --- | --- |
| `INVALID_REQUEST` | 400 | 필수값 누락, 정해진 값이 아님 |
| `SESSION_NOT_FOUND` | 404 | 없는 통화 |
| `SESSION_NOT_STARTED` | 409 | 받기 전에 답변을 보냄 |
| `SESSION_ENDED` | 409 | 끝난 통화에 답변을 보냄 |
| `SESSION_NOT_ENDED` | 409 | 통화 중에 리포트를 요청함 |
| `TURN_IN_PROGRESS` | 409 | 이전 답변을 아직 처리 중 |
| `LLM_UNAVAILABLE` | 503 | Gemini 호출 실패 (프론트가 재시도 버튼을 띄움) |

### 프론트와 맞춘 동작

- `/end`는 이미 끝난 통화에도 성공을 돌려줘요. 답변을 기다리는 사이 서버가 먼저 통화를 끝냈을 수 있기 때문이에요.
- 사용자 답변은 AI 응답이 성공했을 때만 저장해요. 그래서 실패 후 같은 답변으로 재시도해도 기록이 겹치지 않아요.
- 마감 뒤에 도착한 답변은 마지막 답변으로 받아요. 사기범이 마무리 인사를 하고 `ended: true`로 끝나요.
- 당한 경우 `scammedTurn`과 같은 턴의 항목이 `riskyMoments` 맨 앞에 있어요. 인용문(`quote`)은 서버가 실제 대화 기록에서 꺼내요.
- 시연할 때 시나리오를 고정하려면 세션 생성 요청에 `"scenarioId": "noshow-military-01"`을 넣어요. 프론트는 이 값을 보내지 않아요.

## 패키지와 담당

| 패키지 | 담당 | 내용 |
| --- | --- | --- |
| `global` | BE-1 | 설정, CORS, 오류 응답, 공통 도우미 |
| `session` | BE-1 | 프로필, 통화 생성·시작·종료, 10분 마감 |
| `conversation` | BE-2 | 사기범 발화 생성, 종료 판정, 단계(`CallPhase`) |
| `llm` | BE-2 | Gemini 호출, JSON 스키마, 프롬프트 템플릿 |
| `report` | BE-3 | 평가, 점수 계산, 리포트 저장 |
| `scenario` | BE-4 | 시나리오 읽기·검사·랜덤 배정 |
| `resources/prompts` | BE-4 | 사기범·평가 프롬프트 문구 |
| `resources/scenarios` | BE-4 | 시나리오 JSON |
| `resources/scoring` | BE-4 | 프로필 보정 점수표 |

## 시나리오 추가하기 (BE-4)

`src/main/resources/scenarios/`에 JSON 파일을 넣고 서버를 다시 켜면 돼요. 기존 파일을 복사해서 고치는 게 가장 빨라요.
서버가 시작할 때 아래 규칙을 검사하고, 틀리면 이유를 알려 주며 멈춰요.

- `id`: 소문자, 숫자, 하이픈 (예: `institution-police-02`)
- `subType`: `INSTITUTION`, `FAMILY_KIDNAP`, `FAMILY_CHILD`, `FAMILY_SPOUSE`, `LOAN`, `NOSHOW_PROXY_PURCHASE` 중 하나
- `personas`: 1~2명, 첫 번째 key는 `C`, 두 번째는 `B`. 프론트가 두 목소리만 구분해요.
- `checkpoints`: id는 대문자와 밑줄, weight 합은 100
- `eligibility`: 비우면 누구에게나 나와요. `jobs`(예: `["자영업자"]`), `requiresMarried`, `childAgeGroups`(예: `["중고등학생", "성인"]`)
- `tips`: 실전 대응 수칙. 평가 AI가 참고하고, 리포트 팁이 비면 이걸 보여 줘요.
- `sources`: 근거 기사·기관 안내. 발표 자료에 쓸 수 있게 꼭 남겨요.
- 조건 없이 누구에게나 나가는 시나리오가 최소 1개는 있어야 해요.

배정은 프로필로 후보를 거른 뒤 **유형을 먼저 고르게 뽑고**, 그 유형 안에서 시나리오를 뽑아요. 그래서 한 유형에 시나리오가 많아도 그 유형만 자주 나오지 않아요.

## 점수

- 당함: `min(49, 0.49 × S_pre + B)`. `S_pre`는 당한 턴보다 앞에서 달성한 체크포인트 가중치 합이에요.
- 안 당함: `min(100, 50 + 0.4 × S + B)`. `S`는 달성한 체크포인트 가중치 합이에요.
- `B`는 `scoring/difficulty.json`의 연령대×유형 보정(0~10)이에요. **지금 값은 임시값**이에요.

점수는 AI가 아니라 코드(`ScoreCalculator`)가 계산해요. AI는 판정 근거만 줘요.

## Gemini 설정 (`application.yaml`)

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `gemini.model` | `gemini-3.6-flash` | 기본 모델. 2026-11-09 측정에서 응답 약 1.8초 |
| `gemini.fallback-model` | `gemini-3.5-flash-lite` | 첫 시도가 실패하면 두 번째 시도에 쓰는 모델 |
| `gemini.turn.thinking-level` | `LOW` | 통화 중 응답 속도용. 생각 기능이 없는 모델이면 비워요. |
| `gemini.turn.max-output-tokens` | `2048` | 생각 토큰도 포함돼요. |
| `gemini.turn.timeout` | `20s` | 통화 중 한 번 호출을 기다리는 시간 |
| `gemini.report.*` | 모델 기본, `60s` | 리포트는 정확도가 중요해서 생각 수준을 낮추지 않아요. |
| `gemini.safety-threshold` | `BLOCK_ONLY_HIGH` | 사기범 역할극이 안전 필터에 막히지 않게 괴롭힘·위험 콘텐츠 기준을 완화해요. |

무료 티어 요청 한도는 AI Studio에서 확인할 수 있어요. 한도를 넘으면(429) 2초 뒤 한 번 더 시도하고, 그래도 실패하면 `LLM_UNAVAILABLE`을 돌려줘요.
