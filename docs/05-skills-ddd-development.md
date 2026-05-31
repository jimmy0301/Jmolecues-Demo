# Chapter 5.1：DDD 開發 Skills

本章說明新增 Bounded Context、Aggregate、Entity、Domain Event、Service 與 Repository 的技能。

---

## `/add-bounded-context`

**用途：** 在專案中新增一個全新的 Bounded Context，包含 package 骨架、Onion ring 的 `package-info.java`、以及 per-context `CLAUDE.md`。

**用法：**

```text
/add-bounded-context <context-name>
```

**範例：**

```text
/add-bounded-context shipping
```

**建立的結構：**

```text
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

```text
/new-aggregate <context> <AggregateName> [ValueObject1] [ValueObject2] ...
```

**範例：**

```text
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

```text
/new-entity <context> <AggregateName> <EntityName>
```

**範例：**

```text
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

```text
/new-domain-event <EventName> <context>
```

**範例：**

```text
/new-domain-event OrderPlaced ordering
```

**建立 / 修改的檔案：**

| 檔案 | 說明 |
|---|---|
| `domain/<EventName>.java` | `@DomainEvent record`，命名用**過去式** |
| `domain/<AggregateRoot>.java` | 加入 producer 方法，用 `registerEvent()` 登記事件 |
| `application/<EventName>Listener.java` | `@ApplicationModuleListener` 接收 event |

**事件流程：**

```text
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

```text
/new-service <type> <context> <ServiceName>
```

`<type>` 填 `domain` 或 `application`。

**範例：**

```text
/new-service domain ordering PricingService
/new-service application ordering OrderService
```

**如何選擇？**

```text
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

```text
/new-repository <context> <AggregateName>
```

**範例：**

```text
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

```text
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
