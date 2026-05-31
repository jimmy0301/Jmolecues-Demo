# Chapter 5：Agent Skills 說明

本章說明專案內建的 Agent Skill，每個 Skill 封裝一個常見的 DDD 開發任務，確保每次操作都遵循專案規則。

---

## 什麼是 Skill？

Skill 是放在 `.agents/skills/<name>/SKILL.md` 的提示模板，在 Agent 的對話框輸入 `/<skill-name>` 即可執行。
每個 Skill 包含固定步驟、程式碼模板、以及完成後的驗證指令，讓 Agent 不需要重新推導每次操作應該怎麼做。

**執行方式：**

```
/<skill-name> [arguments]
```

---

## Skills 總覽

| Skill | 用途 | 必填引數 |
|---|---|---|
| `/add-bounded-context` | 新增一個完整的 Bounded Context | `<context-name>` |
| `/new-aggregate` | 建立 Aggregate（ID、class、Repository） | `<context> <AggregateName>` |
| `/new-entity` | 在 Aggregate 下建立內部 Entity | `<context> <AggregateName> <EntityName>` |
| `/new-domain-event` | 建立 DomainEvent、producer 方法、listener | `<EventName> <context>` |
| `/new-service` | 建立 Domain Service 或 Application Service | `<type> <context> <ServiceName>` |
| `/new-repository` | 為 AggregateRoot 建立 Repository（含框架分離方案） | `<context> <AggregateName>` |
| `/verify-architecture` | 執行完整架構驗證（Spotless + ArchUnit + Modulith） | 無 |
| `/review-pr-draft` | Review 別人的 PR/MR，只產出 review comment 草稿表格 | `<pr-or-mr-url>` |
| `/triage-pr-comments` | 整理別人 review 自己 PR/MR 的 active comments，含行數、程式碼片段與回覆草稿 | `<pr-or-mr-url>` |

---

## `/add-bounded-context`

**用途：** 在專案中新增一個全新的 Bounded Context，包含 package 骨架、Onion ring 的 `package-info.java`、以及 per-context `CLAUDE.md`。

**用法：**
```
/add-bounded-context <context-name>
```

**範例：**
```
/add-bounded-context shipping
```

**建立的結構：**
```
src/main/java/com/example/demo/shipping/
├── package-info.java           @BoundedContext(name = "shipping")
├── CLAUDE.md                   per-context 說明文件
└── domain/
    └── package-info.java       @DomainModelRing
```

`application/`、`domainservice/`、`infrastructure/web/` 等 ring 在有對應 class 時才建立，避免空目錄。

**執行後需要：** 接著用 `/new-aggregate` 在新 Context 建立至少一個 AggregateRoot。

---

## `/new-aggregate`

**用途：** 在指定 Bounded Context 的 `domain/` ring 建立一組完整的 Aggregate 相關 class。

**用法：**
```
/new-aggregate <context> <AggregateName> [ValueObject1] [ValueObject2] ...
```

**範例：**
```
/new-aggregate catalog Product Money Price
```

**建立的檔案：**

| 檔案 | 說明 |
|---|---|
| `domain/<AggregateName>Id.java` | `@ValueObject record` implements `Identifier` |
| `domain/<AggregateName>.java` | `@AggregateRoot @Document` |
| `domain/<ValueObject>.java` | 各 ValueObject（依引數） |
| `domain/<AggregateName>Repository.java` | `@Repository` extends `MongoRepository` |

**jMolecules 規則：**
- `@AggregateRoot` annotation（ArchUnit 辨識）
- `@Id @Identity` 雙標記（Spring Data + jMolecules 都需要）
- Protected 無參數建構子（MongoDB 反序列化用）

**完成後自動執行：** `mvn spotless:apply && mvn test -Dtest=JMoleculesArchitectureTest`

---

## `/new-entity`

**用途：** 在指定 Aggregate 下建立內部 Entity，同時確認 owning AggregateRoot 實作 typed interface。

**用法：**
```
/new-entity <context> <AggregateName> <EntityName>
```

**範例：**
```
/new-entity ordering Order OrderItem
```

**建立的檔案：**

| 檔案 | 說明 |
|---|---|
| `domain/<EntityName>Id.java` | Entity 的 ID class |
| `domain/<EntityName>.java` | `@Entity` + `implements Entity<AggregateRoot, EntityId>` |

