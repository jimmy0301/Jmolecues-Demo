# 領域模型說明

本文件定義各 Bounded Context 的 Ubiquitous Language（統一語言）與核心概念，供開發者在撰寫程式碼、討論需求、命名類別時參照。

---

## Bounded Context 地圖

```
┌─────────────┐        ID 參照        ┌──────────────────┐
│   catalog   │ ◄─── ProductId ───── │    ordering      │
│             │                       │                  │
│  Product    │                       │  Order           │
│  Money      │                       │  OrderItem       │
└─────────────┘                       │  PricingService  │
                                      └──────────────────┘
┌─────────────┐        ID 參照               ▲
│  customer   │ ◄─── CustomerId ────────────┘
│             │
│  Customer   │
│  Address    │
└─────────────┘

跨 Context 只允許 ID 參照，禁止直接持有其他 Context 的 Aggregate 物件。
```

---

## catalog Context

### Ubiquitous Language

| 術語 | 中文 | 定義 |
|---|---|---|
| Product | 商品 | 電商平台上可供販售的品項，具有唯一識別碼、名稱與售價 |
| ProductId | 商品識別碼 | 商品的唯一識別，UUID 包裝型別 |
| Money | 金額 | 由數值（BigDecimal）與幣別（String）組成的不可變值物件，支援加法與乘法 |
| Price | 定價 | 商品的售出金額，以 Money 表示 |

### 模型圖

```
Product (AggregateRoot)
├── id: ProductId          @Identity
├── name: String
└── price: Money           @ValueObject — { amount: BigDecimal, currency: String }
```

### 業務規則

- 商品一旦建立，只能透過 Product 自身的方法變更定價
- Money 不可變，計算時返回新的 Money 實例
- 跨 Context 使用商品資訊時，只傳遞 ProductId

---

## customer Context

### Ubiquitous Language

| 術語 | 中文 | 定義 |
|---|---|---|
| Customer | 顧客 | 在平台上擁有帳號的使用者，具有基本個人資料與地址 |
| CustomerId | 顧客識別碼 | 顧客的唯一識別，UUID 包裝型別 |
| Address | 地址 | 由街道、城市、郵遞區號、國家組成的不可變值物件 |

### 模型圖

```
Customer (AggregateRoot)
├── id: CustomerId         @Identity
├── name: String
├── email: String
└── address: Address       @ValueObject — { street, city, postalCode, country }
```

### 業務規則

- 每位顧客有唯一的 email
- 地址是值物件，更新地址等同於替換整個 Address 實例
- 跨 Context 使用顧客資訊時，只傳遞 CustomerId

---

## ordering Context

### Ubiquitous Language

| 術語 | 中文 | 定義 |
|---|---|---|
| Order | 訂單 | 顧客發出的購買請求，包含一或多個訂單項目，具有明確的生命週期 |
| OrderId | 訂單識別碼 | 訂單的唯一識別，UUID 包裝型別 |
| OrderItem | 訂單項目 | 訂單中的單一商品記錄，含商品 ID、數量、單價 |
| OrderItemId | 訂單項目識別碼 | 訂單項目的唯一識別 |
| Quantity | 數量 | 正整數的值物件，不可為零或負數 |
| OrderStatus | 訂單狀態 | 訂單當前所處的生命週期階段 |
| Place（下單） | 確認訂單 | 將訂單從 PENDING 推進至 PLACED，觸發 OrderPlaced 事件 |
| Cancel（取消） | 取消訂單 | 將訂單推進至 CANCELLED，觸發 OrderCancelled 事件 |
| OrderPlaced | 訂單已成立 | 下單成功後發布的領域事件 |
| OrderCancelled | 訂單已取消 | 取消訂單後發布的領域事件 |
| PricingService | 定價服務 | 計算訂單總價的領域服務，封裝跨 OrderItem 的加總邏輯 |

### 模型圖

```
Order (AggregateRoot)
├── id: OrderId                @Identity
├── customerId: CustomerId     跨 Context ID 參照（不持有 Customer 物件）
├── items: List<OrderItem>
└── status: OrderStatus        PENDING → PLACED → CANCELLED

  OrderItem (Entity)
  ├── id: OrderItemId          @Identity（不加 @Id，非 MongoDB document root）
  ├── productId: ProductId     跨 Context ID 參照（不持有 Product 物件）
  ├── quantity: Quantity       @ValueObject — { value: int > 0 }
  └── unitPrice: Money         @ValueObject

OrderStatus (Enum)
├── PENDING    訂單建立，尚未確認
├── PLACED     訂單已確認，等待出貨
└── CANCELLED  訂單已取消
```

### 訂單生命週期

```
建立 Order（PENDING）
    │
    ▼
addItem() ──► 可重複加入 OrderItem
    │
    ▼
place()  ──► PLACED，發布 OrderPlaced
    │
    ▼
cancel() ──► CANCELLED，發布 OrderCancelled
```

### 業務規則

- 只有 PENDING 狀態的訂單可以加入 OrderItem
- 已 CANCELLED 的訂單不能再次取消
- 訂單總價由 PricingService 計算，不在 Order 本身維護
- 所有訂單操作須透過 Order 的方法進行，不可繞過直接操作 OrderItem

---

## 跨 Context 規則

| 規則 | 說明 |
|---|---|
| ID 參照 | 跨 Context 只允許傳遞 ID（`CustomerId`、`ProductId`），不持有對方的 Aggregate 物件 |
| 無直接呼叫 | Context 之間不互相呼叫 Repository，透過 Domain Event 通知 |
| 邊界強制 | ArchUnit `shouldFollowDddRules` 自動驗證，違規在 CI 階段阻擋 |
