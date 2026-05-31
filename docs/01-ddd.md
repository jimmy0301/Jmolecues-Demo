# Chapter 1：Domain-Driven Design 核心概念

本章說明 DDD 的核心語言與基本 building blocks。
跨 Context 整合、並發、事件與流程協調分別由延伸章節說明。

---

## 什麼是 DDD？

DDD 是一套把「業務邏輯」放在程式設計核心的方法論，核心思想是：

> **讓程式碼的語言與業務的語言一致。**

傳統做法容易讓 `Order` 退化成只有 getter/setter 的資料袋，業務邏輯散落在各處的 Service。
DDD 的做法是讓 `Order` 真正知道自己可以 `place()`（下單）、`cancel()`（取消），
並在狀態改變時發布 `OrderPlaced` 事件，讓程式碼本身就能說明業務意圖。

閱讀本章時，先掌握三個問題：

- 這段邏輯屬於哪個業務概念？
- 這個物件能不能被外部直接修改？
- 跨 Bounded Context 時，誰擁有資料、誰只能透過合約使用資料？

---

## Building Blocks

DDD 定義了幾種戰術模式（Tactical Patterns），用來組織領域物件。

### Aggregate Root

Aggregate Root 是一群相關物件的統一入口，負責維護一致性邊界。
外部不能隨意操作 Aggregate 內部的 Entity 或 Value Object，只能透過 Aggregate Root 暴露的方法改變狀態。

例如訂單模型中，外部應該呼叫 `Order.addItem(...)`、`Order.place()`，而不是直接修改 `OrderItem` 清單。

Aggregate Root 的核心責任是保護 invariant（永遠必須成立的業務規則）。
例如「已取消訂單不能再次成立」、「訂單項目數量必須大於 0」、「已成立訂單的成交價格不能被商品後續調價影響」。
這些規則要放在 Aggregate / Entity / Value Object / Domain Service 中，不放在 Controller 或 Command Handler。

外部也不應取得可修改的內部集合：

```java
// ✅ 正確：透過有業務語意的方法改變狀態
order.addItem(productRef, productName, unitPrice, quantity);
order.place();

// ❌ 錯誤：繞過 Aggregate Root 的一致性規則
order.getItems().add(item);
order.setStatus(OrderStatus.PLACED);
```

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

Domain Event 和 Integration Event 的差異見 [一致性、並發與流程協調](01-ddd-consistency-workflows.md#domain-event-vs-integration-event)。

### Repository

Repository 是 Aggregate Root 的持久化抽象，封裝儲存與查詢細節。
Repository 不應為 Entity 或 Value Object 建立；外部若需要操作 Aggregate 內部物件，應先取得 Aggregate Root。

例如應該有 `OrderRepository`，不應該有 `OrderItemRepository`。

### Domain Service

Domain Service 放置不自然屬於任一 Aggregate 的領域邏輯，通常是無狀態的。
它仍屬於 Domain 層，不應處理資料庫、HTTP 或訊息佇列等技術細節。

例如計算多個訂單項目的總價，可以放在 `PricingService`，而不是塞進 Controller 或 Repository。

若邏輯只是單一 Aggregate 的狀態轉換，優先放在 Aggregate 方法。
只有當規則橫跨多個 domain object，且不自然屬於任一物件時，才抽成 Domain Service。

### Bounded Context

Bounded Context 是一個業務子域的邊界，有自己的模型、語言與規則。
同一個詞在不同 Context 裡可以有不同意義，因此跨 Context 不應直接共用對方的 domain model。

例如 `Customer` 在 customer context 可能代表會員帳戶與地址，在 ordering context 可能只代表「下單者的 reference」。

每個 Bounded Context 都要保護自己的 Ubiquitous Language（通用語言）。
外部系統或其他 Context 的詞彙進來時，要透過 public API、adapter 或 mapper 轉成本 Context 的語言；這層轉換就是 Anti-Corruption Layer（防腐層）。
不要讓對方的欄位名稱、狀態 enum 或資料結構直接污染自己的 domain model。

命名也要跟著 Bounded Context 的語言走：

- 類別、方法、事件、Command、Query 都用業務詞彙，不用資料庫或 API payload 的欄位名硬套
- 避免 `Manager`、`Processor`、`Data` 這類模糊名稱；若使用 `ProcessManager`，必須真的是長流程協調角色
- 同一個詞跨 Context 意義不同時，各 Context 各自命名，例如 `Customer` 與 `CustomerReference`
- 外部詞彙進入本 Context 前先轉譯，不直接把外部 enum / DTO 名稱帶進 domain

### Shared Kernel

Shared Kernel 是多個 Context 協議共用的一小塊模型。
它是 Bounded Context 隔離規則的例外，因此應該盡量小、穩定，且修改時要讓所有依賴方一起承擔影響。

例如 `Money` 若在多個 Context 中語義完全一致，可以放進 Shared Kernel；若語義不同，就應各自定義。

---

## 延伸章節

| 主題 | 文件 |
|---|---|
| 跨 Context 資料擁有權、Reference Object、Snapshot、API DTO 邊界 | [Bounded Context 與整合邊界](01-ddd-context-integration.md) |
| Aggregate 交易邊界、Domain Event、Listener、並發鎖、Process Manager / Saga | [一致性、並發與流程協調](01-ddd-consistency-workflows.md) |
| Onion ring、依賴方向、layer DTO 邊界 | [Onion Architecture](04-onion.md) |
| Command / Query 分離、validation、projection、eventual consistency | [CQRS](03-cqrs.md) |

## 實作對應

- [DDD Building Blocks 實作對應](01-ddd-impl-building-blocks.md)
- [Bounded Context 與跨 Context 實作](01-ddd-impl-bounded-context.md)
- [DDD Violation Demo](01-ddd-impl-violations.md)