**重要限制：**
- Entity 的 id 欄位加 `@Identity`，**不加** `@Id`（只有 MongoDB document root 加 `@Id`）
- owning AggregateRoot 必須同時 `implements AggregateRoot<A, ID>`，否則編譯失敗
- **禁止為 Entity 建立 `@Repository`**，Repository 只能管理 AggregateRoot

---

## `/new-domain-event`

**用途：** 建立完整的 DomainEvent 發布與接收流程，包含 event record、AggregateRoot 的 producer 方法、以及 application 層的 event listener。

**用法：**
```
/new-domain-event <EventName> <context>
```

**範例：**
```
/new-domain-event OrderPlaced ordering
```

**建立 / 修改的檔案：**

| 檔案 | 說明 |
|---|---|
| `domain/<EventName>.java` | `@DomainEvent record`，命名用**過去式** |
| `domain/<AggregateRoot>.java` | 加入 producer 方法，用 `registerEvent()` 登記事件 |
| `application/<EventName>Listener.java` | `@ApplicationModuleListener` 接收 event |

**事件流程：**

```
AggregateRoot.someAction()   →   registerEvent(DomainEvent)
    ↓
ApplicationService           →   repository.save(aggregate)（Spring Data 自動發布）
    ↓
EventListener.on(event)      →   @ApplicationModuleListener（async、交易隔離）
```

---

## `/new-service`

**用途：** 建立 Domain Service（`domainservice/` ring）或 Application Service（`application/` ring）。兩者職責截然不同，Skill 內含判斷指引。

**用法：**
```
/new-service <type> <context> <ServiceName>
```

`<type>` 填 `domain` 或 `application`。

**範例：**
```
/new-service domain ordering PricingService
/new-service application ordering OrderService
```

**如何選擇？**

```
單一 Entity 的狀態變更
  → 放在 Entity / AggregateRoot 方法

跨多個 Entity/Aggregate，不需要資料庫
  → Domain Service（domainservice/）

需要協調：載入 → 執行邏輯 → 儲存（Spring Data 發布已登記 Event）
  → Application Service（application/）
```

| 特性 | Domain Service | Application Service |
|---|---|---|
| Ring | `@DomainServiceRing` | `@ApplicationServiceRing` |
| Annotation | `@Service`（jMolecules） | `@Service`（Spring） |
| 可依賴 | Domain 物件 | Repository、Domain Service |
| 輸入邊界 | Domain 物件 | `@Command` / application input；API DTO 必須先在 `infrastructure.web` 轉換 |
| 交易邊界 | 不開交易、不存資料 | 原則上單一 handler 修改單一 Aggregate |
| 典型例子 | `PricingService`、`DiscountPolicy` | `OrderService`、`CustomerService` |

**完成後：** 自動補對應單元測試（Domain Service 不 mock；Application Service mock Repository，event 驗證透過 `getRegisteredEvents()` 檢查 aggregate；若 command 有冪等語意，補重送測試）。

---

## `/new-repository`

**用途：** 為指定 AggregateRoot 建立 Repository，提供框架耦合（方式 A）與框架分離（方式 B）兩種方案。

**用法：**
```
/new-repository <context> <AggregateName>
```

**範例：**
```
/new-repository ordering Order
```

**方式 A — 框架耦合（快速）**

Domain interface 直接繼承 `MongoRepository`，適合早期或單一資料庫的專案。

```java
@Repository  // org.jmolecules.ddd.annotation.Repository
public interface OrderRepository extends MongoRepository<Order, OrderId> {}
```

**方式 B — 框架分離（推薦）**

Domain interface 是純 Java，infrastructure 層各自實作，透過 Spring Profile 切換。

```
domain/
  OrderRepository.java          ← 純 Java interface，無框架 import

infrastructure/repository/mongo/
  MongoOrderRepository.java     ← @Profile("mongodb") implements OrderRepository
  SpringDataOrderRepository.java ← package-private，extends MongoRepository

infrastructure/repository/jpa/
  JpaOrderRepository.java       ← @Profile("jpa") implements OrderRepository
```

**DDD Repository 三個特點：**
1. 只能從 AggregateRoot 取得或修改領域物件
2. 隱藏持久化技術細節
3. 定義領域模型與資料模型的邊界

