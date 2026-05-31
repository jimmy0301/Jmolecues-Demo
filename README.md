# 團隊技術入門指南

這份文件提供**新加入團隊的工程師**所需的專案導讀。
目標是在最短時間內理解本專案採用的軟體設計思想，以及這些思想如何落地在真實的程式碼裡。

本專案以一個簡單的電商範例（`catalog` / `customer` / `ordering` 三個 Bounded Context）貫穿所有概念。
範例以 Java + Spring Boot 實作，並以 [jMolecules](https://github.com/xmolecules/jmolecules) annotation 在程式碼中明確標記每個 DDD 角色。

**建議閱讀方式：**

- 先讀設計概念，理解專案為什麼這樣切程式碼
- 再看本專案實作，知道每個概念對應到哪些 package / class
- 最後看技術棧與 Agent Skills，學會如何新增符合規範的程式碼

---

## 學習路徑

0. [新人上手檢查清單](docs/00-onboarding-checklist.md)
   — 第一天、第一週、獨立開發前要理解的規則

### 1. 軟體設計概念

1. [Domain-Driven Design 核心概念](docs/01-ddd.md)
   — Building Blocks、Aggregate invariant、Bounded Context、Shared Kernel、Ubiquitous Language

2. [Bounded Context 與整合邊界](docs/01-ddd-context-integration.md)
   — 跨 Context 資料擁有權、Reference Object、Snapshot、Anti-Corruption Layer、API DTO 邊界

3. [一致性、並發與流程協調](docs/01-ddd-consistency-workflows.md)
   — 交易邊界、Domain Event vs Integration Event、Listener 冪等性、樂觀鎖 / 悲觀鎖、Process Manager / Saga

4. [Onion Architecture — 洋蔥分層](docs/04-onion.md)
   — 四個 Ring 的職責、依賴方向、Domain / Application / Infrastructure / Adapter DTO 的邊界

5. [CQRS — 命令與查詢分離](docs/03-cqrs.md)
   — Command vs Query、validation 分層、冪等、projection、並發衝突、命名規則、Command 與 API DTO 分離

### 2. 實作範例說明

6. DDD 實作對應
   — 從概念跳到本專案程式碼位置

   - [DDD Building Blocks 實作對應](docs/01-ddd-impl-building-blocks.md)
     — Aggregate Root、Entity、Value Object、Domain Event、Repository、Domain Service

   - [Bounded Context 與跨 Context 實作](docs/01-ddd-impl-bounded-context.md)
     — Shared Kernel、Reference Object、publicapi Facade、Snapshot、跨 Context update

   - [DDD Violation Demo](docs/01-ddd-impl-violations.md)
     — 刻意保留的錯誤範例，以及架構測試如何抓到

7. [Onion Architecture 實作說明](docs/04-onion-impl.md)
   — package-info.java、Ring annotation、ArchUnit 驗證

8. [CQRS 實作說明](docs/03-cqrs-impl.md)
   — jMolecules CQRS annotations、Command / QueryModel / CommandHandler 範例

### 3. 技術棧與日常開發工具

9. [技術棧說明](docs/02-tech-stack.md)
   — Spring Boot、MongoDB、jMolecules、Spring Modulith、ArchUnit、OpenAPI Generator、Spotless

10. [Agent Skills 說明](docs/05-skills.md)
   — 如何新增 Bounded Context、Aggregate、Entity、Repository、Domain Event、Service，並執行架構驗證

11. [架構 Review Checklist](docs/06-architecture-review-checklist.md)
    — 開 PR 前檢查 DDD / CQRS / Onion / Event / Concurrency / Test 文件是否到位

---

## 驗證指令

```bash
# 格式化
mvn spotless:apply

# 全部測試（含 ArchUnit + Spring Modulith）
mvn test

# 只跑架構測試（ArchUnit）
mvn test -Dtest=JMoleculesArchitectureTest

# 只跑模組結構測試（Spring Modulith）
mvn test -Dtest=ModularityTest
```

**預期結果**：

本專案刻意保留 violation demo，所以部分架構測試預期失敗；這是教學用途，不代表專案壞掉。

| 測試 | 結果 | 說明 |
|---|---|---|
| `JMoleculesArchitectureTest`（8 tests） | 7 失敗 / 1 通過 | 7 個因 violation demo 刻意失敗；`shouldFollowOnionArchitecture` 通過 |
| `ModularityTest#verifiesModularStructure` | 失敗 | `BadOrder` 持有跨 Context 的 `Customer` 物件；`BadCommand` 直接 import `customer.domain.CustomerId`（violation demo #1、#4） |
| `ModularityTest#documentModules` | 通過 | 產生模組文件到 `target/spring-modulith-docs/` |

---

## 延伸閱讀

- [`docs/adr/001-use-jmolecules.md`](docs/adr/001-use-jmolecules.md) — 為何選擇 jMolecules 的架構決策記錄
- [`patterns/`](patterns/) — 跨專案可重用的技術模式
