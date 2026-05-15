# Chapter 5：Bounded Context 邊界與跨 Context 溝通

## 為什麼需要邊界？

每個 Bounded Context 代表一個獨立的業務子域，有自己的語言與模型。  
若 Context 之間可以任意互相依賴，會造成：

- **結構耦合**：`ordering` 持有 `Customer` 物件，`customer` 結構一改，`ordering` 跟著壞
- **語義衝突**：`Customer` 在 `ordering` 眼中是「下單的人」，在 `customer` 眼中是「有地址的帳戶」——同名不同義
- **部署耦合**：兩個業務域無法獨立演進

---

## 跨 Context 的合法溝通方式

### 方式 A — ID 參照

Context A 只儲存 Context B 的 Aggregate ID，不持有物件本身：

```java
// ✅ ordering 只知道「這筆訂單屬於哪個顧客 ID」
public class Order {
    private CustomerReference customer; // 儲存 UUID，不持有 Customer 物件
}

// ❌ 違規：直接持有另一 Context 的 Aggregate
public class BadOrder {
    private Customer customer; // ordering 與 customer 耦合
}
```

### 方式 B — Domain Event

Context A 發布事件，Context B 監聽後在自己的模型中處理：

```
ordering ──publishes──▶ OrderPlaced ──consumed by──▶ inventory (更新庫存)
```

事件是最鬆耦合的方式，兩邊只需要對「事件結構」達成協議。

---

## Reference 物件模式

僅儲存 UUID 已能表達「我需要這個 Context 的某筆資料」，但 raw `UUID` 在程式碼中沒有語義。

**Reference 物件**是各 Context 在自己的 domain 層定義的 Value Object，包裝 UUID 並賦予業務名稱：

```
ordering/domain/
  CustomerReference(UUID id)  ← ordering 對顧客的參照
  ProductReference(UUID id)   ← ordering 對商品的參照
```

**好處：**
- 語義明確：`CustomerReference` 比 `UUID customerId` 更清楚表達意圖
- 不實作 `Identifier`：避免 ByteBuddy 誤將欄位標記為資料庫 `@Id`
- 零跨 Context import：`ordering.domain` 不再引用任何其他 Context 的型別

---

## Shared Kernel — 真正共用的型別

當某個型別**跨多個 Context 都需要用到**，且各 Context 對它的理解完全一致，才放入 Shared Kernel：

```
shared/
  Money(BigDecimal amount, String currency)  ← catalog 定價、ordering 訂單項目都需要
```

**判斷準則：**

| 情境 | 做法 |
|---|---|
| 只有一個 Context 使用 | 放在該 Context 的 `domain/` |
| 多個 Context 使用，語義一致 | 放在 `shared/` Shared Kernel |
| 多個 Context 使用，語義不同 | 各 Context 自訂型別，不要共用 |

**限制：**  
修改 Shared Kernel 型別，**所有依賴它的 Context 必須同步調整**，因此應保持小而穩定。

---

## ID 的歸屬

Aggregate 的 ID 型別（如 `CustomerId`）在語義上**屬於它所在的 Context**。  
其他 Context 需要參照時，透過 Reference 物件間接持有，而非直接 import 對方的 ID 型別。

```
customer/domain/CustomerId  ← 屬於 customer context，customer 自己使用
catalog/domain/ProductId    ← 屬於 catalog context，catalog 自己使用

ordering/domain/CustomerReference(UUID)  ← ordering 自己的跨 context 參照
ordering/domain/ProductReference(UUID)   ← ordering 自己的跨 context 參照
```

---

→ 本專案的實作對應見 [Bounded Context 實作說明](05-bounded-context-impl.md)
