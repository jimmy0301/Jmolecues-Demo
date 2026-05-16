# Chapter 5：Claude Code Skills 說明

本章說明專案內建的 Claude Code Skill，每個 Skill 封裝一個常見的 DDD 開發任務，確保每次操作都遵循專案規則。

---

## 什麼是 Skill？

Skill 是放在 `.claude/skills/<name>/SKILL.md` 的提示模板，在 Claude Code 的對話框輸入 `/<skill-name>` 即可執行。  
每個 Skill 包含固定步驟、程式碼模板、以及完成後的驗證指令，讓 Claude 不需要重新推導每次操作應該怎麼做。

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
| `domain/<AggregateRoot>.java` | 加入 producer 方法（回傳 event，不主動發布） |
| `application/<Context>Service.java` | 加入 `ApplicationEventPublisher.publishEvent()` |
| `application/<EventName>Listener.java` | `@ApplicationModuleListener` 接收 event |

**事件流程：**

```
AggregateRoot.someAction()   →   回傳 DomainEvent
    ↓
ApplicationService           →   publishEvent(event)
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

需要協調：載入 → 執行邏輯 → 儲存 → 發布 Event
  → Application Service（application/）
```

| 特性 | Domain Service | Application Service |
|---|---|---|
| Ring | `@DomainServiceRing` | `@ApplicationServiceRing` |
| Annotation | `@Service`（jMolecules） | `@Service`（Spring） |
| 可依賴 | Domain 物件 | Repository、EventPublisher、Domain Service |
| 典型例子 | `PricingService`、`DiscountPolicy` | `OrderService`、`CustomerService` |

**完成後：** 自動補對應單元測試（Domain Service 不 mock；Application Service mock Repository + EventPublisher）。

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
