# 團隊技術入門指南

這份文件是給**新加入團隊的工程師**看的。  
目的是讓你在最短時間內理解我們採用的軟體設計思想，以及這些思想如何落地在真實的程式碼裡。

我們用一個簡單的電商範例（`catalog` / `customer` / `ordering` 三個 Bounded Context）貫穿所有概念。  
範例以 Java + Spring Boot 實作，並以 [jMolecules](https://github.com/xmolecules/jmolecules) annotation 在程式碼中明確標記每個 DDD 角色，讓你讀 code 時不需要猜測。

**讀完這份文件，你會知道：**

- 我們為什麼用 DDD，以及 AggregateRoot / ValueObject / DomainEvent 這些詞在程式碼裡對應什麼
- 我們如何用 Onion Architecture 組織程式碼的分層結構
- 我們如何用 CQRS 分離寫入與讀取
- 我們選了哪些工具，以及為什麼這樣選
- 如何用內建的 Claude Code Skills 快速新增符合規範的程式碼

---

## 學習路徑

1. [Domain-Driven Design 核心概念](docs/01-ddd.md)  
   — Building blocks 定義、Bounded Context、Shared Kernel、跨 Context 溝通  
   &nbsp;&nbsp;&nbsp;&nbsp;↳ [DDD 實作說明](docs/01-ddd-impl.md) — 本專案對照表、Reference 物件模式、學習路徑、violation demo

2. [Onion Architecture — 洋蔥分層](docs/04-onion.md)  
   — 四個 Ring 職責、依賴方向規則  
   &nbsp;&nbsp;&nbsp;&nbsp;↳ [Onion Architecture 實作說明](docs/04-onion-impl.md) — Ring annotation、package-info.java、ArchUnit 驗證

3. [CQRS — 命令與查詢分離](docs/03-cqrs.md)  
   — 核心概念、Command vs Query、執行流程圖  
   &nbsp;&nbsp;&nbsp;&nbsp;↳ [CQRS 實作說明](docs/03-cqrs-impl.md) — jMolecules annotations、程式碼範例、violation demo

4. [技術棧說明](docs/02-tech-stack.md)  
   — 採用的框架與工具、各自的職責與選擇理由

5. [Claude Code Skills 說明](docs/05-skills.md)  
   — 內建 Skill 一覽、用法範例、典型開發流程

6. [跨 Bounded Context 存取資料的設計模式](docs/06-cross-context-data-access.md)  
   — Spring Modulith 可見性規則、反模式、公開 API 設計、Reference Object vs Facade vs Domain Event

---

## 快速開始

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

| 測試 | 結果 | 說明 |
|---|---|---|
| `JMoleculesArchitectureTest`（8 tests） | 7 失敗 / 1 通過 | 7 個因 violation demo 刻意失敗；`shouldFollowOnionArchitecture` 通過 |
| `ModularityTest#verifiesModularStructure` | 失敗 | `BadOrder` 持有跨 Context 的 `Customer` 物件；`BadCommand` 直接 import `customer.domain.CustomerId`（violation demo #1、#4） |
| `ModularityTest#documentModules` | 通過 | 產生模組文件到 `target/spring-modulith-docs/` |

---

## 延伸閱讀

- [`docs/adr/001-use-jmolecules.md`](docs/adr/001-use-jmolecules.md) — 為何選擇 jMolecules 的架構決策記錄
