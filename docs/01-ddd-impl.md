# DDD 實作說明 — 索引

本文件是 [DDD 核心概念](01-ddd.md) 的實作索引。各主題拆成獨立檔案，讓概念文章可以直接連到對應實作。

---

## 實作文件

| 主題 | 文件 | 內容 |
|---|---|---|
| Building Blocks | [DDD Building Blocks 實作對應](01-ddd-impl-building-blocks.md) | Aggregate Root、Entity、Value Object、Domain Event、Repository、Domain Service 的專案對照與學習路徑 |
| Bounded Context / Shared Kernel / 跨 Context | [Bounded Context 與跨 Context 實作](01-ddd-impl-bounded-context.md) | Context 邊界、Shared Kernel、Reference Object、publicapi Facade、Snapshot、跨 Context update |
| Violation Demo | [DDD Violation Demo](01-ddd-impl-violations.md) | 刻意保留的違規範例與觸發的架構測試 |

---

## 相關章節

- [DDD 核心概念](01-ddd.md)
- [Onion Architecture 實作說明](04-onion-impl.md)
- [CQRS 實作說明](03-cqrs-impl.md)
