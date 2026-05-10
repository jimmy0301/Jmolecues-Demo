---
description: Bounded Context 邊界規則，所有 Java 檔案都適用
paths:
  - src/main/java/**/*.java
---

# Bounded Context 邊界規則

## 跨 Context 只能用 ID 參照

```java
// ✅ 正確：Order 只持有 CustomerId
public class Order {
    private CustomerId customerId;
}

// ❌ 違規：直接持有其他 Context 的 Aggregate 物件
public class BadOrder {
    private Customer customer;  // 違反邊界
}
```

## 現有 Context 與其 AggregateRoot

| Context | Domain Package | AggregateRoot | ID class |
|---|---|---|---|
| catalog | `com.example.demo.catalog.domain` | `Product` | `ProductId` |
| customer | `com.example.demo.customer.domain` | `Customer` | `CustomerId` |
| ordering | `com.example.demo.ordering.domain` | `Order` | `OrderId` |

跨 Context ID 參照時，import 路徑需使用 `.domain` sub-package：
```java
import com.example.demo.customer.domain.CustomerId;
import com.example.demo.catalog.domain.ProductId;
```

## 禁止事項

1. 跨 Context 持有 Aggregate 物件（只能持有其 ID）
2. `@Document` 不搭配 jMolecules annotation 單獨使用
3. 為非 `@AggregateRoot` 的 Entity 建立 `@Repository`
4. ValueObject 有可變狀態（非 final field 或 setter）
5. 跳過 AggregateRoot 直接操作內部 Entity
