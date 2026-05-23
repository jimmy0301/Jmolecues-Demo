---
name: new-service
description: 在指定的 Bounded Context 建立 Domain Service 或 Application Service
arguments:
  - type
  - context
  - ServiceName
---

在指定的 Bounded Context 建立 Service。

用法：/new-service <type> <context> <ServiceName>
範例：/new-service domain ordering PricingService
      /new-service application ordering OrderService

引數：$ARGUMENTS

---

## 核心差異

Application Service 是「**做什麼（Use Case）**」，Domain Service 是「**核心邏輯怎麼做（Domain Logic）**」。

| 特性 | Application Service（應用服務） | Domain Service（領域服務） |
|---|---|---|
| **角色** | 指揮官、協調者 | 專家、業務計算者 |
| **Ring** | `application/` `@ApplicationServiceRing` | `domainservice/` `@DomainServiceRing` |
| **層級** | 應用層（外層） | 領域層（內層） |
| **職責** | 流程控制：DTO→Domain、載入 Aggregate、調用業務邏輯、事務提交、發布 Event | 業務規則：處理跨 Entity/Aggregate 的領域邏輯 |
| **輸入** | ID 或 DTO（從外部傳入，Repository 負責載入） | Domain 物件（Aggregate、ValueObject） |
| **輸出** | 更新後的 Aggregate 或 void | 計算結果（ValueObject 或 primitive） |
| **依賴** | 可依賴 Repository、ApplicationEventPublisher、Domain Service | 只依賴 domain layer，不碰 Repository、不發 Event |
| **annotation** | `@Service`（Spring `org.springframework.stereotype.Service`） | `@Service`（jMolecules `org.jmolecules.ddd.annotation.Service`） |
| **典型例子** | `OrderService`、`RegisterUserService` | `PricingService`、`TransferService`、`DiscountPolicy` |
| **商業邏輯** | 不包含，只有流程 | 包含複雜業務規則 |

---

## 如何決定使用哪個？

```
邏輯屬於單一 Entity 的狀態變更（如 user.changePassword()）
  → 放在 Entity / AggregateRoot 方法中

邏輯跨多個 Entity/Aggregate，且不需要存取資料庫
  → Domain Service（domainservice/）

需要協調：載入 Aggregate → 執行邏輯 → 儲存 → 發布 Event
  → Application Service（application/）
```

> **重構訊號：** 當 Application Service 的方法出現 if/else 業務判斷、或方法變得過長時，
> 代表領域邏輯外洩 → 應抽出 Domain Service 或移入 Aggregate。

---

## 方式 A — Domain Service（`domainservice/` ring）

**適用：** 純領域計算，封裝跨 Entity 的業務規則，無 I/O、無框架依賴。

典型場景：
- 跨多個 Aggregate 的計算（如訂單總價、轉帳金額驗證）
- 涉及外部定義的複雜演算法（如折扣策略、稅率計算）
- 邏輯無法歸類為特定 Entity 的狀態改變

```
<context>/
└── domainservice/
    ├── package-info.java      @DomainServiceRing（若不存在則建立）
    └── <ServiceName>.java
```

```java
// domainservice/package-info.java（若不存在）
@DomainServiceRing
package <base-package>.<context>.domainservice;
import org.jmolecules.architecture.onion.classical.DomainServiceRing;
```

```java
// domainservice/<ServiceName>.java
@Service  // org.jmolecules.ddd.annotation.Service
public class <ServiceName> {

    // ✅ 只依賴 domain 物件，不 inject Repository 或 ApplicationEventPublisher
    public <ResultType> <methodName>(List<<DomainObject>> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("...");
        }
        return items.stream()
                .map(<DomainObject>::<value>)
                .reduce(<ResultType>::<op>)
                .orElseThrow();
    }
}
```

**禁止在 Domain Service 中：**
- constructor injection `Repository`
- `ApplicationEventPublisher`
- 任何 Spring Data / MongoDB / JPA import

---

## 方式 B — Application Service（`application/` ring）

**適用：** 協調 Use Case 流程，扮演外部請求與領域模型之間的橋樑。

典型場景：
- 處理從 Controller / 外部系統進來的請求
- 從 Repository 載入 Aggregate，委派業務邏輯，再儲存並發布 Event
- 事務邊界、權限校驗、通知外部系統

```
<context>/
└── application/
    ├── package-info.java      @ApplicationServiceRing（若不存在則建立）
    └── <ServiceName>.java
```

```java
// application/package-info.java（若不存在）
@ApplicationServiceRing
package <base-package>.<context>.application;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
```

```java
// application/<ServiceName>.java
@Service  // org.springframework.stereotype.Service
public class <ServiceName> {

    private final <AggregateRoot>Repository repository;
    private final ApplicationEventPublisher events;  // org.springframework.context.ApplicationEventPublisher

    public <ServiceName>(<AggregateRoot>Repository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    // Use Case：建立（接受 @Command 作為參數，方法加 @CommandHandler）
    @CommandHandler  // org.jmolecules.architecture.cqrs.CommandHandler
    public <AggregateRoot> handle(Create<AggregateRoot>Command command) {
        return repository.save(new <AggregateRoot>(command.<field>()));
    }

    // Use Case：狀態變更 + 發布 Event
    @CommandHandler
    public <AggregateRoot> handle(<Action><AggregateRoot>Command command) {
        <AggregateRoot> aggregate = findOrThrow(command.id());
        events.publishEvent(aggregate.<action>());   // producer 方法回傳 event
        return repository.save(aggregate);
    }

    public Optional<<AggregateRoot>> findById(<AggregateRoot>Id id) {
        return repository.findById(id);
    }

    private <AggregateRoot> findOrThrow(<AggregateRoot>Id id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("<AggregateRoot> not found: " + id.id()));
    }
}
```

**Application Service 的規則：**
- 每個方法對應一個 Use Case，不要把多個 Use Case 混在一起
- 不包含 if/else 業務判斷（業務邏輯在 Aggregate 或 Domain Service 裡）
- 流程固定：載入 → 委派給 Aggregate → 儲存 → 發布 Event

---

## 步驟

1. 若 ring sub-package 不存在，建立 `package-info.java`
2. 建立 Service class（依上方模板）
3. 更新 `<context>/AGENTS.md`，將新 Service 加入 Classes 表格
4. 建立對應單元測試：
   - **Domain Service** → 直接 `new PricingService()`，不 mock，測試計算邏輯與邊界條件
   - **Application Service** → `@ExtendWith(MockitoExtension.class)` + `@Mock Repository` + `@Mock ApplicationEventPublisher`，驗證 command handler 流程與 event 發布

   測試位置：`src/test/java/<base-package>/<context>/<ring>/<ServiceName>Test.java`

完成後執行：
```bash
mvn spotless:apply && mvn test -Dtest=JMoleculesArchitectureTest
```
