# Feature Toggle, Migration, and Backward Compatibility

## Context

Production feature 不只包含程式碼變更，也包含部署順序、資料形狀、API / event contract 相容性，以及 rollback 方式。
當多個版本的服務可能同時存在，或 consumer 升級速度不一致時，破壞性變更會造成 runtime failure。

## Pattern

### Feature Toggle

使用 feature toggle 隔離「部署」與「啟用」：

- toggle 判斷放在 application / infrastructure adapter，不放在 domain model
- domain invariant 不依賴 toggle 開關來維持正確性
- toggle 名稱要對應業務能力，不使用臨時 ticket id 作為永久名稱
- 每個 toggle 要有 owner、預期移除時間與預設值
- toggle 關閉時，舊流程仍需保持可用

### Expand and Contract Migration

資料或 contract 變更採用 expand and contract：

1. Expand：先新增欄位、collection、index 或 event 欄位，保持舊 consumer 可讀
2. Backfill：補齊既有資料，且 backfill 可以重跑
3. Dual read / dual write：過渡期同時支援新舊欄位或新舊格式
4. Switch：切換讀取路徑或啟用新功能
5. Contract：確認所有 consumer 升級後，再移除舊欄位或舊行為

Data migration 使用 Flamingock Change 管理，避免一次性手動腳本造成環境漂移。
Flamingock Change 必須是版本控管、可稽核、可重跑、可 rollback 或可補償的 migration 單位。

### Backward Compatibility

API、event、read model 與 public DTO 預設只做 additive change：

- 新增 optional 欄位通常相容
- 移除欄位、改欄位語意、改 enum 值、改必填性都是 breaking change
- event schema 需要 version 或明確相容策略
- consumer 不應因未知欄位失敗
- producer 不應要求所有 consumer 同步升級

## Rules

- Public API / Integration Event / Public DTO 的破壞性變更必須有 deprecation 流程。
- Migration 要能重跑，且要能觀察進度與失敗筆數。
- Data migration 使用 Flamingock；已部署 Change 不可修改，修正請新增下一個 Change。
- Flamingock Change 要有 `@Rollback` 或明確補償流程。
- Backfill 不直接繞過 Aggregate invariant；必要時透過 application use case 或明確的 migration adapter。
- 新增查詢欄位或 read model 欄位時，檢查 index 與既有資料預設值。
- Rollback 策略要在 PR 內說明：關閉 toggle、回復讀取路徑、停止 consumer 或保留 dual write。
- Contract 移除要等所有 consumer 升級完成，不能只看 producer 已部署。

## Review Questions

- 這個變更是否會讓舊版本 producer / consumer 無法共存？
- API 或 event 是否只做 additive change？
- 新欄位是否有預設值、backfill 或兼容讀取邏輯？
- 是否需要 Flamingock Change？Change 是否可重跑並具備 rollback / 補償策略？
- rollback 時是否會讀不到資料或重複處理事件？
- feature toggle 關閉時是否仍保留舊流程？
- toggle 何時移除？誰負責移除？
