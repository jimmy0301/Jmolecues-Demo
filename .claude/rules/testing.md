---
description: 單元測試、ArchUnit 和 Spring Modulith 測試規則
paths:
  - src/test/**/*.java
  - src/main/java/**/*.java
---

# 測試規則

每次新增 class 後都要跑架構測試；新增 domain 或 application service 時也要補對應單元測試。

## Unit Test — 各層單元測試

框架：JUnit 5 + AssertJ + Mockito（`spring-boot-starter-test` 已內含）。

### Domain layer（不 mock，直接 new）

```java
// ordering/domain/OrderTest.java
class OrderTest {

    @Test
    void place_whenPending_transitionsToPlacedAndReturnsEvent() {
        var order = new Order(UUID.randomUUID());

        var event = order.place();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(event).isInstanceOf(OrderPlaced.class);
    }

    @Test
    void place_whenAlreadyPlaced_throws() {
        var order = new Order(UUID.randomUUID());
        order.place();

        assertThatThrownBy(order::place)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already");
    }
}
```

**規則：** domain 測試不需要 `@ExtendWith`，直接 `new` 物件即可。只驗證狀態與回傳值，不驗證持久化。

### Domain Service layer（不 mock）

```java
// ordering/domainservice/PricingServiceTest.java
class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    void calculateTotal_multipleItems_returnsSumOfSubtotals() {
        var result = pricingService.calculateTotal(List.of(
                new OrderItem(UUID.randomUUID(), Quantity.of(2), Money.of(new BigDecimal("10.00"), "USD")),
                new OrderItem(UUID.randomUUID(), Quantity.of(3), Money.of(new BigDecimal("5.00"), "USD"))));

        assertThat(result.amount()).isEqualByComparingTo("35.00");
    }
}
```

### Application Service layer（mock repository + event publisher）

```java
// ordering/application/OrderServiceTest.java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ApplicationEventPublisher events;
    @InjectMocks private OrderService orderService;

    @Test
    void handle_placeOrderCommand_publishesEventAndSaves() {
        var order = new Order(UUID.randomUUID());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = orderService.handle(new PlaceOrderCommand(order.getId()));

        var captor = ArgumentCaptor.forClass(OrderPlaced.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(order.getId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PLACED);
    }
}
```

**規則：** mock 只用於 Repository 和 ApplicationEventPublisher，不 mock domain 物件。

### 測試檔案位置

```
src/test/java/<base-package>/
├── shared/
│   └── MoneyTest.java                          # Shared Kernel VO
├── catalog/application/
│   └── ProductServiceTest.java                 # Application Service
├── customer/application/
│   └── CustomerServiceTest.java                # Application Service
└── ordering/
    ├── domain/
    │   ├── OrderTest.java                      # Aggregate 狀態機
    │   └── OrderItemTest.java                  # Entity
    ├── domainservice/
    │   └── PricingServiceTest.java             # Domain Service
    └── application/
        └── OrderServiceTest.java               # Application Service
```

## ArchUnit — 架構測試

### 應涵蓋的驗證規則

#### DDD / Onion

| 驗證內容 | 說明 |
|---|---|
| `shouldFollowDddRules` | jMolecules 內建規則（`JMoleculesDddRules.all()`），含跨 Context 物件參照、`@Identity` 必要性 |
| `shouldFollowOnionArchitecture` | Onion Classical 環狀依賴方向（`JMoleculesArchitectureRules.ensureOnionClassical()`） |
| `repositoriesShouldOnlyManageAggregateRoots` | `@Repository` 只能管理 `@AggregateRoot` |
| `valueObjectsShouldBeImmutable` | `@ValueObject` 不可有非 final 欄位或 setter |

#### CQRS

| 驗證內容 | 說明 |
|---|---|
| `commandsShouldBeImmutable` | `@Command` 不可有非 final 欄位或 setter |
| `queryModelsShouldNotTriggerCommands` | `@QueryModel` 不可呼叫 `@CommandHandler` 方法 |
| `commandsShouldResideInCommandPackage` | `@Command` 必須在 `*.application.command` 套件 |
| `commandHandlersShouldBeInApplicationLayer` | `@CommandHandler` 方法只能在 `*.application.*` 套件 |

### ArchUnit 實作細節

- `JavaParameterizedType` → 用 `.toErasure()` 取得 `JavaClass`，不是 `.getRawType()`
- Lombok `@Setter` 的 `RetentionPolicy.SOURCE`，在 bytecode 中不可見  
  → 改用**非 final field 檢查**即可涵蓋所有 Lombok setter 情況
- record 類別跳過 immutability 檢查（record 天生不可變）
- `MethodCallTarget.resolveMember()` → 回傳 `Optional<JavaMethod>`，用於追蹤 bytecode method call 的 annotation
- `JMoleculesArchitectureRules.ensureOnionClassical()` → 直接 `.check(classes)` 即可
- `JMoleculesCqrsRules` 不存在（v0.33.0），CQRS 規則需手寫 `ArchCondition`

### 預期失敗對照表

以下失敗是刻意保留的 violation demo，**不要修復**：

| 測試方法 | 預期失敗原因（violation demo） |
|---|---|
| `shouldFollowDddRules` | `BadOrder`（持有 `Customer` 物件）、`MutablePrice`（`@ValueObject` 可變） |
| `repositoriesShouldOnlyManageAggregateRoots` | `OrderItemRepository`（管理非 AggregateRoot 的 Entity） |
| `valueObjectsShouldBeImmutable` | `MutablePrice`（有 setter） |
| `commandsShouldBeImmutable` | `BadCommand`（non-final 欄位） |
| `queryModelsShouldNotTriggerCommands` | `BadQueryModel`（`@QueryModel` 呼叫 `@CommandHandler`） |
| `commandsShouldResideInCommandPackage` | `BadCommand`（放在 context root，非 `application.command`） |
| `commandHandlersShouldBeInApplicationLayer` | `BadDomainHandler`（`@CommandHandler` 在 context root） |
| `shouldFollowOnionArchitecture` | **通過**（無預期失敗） |
| `ModularityTest#verifiesModularStructure` | `BadOrder`（持有 `Customer`）、`BadCommand`（直接 import `customer.domain.CustomerId`） |

新增的 class 若造成上表以外的測試失敗，代表真正的架構違規，需要修正。

## Spring Modulith — ModularityTest

- `verifiesModularStructure` — 驗證模組邊界；若有 violation demo 刻意違反邊界，預期失敗
- `documentModules` — 產生模組文件到 `target/spring-modulith-docs/`

## 執行指令

```bash
mvn test -Dtest=JMoleculesArchitectureTest   # 只跑架構測試
mvn test -Dtest=ModularityTest               # 只跑模組測試
mvn test -Dtest="OrderTest,OrderServiceTest" # 只跑指定單元測試
mvn test                                     # 全部跑（架構 + 單元）
```

## Spotless（格式化）

- Spotless `check` 在 `validate` phase 執行（早於 `compile`），格式錯誤會立即終止建置
- ByteBuddy 在 `process-classes` 執行，格式通過才會到達此階段
- 提交前務必先跑 `mvn spotless:apply`
- 若 spotless 報錯，先 `mvn clean` 再重跑