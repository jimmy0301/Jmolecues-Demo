# Chapter 6：架構 Review Checklist

這份清單用於開 PR 前自查，也可作為 reviewer 檢查 DDD / CQRS / Onion Architecture 的順序。

---

## DDD Building Blocks

- 每個 building block 都有對應 jMolecules annotation
- Aggregate Root 保護 invariant，不暴露 setter 或可修改 collection
- Entity 隸屬於 Aggregate，不獨立建立 Repository
- ValueObject / Command 不可變
- Domain Service 只放不自然屬於單一 Aggregate 的領域邏輯

## Bounded Context

- 跨 Context 關聯使用 Reference Object，不 import 對方 Aggregate / ID 型別
- 讀取別的 Context 資料只透過公開查詢合約、read model 或 snapshot
- 更新別的 Context 只透過 owner 的公開 command 合約或事件
- 外部語言進入本 Context 前有 Anti-Corruption Layer
- Public DTO 不洩漏 owner 的 internal domain type

## CQRS

- Command 位於標記 `@ApplicationServiceRing` 的 package，命名是祈使語意
- Command Handler 位於標記 `@ApplicationServiceRing` 的 package，只協調載入、呼叫 domain method、儲存
- QueryModel 只讀，不呼叫 command handler、不 save/delete、不 publish event
- API DTO 在 controller 轉成 Command / Query 參數，不進 application / domain
- Validation 分層清楚：transport、use case、domain invariant 各在自己的位置

## Events and Workflows

- Domain Event 命名使用過去式，代表已發生事實
- 跨 Context event 當成 Integration Event / public contract 管理
- Listener 可重入且具備冪等性
- Projection 可重建，重複處理事件不產生重複資料
- 多步驟且有狀態的流程使用 Process Manager / Saga，不塞進普通 listener
- Process Manager / Saga 有 process id / correlation id、流程狀態、timeout、retry 與補償策略

## Concurrency

- 可被並發修改的 Aggregate 有 version / optimistic locking 策略
- Command 重送語意明確，且和並發策略一致
- 版本衝突有固定處理方式：回傳 conflict、重新載入套用規則，或要求重試
- 悲觀鎖只作例外策略，文件寫明 lock scope、timeout、deadlock handling 與測試
- MongoDB 若需悲觀鎖語意，使用 lease lock，不宣稱有 SQL row lock

## Compatibility and Migration

- Public API / Integration Event / public DTO 只做 additive change，或有 deprecation 流程
- 新欄位有預設值、backfill 或兼容讀取邏輯
- 資料變更採用 expand and contract，不要求所有服務同時部署
- Feature toggle 有 owner、預設值、移除條件與關閉時的舊流程
- Migration / backfill 可重跑，且不繞過 Aggregate invariant
- Data migration 使用 Flamingock Change；已部署 Change 沒有被修改
- Flamingock Change 有 rollback 或明確補償策略，且大量資料 migration 有 batching / observability
- Rollback 策略已描述：關閉 toggle、回復讀取路徑、停止 consumer 或保留 dual write

## Tests and Docs

- 新增 controller 時有同 package `@WebMvcTest`
- 新增 rule / pattern 時同步 `.codex/rules/`、`docs/`、`patterns/`
- 新增架構決策時補 `decisions/` 或 `docs/adr/`
- 新增跨專案可重用模式時補 `patterns/`
- Public contract 或 migration 策略變更時補相容性測試或 migration 驗證計畫
- Flamingock Change 補 `@Apply` / `@Rollback` 測試，必要時用 Testcontainers MongoDB 驗證
- 跑過 `mvn spotless:apply` 與必要 focused tests
- package rename 時同步檢查 package-info annotation，而不是只改資料夾名稱