---

## `/verify-architecture`

**用途：** 一鍵執行完整的架構驗證，回報通過 / 失敗，並說明哪些失敗是預期的 violation demo。

**用法：**
```
/verify-architecture
```

**執行步驟：**

1. `mvn spotless:apply` — 格式化
2. `mvn test -Dtest=JMoleculesArchitectureTest` — ArchUnit 規則（8 個測試）
3. `mvn test -Dtest=ModularityTest` — Spring Modulith 模組驗證

**預期失敗（violation demo，不需修復）：**

| 測試 | 失敗原因 |
|---|---|
| `shouldFollowDddRules` | `BadOrder`（持有 `Customer` 物件） |
| `repositoriesShouldOnlyManageAggregateRoots` | `OrderItemRepository`（管理 Entity） |
| `valueObjectsShouldBeImmutable` | `MutablePrice`（有 setter） |
| `commandsShouldBeImmutable` | `BadCommand`（non-final 欄位） |
| `queryModelsShouldNotTriggerCommands` | `BadQueryModel`（呼叫 `@CommandHandler`） |
| `commandsShouldResideInCommandPackage` | `BadCommand`（放在 context root） |
| `commandHandlersShouldBeInApplicationLayer` | `BadDomainHandler`（在 context root） |
| `verifiesModularStructure` | `BadOrder`、`BadCommand`（跨模組邊界） |

---

## `/review-pr-draft`

**用途：** Review 別人的 GitHub PR、GitLab MR 或 Azure DevOps PR，只產出草稿，不發布 comment、不提交 review、不 approve/request changes。

**用法：**
```
/review-pr-draft <pr-or-mr-url>
```

**輸出表格：**

| # | Priority | Type | File | Lines | Code snippet | Finding | Risk | Draft review comment | Blocking |
|---|---|---|---|---|---|---|---|---|---|

**平台支援：**

| 平台 | Review 目標 | 相關資料 |
|---|---|---|
| GitHub | Pull Request | PR metadata、diff、review threads、check runs |
| GitLab | Merge Request | MR metadata、diff、discussions、pipelines |
| Azure DevOps | Pull Request | PR metadata、diff、comment threads、policies/checks |

**規則：**
- 只產出草稿，不寫回平台
- 優先檢查 correctness、架構邊界、測試、相容性、安全與營運風險
- 找不到 actionable issue 時，明確說明沒有發現阻塞問題，並列出剩餘風險或測試缺口

---

## `/triage-pr-comments`

**用途：** 整理別人 review 自己 PR/MR 後留下的 active comments，只抓尚未 resolved / closed / inactive 的 comments，產出處理表格與回覆草稿。

**用法：**
```
/triage-pr-comments <pr-or-mr-url>
```

**輸出表格：**

| # | Status | Priority | Type | Reviewer | File | Lines | Code snippet | Comment summary | Agent judgment | Suggested action | Reply draft |
|---|---|---|---|---|---|---|---|---|---|---|---|

**Active comments 定義：**

| 平台 | 納入 | 排除 |
|---|---|---|
| GitHub | unresolved review threads、active review comments | resolved、outdated、dismissed、非 actionable bot comments |
| GitLab | unresolved discussions | resolved discussions、obsolete diff notes |
| Azure DevOps | active comment threads | closed、fixed、won't fix、resolved、inactive threads |

**規則：**
- 每筆 active comment 必須帶檔案、行數或行數範圍
- 每筆 active comment 必須附最小必要程式碼片段，預設 comment 行上下各 3 行
- 沒有行號的 active comment 放在 `General comments`
- 已 resolved / outdated / closed 的 comments 預設略過，最後只回報略過數量
- 只產出 reply draft，不發布、不 resolve thread、不改 PR/MR 狀態

---

## 典型開發流程

新增一個完整的 Bounded Context 時，按以下順序執行：

```
/add-bounded-context <context>
  └─ /new-aggregate <context> <AggregateName>
       ├─ /new-entity <context> <AggregateName> <EntityName>   （有內部 Entity 時）
       ├─ /new-domain-event <EventName> <context>               （有 DomainEvent 時）
       ├─ /new-service domain <context> <DomainService>         （有跨 Aggregate 邏輯時）
       └─ /new-service application <context> <AppService>
/verify-architecture
```
