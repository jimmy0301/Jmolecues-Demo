# Chapter 1：Domain-Driven Design 核心概念

## 什麼是 DDD？

DDD 是一套把「業務邏輯」放在程式設計核心的方法論，核心思想是：

> **讓程式碼的語言與業務的語言一致。**

傳統做法容易讓 `Order` 退化成只有 getter/setter 的資料袋，業務邏輯散落在各處的 Service。  
DDD 的做法是讓 `Order` 真正知道自己可以 `place()`（下單）、`cancel()`（取消），  
並在狀態改變時發布 `OrderPlaced` 事件，讓程式碼本身就能說明業務意圖。

第一次閱讀本章時，先抓住三個問題即可：

- 這段邏輯屬於哪個業務概念？
- 這個物件能不能被外部直接修改？
- 跨 Bounded Context 時，誰擁有資料、誰只能透過合約使用資料？

---

## Building Blocks

DDD 定義了幾種戰術模式（Tactical Patterns），用來組織領域物件：

### Aggregate Root

Aggregate Root 是一群相關物件的統一入口，負責維護一致性邊界。
外部不能隨意操作 Aggregate 內部的 Entity 或 Value Object，只能透過 Aggregate Root 暴露的方法改變狀態。

例如訂單模型中，外部應該呼叫 `Order.addItem(...)`、`Order.place()`，而不是直接修改 `OrderItem` 清單。

### Entity

Entity 有唯一 ID 與生命週期；即使屬性改變，只要 ID 相同，仍代表同一個物件。
在 Aggregate 裡，Entity 通常隸屬於某個 Aggregate Root，不能被外部獨立存取。

例如 `OrderItem` 可以有自己的 `OrderItemId`，但它仍然隸屬於 `Order`，不應被外部單獨查詢或儲存。

### Value Object

Value Object 沒有獨立身分，以值定義相等，且必須不可變。
它適合表達金額、地址、數量、跨 Context reference 等概念。

例如 `Money(100, "TWD")` 與另一個同樣金額和幣別的 `Money(100, "TWD")` 可以視為相等；
修改金額時應回傳新物件，而不是改動原物件。

### Domain Event

Domain Event 是 Aggregate 狀態改變時發出的通知，代表已經發生的事實，因此命名通常使用過去式。

例如 `OrderPlaced` 表示「訂單已成立」，不是「請成立訂單」。
事件讓其他邏輯可以在不直接耦合 Aggregate 的情況下反應狀態變化。

### Repository

Repository 是 Aggregate Root 的持久化抽象，封裝儲存與查詢細節。
Repository 不應為 Entity 或 Value Object 建立；外部若需要操作 Aggregate 內部物件，應先取得 Aggregate Root。

例如應該有 `OrderRepository`，不應該有 `OrderItemRepository`。

### Domain Service

Domain Service 放置不自然屬於任一 Aggregate 的領域邏輯，通常是無狀態的。
它仍屬於 Domain 層，不應處理資料庫、HTTP 或訊息佇列等技術細節。

例如計算多個訂單項目的總價，可以放在 `PricingService`，而不是塞進 Controller 或 Repository。

### Bounded Context

Bounded Context 是一個業務子域的邊界，有自己的模型、語言與規則。
同一個詞在不同 Context 裡可以有不同意義，因此跨 Context 不應直接共用對方的 domain model。

例如 `Customer` 在 customer context 可能代表會員帳戶與地址，在 ordering context 可能只代表「下單者的 reference」。

### Shared Kernel

Shared Kernel 是多個 Context 協議共用的一小塊模型。
它是 Bounded Context 隔離規則的例外，因此應該盡量小、穩定，且修改時要讓所有依賴方一起承擔影響。

例如 `Money` 若在多個 Context 中語義完全一致，可以放進 Shared Kernel；若語義不同，就應各自定義。

→ 本專案的 building block 實作對應見 [DDD Building Blocks 實作對應](01-ddd-impl-building-blocks.md)

---

## 架構模式

### Bounded Context — 業務邊界

每個 Bounded Context 是一個獨立的業務子域，有自己的模型、語言與資料一致性邊界。
跨 Context 存取資料不是「能不能拿資料」的問題，而是「誰擁有資料、透過什麼合約互動」的問題。

在這個 demo 裡：

- `Product` 由 `catalog` 擁有
- `Customer` 由 `customer` 擁有
- `Order` 由 `ordering` 擁有

其他 Context 可以知道某個外部 Aggregate 的身分，也可以透過公開合約取得必要資訊，
但不應該直接依賴對方的 domain model、repository 或資料庫結構。

**跨 Context 持有物件的後果：**
- **結構耦合**：`ordering` 持有 `Customer` 物件，`customer` 結構一改，`ordering` 跟著壞
- **語義衝突**：`Customer` 在 `ordering` 眼中是「下單的人」，在 `customer` 眼中是「有地址的帳戶」——同名不同義
- **部署耦合**：兩個業務域無法獨立演進

#### 跨 Context 溝通方式

先看互動意圖，再選模式：

| 意圖 | 正確做法 | 不該做 |
|---|---|---|
| 保存關聯 | Reference Object，只保存對方 ID | 持有對方 Aggregate 物件 |
| 讀取展示資料 | 呼叫 owner 的 Query Facade，或使用本地 read model | import 對方 QueryModel / Repository |
| 要求對方改變狀態 | 呼叫 owner 的 Command Facade，或發布事件讓 owner 自己處理 | 直接修改對方資料庫 |
| 保存當下事實 | 在自己的 Aggregate 保存 Snapshot | 每次都查對方最新狀態來解釋歷史資料 |

