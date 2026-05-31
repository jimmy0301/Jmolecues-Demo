# CQRS Validation and Idempotency

## Context

Command 經常來自 HTTP、message queue 或外部系統。這些來源可能重送同一個 request，也可能只做 transport-level validation。若 validation 和冪等規則沒有分層，Command Handler 會變成雜湊場：既檢查 JSON、又檢查權限、又保護 domain invariant。

## Pattern

把 validation 分成三層：

| 層級 | 責任 |
|---|---|
| Controller / API DTO | 格式、必填、型別、基本字串長度 |
| Application Service | use case 是否可執行、reference 是否存在、權限與流程協調 |
| Domain Model | 永遠必須成立的 business invariant |

對外部可能重送的 command，定義固定語意：

- 使用 request id / command id 去重。
- 或明確定義重送時回傳既有結果、視為成功、或回傳固定錯誤。
- Event listener 同樣要能重複處理同一事件。

## Rules

- API validation 不取代 domain invariant。
- Command Handler 不接收 transport DTO。
- Query path 不觸發 command。
- Projection / read model 可以 eventual consistent，但必須可重建。
- 重複 command / event 不應造成重複 side effect。
