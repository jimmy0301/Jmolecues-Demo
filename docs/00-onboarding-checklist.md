# Chapter 0：新人上手檢查清單

這份清單用來確認新成員是否已經理解本專案的 DDD / CQRS / Onion Architecture 基本規則。
重點不是背熟所有 annotation，而是知道遇到問題時該看哪份文件、該用哪個範例。

---

## 第一天：讀懂地圖

- 讀完 [README](../README.md) 的學習路徑
- 知道本專案三個 Bounded Context：`catalog`、`customer`、`ordering`
- 知道 `domain`、`application`、`domainservice`、`infrastructure` 四層各自負責什麼
- 知道 `.codex/rules/` 是架構規則，`patterns/` 是可重用模式，`docs/` 是入門文件
- 知道本專案刻意保留 violation demo，所以部分架構測試預期失敗

## 第一週：能判斷一個改動該放哪裡

- 新增業務狀態轉換時，優先放在 Aggregate / Entity 方法
- 新增 use case 時，用 Command + Application Service 協調
- 新增查詢時，用 QueryModel / read model，不從 query path 觸發 command
- 跨 Context 保存關聯時，用 Reference Object，不 import 對方 Aggregate
- API request / response DTO 只留在 `infrastructure.web`
- 事件消費者要能處理重送，projection 要能重建

## 獨立開發前

- 能說明「一個 Command Handler 原則上只修改一個 Aggregate」的原因
- 能分辨 Domain Event 與 Integration Event
- 能說明 optimistic locking 是預設策略，pessimistic locking 是例外策略
- 能說明 Process Manager / Saga 何時需要，何時只是普通 listener
- 能跑 `mvn spotless:apply`
- 能跑 focused test，並知道哪些架構測試因 violation demo 預期失敗

## PR 前自查

開 PR 前先走一次 [架構 Review Checklist](06-architecture-review-checklist.md)。
如果有新的架構決策，補到 `decisions/` 或 `docs/adr/`；如果是跨專案可重用的技術模式，補到 `patterns/`。