一句話：**可以取得資料，但只能透過 owner Context 設計好的公開契約；取得後要轉成本 Context 的語言。**

第一次讀到這裡時，不需要先記住每個實作細節。
先記住「保存關聯用 Reference、讀取資料用公開查詢、保存歷史用 Snapshot、通知狀態改變用 Event」即可。

#### Reference Object — 保存關聯身分

只儲存對方 Aggregate 的識別碼，不持有物件。raw `UUID` 缺乏語義，可在自己的 domain 定義 Reference 物件（包裝 UUID 的 `@ValueObject`）：

```
ordering/domain/
  CustomerReference(UUID id)  ← ordering 對顧客的參照
  ProductReference(UUID id)   ← ordering 對商品的參照
```

```java
public class Order {
    private CustomerReference customer;
}

public class OrderItem {
    private ProductReference product;
}
```

不要直接保存對方型別：

```java
// ❌ ordering 不應該 import customer.domain.Customer
private Customer customer;

// ❌ 本專案也不要直接用對方 ID 型別
private CustomerId customerId;
```

#### Query Facade / Read Model — 取得跨 Context 資料

需要讀取另一個 Context 的展示資料時，透過 owner Context 暴露的公開查詢 API / Facade 取得。
不要 import 對方的 `domain`、`application`、`repository` 等 internal 型別。

```
ordering ──queries──▶ catalog 的公開查詢介面
```

這仍然是一條跨 Bounded Context 依賴，但它依賴的是 owner Context 刻意公開的合約，而不是內部模型。
公開查詢介面應回傳穩定 DTO，例如 `ProductSummary`，而不是回傳 owner 的 Aggregate、Entity、Repository 或內部 QueryModel。

若一個 Context 經常需要另一個 Context 的資料，而且查詢量大或可以接受 eventual consistency，
可以訂閱 owner 的公開事件，在本 Context 建立 read model / projection。

#### Command Facade / Domain Event — 更新跨 Context 資料

需要請另一個 Context 改變狀態時，不直接改對方資料庫，也不直接拿對方 Repository。
若 use case 必須同步知道成功/失敗，呼叫 owner 的公開 Command Facade；若只需通知已發生的事，發布事件讓對方自行處理：

```
ordering ──publishes──▶ OrderPlaced ──consumed by──▶ inventory
```

事件代表「已發生的事實」，不是遠端方法呼叫。Producer 不應知道誰會處理事件。
當事件被其他 Context 消費時，它就成為跨 Context 合約，語意上接近 Integration Event，應避免包含 owner 的 internal domain 型別。

#### Snapshot — 保存本 Context 的歷史事實

有些資料一旦進入本 Context，就變成本 Context 的歷史事實。
例如商品名稱和價格由 `catalog` 擁有，但「下單當下的商品名稱與成交價格」屬於 `ordering` 的訂單事實。
這時不應每次顯示訂單都查最新商品價格，因為商品後來調價不應改變歷史訂單。

```java
public class OrderItem {
    private ProductReference product;
    private String productNameSnapshot;
    private Money unitPriceSnapshot;
    private Quantity quantity;
}
```

Snapshot 不是壞味道；它是在本 Context 保存業務事實。判斷問題是：
**如果外部資料改變，既有紀錄是否也應跟著改？** 若答案是否定的，就應保存 snapshot。

#### Public DTO 的型別規則

公開 DTO 的目標是穩定契約，不是把 domain model 打包丟出去。

可以使用 `UUID`、`String`、`BigDecimal`、`Instant`、primitive / wrapper、owner 明確設計的 public DTO，
以及非常穩定的 Shared Kernel 型別（例如 `Money`，仍需謹慎）。

避免使用 owner 的 Aggregate、Entity、domain enum、ValueObject、Repository、Application Service、QueryModel 等 internal type。
簡單判斷：**如果 DTO 欄位型別需要 import 某個模組的 sub-package，通常就是洩漏。**

#### 常見反模式

- 直接 import 另一個 Context 的 `domain` / `application` / `repository` package
- 跨 Context 使用 Repository
- 直接修改另一個 Context 的資料庫
- 公開 DTO 洩漏 owner 的 domain type
- 把內部實作細節包成公開合約，而不是設計穩定的跨 Context API

→ 本專案的 Bounded Context 與跨 Context 實作見 [Bounded Context 與跨 Context 實作](01-ddd-impl-bounded-context.md)

### Shared Kernel — 協議共用的型別

Shared Kernel 是 Bounded Context 規則的例外：  
當多個 Context 都需要同一個概念（如金額 `Money`），將它放在共享的小型 package，各 Context 都可以依賴。

**限制：**
- 修改 Shared Kernel 型別，**所有依賴它的 Context 都要同步調整**
- 因此應盡量小、盡量穩定
- 不要把「只有一個 Context 用到」的型別放進來

→ 本專案的 Shared Kernel 實作見 [Bounded Context 與跨 Context 實作](01-ddd-impl-bounded-context.md#bounded-context-與-shared-kernel)

### Onion Architecture — 洋蔥分層

Onion Architecture 定義每個 Bounded Context 內部的分層規則，依賴方向**只能由外往內**，最內層純業務邏輯不依賴任何框架。

→ 詳細說明見 [Chapter 4：Onion Architecture](04-onion.md)

---

→ 本專案的實作對應見：
[DDD Building Blocks 實作對應](01-ddd-impl-building-blocks.md)、
[Bounded Context 與跨 Context 實作](01-ddd-impl-bounded-context.md)、
[DDD Violation Demo](01-ddd-impl-violations.md)
