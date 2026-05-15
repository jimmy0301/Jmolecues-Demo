# Bounded Context 實作說明

## Context 結構

本專案有三個 Bounded Context 與一個 Shared Kernel：

```
com.example.demo
├── shared/             Shared Kernel（Money — catalog + ordering 都使用）
├── catalog/            商品目錄 Context
│   └── domain/         Product, ProductId, ProductRepository
├── customer/           顧客管理 Context
│   └── domain/         Customer, CustomerId, Address, CustomerRepository
└── ordering/           訂單管理 Context
    ├── domain/         Order, OrderItem, CustomerReference, ProductReference, …
    ├── domainservice/  PricingService
    └── application/    OrderService, OrderQueryModel, commands
```

---

## 跨 Context 參照：Reference 物件

`ordering` 需要記錄「這筆訂單屬於哪個顧客」、「這個訂單項目是哪個商品」，  
但 `ordering.domain` **不 import** 其他 Context 的任何型別。

### CustomerReference / ProductReference

```java
// ordering/domain/CustomerReference.java
@ValueObject
public record CustomerReference(UUID id) {}

// ordering/domain/ProductReference.java
@ValueObject
public record ProductReference(UUID id) {}
```

**設計重點：**
- **不實作 `Identifier`**：避免 jMolecules ByteBuddy 將此 field 誤判為 `@Id`，造成 MongoDB 啟動時衝突
- **`@ValueObject`**：不可變，ArchUnit 會驗證
- **存於 `ordering/domain/`**：屬於 ordering 的領域型別，不是共用型別

### Order 使用方式

```java
public class Order {
    @Id @Identity private OrderId id;
    private CustomerReference customer;   // ← 儲存 UUID，不 import CustomerId

    public Order(UUID customerId) {
        this.customer = new CustomerReference(customerId);
    }

    public CustomerReference getCustomer() { return customer; }
}
```

### OrderItem 使用方式

```java
public class OrderItem implements Entity<Order, OrderItemId> {
    @Identity private OrderItemId id;
    private ProductReference product;     // ← 儲存 UUID，不 import ProductId

    public OrderItem(UUID productId, Quantity quantity, Money unitPrice) {
        this.product = new ProductReference(productId);
        ...
    }
}
```

---

## ID 型別的歸屬

每個 Context 的 ID 型別放在自己的 `domain/` package，不再集中於 `shared/`：

| 型別 | 位置 | 說明 |
|---|---|---|
| `CustomerId` | `customer/domain/` | customer context 自用 |
| `ProductId` | `catalog/domain/` | catalog context 自用 |
| `Money` | `shared/` | catalog + ordering 共用，語義完全一致 |

### 為什麼 ID 不需要放 shared？

`CustomerId` 只有 `customer` context 的 aggregate、repository、service 會直接使用。  
`ordering` context 不需要知道 `CustomerId` 的型別，只需要知道「一個 UUID 代表某個顧客」，  
這個語義由 `CustomerReference` 在 `ordering.domain` 內表達。

---

## 應用層的邊界

`ordering.application`（Command / Service）直接使用 `UUID`，  
不需要 import 任何其他 Context 的型別：

```java
// CreateOrderCommand — 不 import CustomerId
@Command
public record CreateOrderCommand(UUID customerId) {}

// OrderService — 將 UUID 傳給 domain
@CommandHandler
public Order handle(CreateOrderCommand command) {
    return orderRepository.save(new Order(command.customerId()));
}
```

控制器從 OpenAPI 生成的 request model 取得 `UUID`，直接傳入 command：

```java
// OrderController — 不 import CustomerId
var command = new CreateOrderCommand(createOrderRequest.getCustomerId()); // UUID
```

---

## 違規 Demo

| Demo | 位置 | 違規內容 |
|---|---|---|
| `BadOrder` | `ordering/` | 直接持有 `Customer` 物件（應用 Reference 或 ID） |

`BadOrder` 由 `shouldFollowDddRules`（ArchUnit）和 `verifiesModularStructure`（Spring Modulith）共同偵測。

---

## ArchUnit 驗證

`shouldFollowDddRules` 使用 `JMoleculesDddRules.all()` 自動檢查：

```
✅ Order.customer 型別為 CustomerReference（不是 Customer）→ 通過
✅ OrderItem.product 型別為 ProductReference（不是 Product）→ 通過
❌ BadOrder.customer 型別為 Customer（直接持有 Aggregate）→ 預期失敗（violation demo）
```

---

## 設計選擇說明

### 為何不用 `Association<Customer, CustomerId>`？

jMolecules 提供 `Association<T extends AggregateRoot, ID>` 作為跨 Aggregate 的型別參照。  
但本專案的 jMolecules ArchUnit DDD rules（v2025.0.2）在掃描 bytecode 時會跟進泛型型別參數，  
把 `Association<Customer, CustomerId>` 中的 `Customer`（implements `AggregateRoot`）誤判為「直接持有 AggregateRoot」，  
導致 `shouldFollowDddRules` 產生誤報。  

Reference 物件模式迴避了這個問題，且在語義上更明確地表達「ordering 的視角」。

### 為何不直接用 raw `UUID`？

Raw `UUID` 沒有語義，看不出「這個 UUID 代表哪種業務概念」。  
`CustomerReference` 和 `ProductReference` 在 `ordering.domain` 中是第一級的領域型別，  
讓程式碼本身就能傳達業務意圖。
