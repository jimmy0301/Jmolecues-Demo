# Chapter 1.1：Bounded Context 與整合邊界

本章說明跨 Bounded Context 時的資料擁有權、公開合約、DTO 邊界與 Anti-Corruption Layer。
核心原則是：「可以取得資料」不等於「可以直接依賴對方模型」。

---

## Bounded Context — 業務邊界

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
- **語義衝突**：`Customer` 在 `ordering` 眼中是「下單的人」，在 `customer` 眼中是「有地址的帳戶」
- **部署耦合**：兩個業務域無法獨立演進

## 跨 Context 溝通方式

先看互動意圖，再選模式：

| 意圖 | 正確做法 | 不該做 |
|---|---|---|
| 保存關聯 | Reference Object，只保存對方 ID | 持有對方 Aggregate 物件 |
| 讀取展示資料 | 呼叫 owner 的 Query Facade，或使用本地 read model | import 對方 QueryModel / Repository |
| 要求對方改變狀態 | 呼叫 owner 的 Command Facade，或發布事件讓 owner 自己處理 | 直接修改對方資料庫 |
| 保存當下事實 | 在自己的 Aggregate 保存 Snapshot | 每次都查對方最新狀態來解釋歷史資料 |

一句話：**可以取得資料，但只能透過 owner Context 設計好的公開契約；取得後要轉成本 Context 的語言。**

## Reference Object — 保存關聯身分

只儲存對方 Aggregate 的識別碼，不持有物件。raw `UUID` 缺乏語義，可在自己的 domain 定義 Reference 物件：

```text
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

## Query Facade / Read Model — 取得跨 Context 資料

需要讀取另一個 Context 的展示資料時，透過 owner Context 暴露的公開查詢 API / Facade 取得。
不要 import 對方的 `domain`、`application`、`repository` 等 internal 型別。

```text
ordering ──queries──▶ catalog 的公開查詢介面
```

公開查詢介面應回傳穩定 DTO，例如 `ProductSummary`，而不是回傳 owner 的 Aggregate、Entity、Repository 或內部 QueryModel。

若一個 Context 經常需要另一個 Context 的資料，而且查詢量大或可以接受 eventual consistency，
可以訂閱 owner 的公開事件，在本 Context 建立 read model / projection。

## Command Facade / Domain Event — 更新跨 Context 資料

需要請另一個 Context 改變狀態時，不直接改對方資料庫，也不直接拿對方 Repository。
若 use case 必須同步知道成功/失敗，呼叫 owner 的公開 Command Facade；若只需通知已發生的事，發布事件讓對方自行處理：

```text
ordering ──publishes──▶ OrderPlaced ──consumed by──▶ inventory
```

事件代表「已發生的事實」，不是遠端方法呼叫。Producer 不應知道誰會處理事件。
當事件被其他 Context 消費時，它就成為跨 Context 合約，語意上接近 Integration Event，應避免包含 owner 的 internal domain 型別。

## Snapshot — 保存本 Context 的歷史事實

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

## Anti-Corruption Layer

Anti-Corruption Layer 是外部模型進入本 Context 前的轉譯層。
它可以是 controller mapper、client adapter、facade adapter 或 message translator。

規則：

- 外部 DTO / enum / 狀態碼不能直接進 domain
- 進入 application 前先轉成本 Context 的 Command、Query 參數或 application result
- 進入 domain 前只留下本 Context 的 ValueObject、Reference Object 或穩定 primitive
- 外部系統改欄位名或資料形狀時，優先影響 adapter，不影響 domain model

## Public DTO 的型別規則

公開 DTO 的目標是穩定契約，不是把 domain model 打包丟出去。

可以使用 `UUID`、`String`、`BigDecimal`、`Instant`、primitive / wrapper、owner 明確設計的 public DTO，
以及非常穩定的 Shared Kernel 型別（例如 `Money`，仍需謹慎）。

避免使用 owner 的 Aggregate、Entity、domain enum、ValueObject、Repository、Application Service、QueryModel 等 internal type。
簡單判斷：**如果 DTO 欄位型別需要 import 某個模組的 sub-package，通常就是洩漏。**

## Backward Compatibility — 公開合約相容性

跨 Context public API、Integration Event、public DTO 和 read model 都是合約。
合約變更預設採用向後相容策略，避免 producer / consumer 必須同時部署。

規則：

- 新增欄位優先設計為 optional，並提供預設行為
- 不直接移除欄位、不改欄位語意、不改 enum 值含義
- 需要移除欄位時，先標記 deprecated，等待所有 consumer 升級後再移除
- Integration Event 需要 event id、correlation id，並保留 schema version 或明確相容策略
- Consumer 必須能忽略未知欄位
- Producer 不要求所有 consumer 在同一個 release 同步升級

資料或合約變更採用 expand and contract：

1. 先新增新欄位 / 新 index / 新 event 欄位，舊流程仍可用
2. 執行可重跑的 backfill
3. 過渡期支援 dual read / dual write
4. 切換讀取路徑或啟用新功能
5. 確認 consumer 升級後，再移除舊欄位或舊行為

詳細 pattern 見 [Feature Toggle, Migration, and Backward Compatibility](../patterns/feature-toggle-migration-compatibility.md)。

## API DTO 與領域模型邊界

REST API request / response DTO、OpenAPI generated model、Spring MVC interface 都屬於 `infrastructure.web` adapter。
它們描述的是傳輸協議，不是領域語言，因此不能出現在 `domain`、`domainservice` 或 `application` 層。

Controller 的責任是做邊界轉換：

```java
// ✅ 正確：在 web adapter 轉成 application command
CreateOrderCommand command = new CreateOrderCommand(
        new CustomerReference(request.customerId()));
```

不要讓 API DTO 穿進 use case 或 Aggregate：

```java
// ❌ application service 不接收 HTTP request DTO
orderService.handle(CreateOrderRequest request);

// ❌ domain model 不持有 API response DTO
private OrderResponse response;
```

判斷方式：

- `infrastructure.web` 可以 import API DTO、OpenAPI generated model、application command / query object
- `application` 可以 import domain model、domain service、repository interface，但不可 import web / generated DTO
- `domain` 只使用本 Context 的領域型別、Shared Kernel、Java 標準型別，不知道 HTTP、JSON、OpenAPI
- API DTO 進入 application 前必須轉成 Command、Query 參數或 application result
- Aggregate / Entity / ValueObject 不接收、不回傳、不保存 API DTO

## Shared Kernel — 協議共用的型別

Shared Kernel 是 Bounded Context 規則的例外：
當多個 Context 都需要同一個概念（如金額 `Money`），將它放在共享的小型 package，各 Context 都可以依賴。

限制：

- 修改 Shared Kernel 型別，所有依賴它的 Context 都要同步調整
- 因此應盡量小、盡量穩定
- 不要把只有一個 Context 用到的型別放進來

## 常見反模式

- 直接 import 另一個 Context 的 `domain` / `application` / `repository` package
- 跨 Context 使用 Repository
- 直接修改另一個 Context 的資料庫
- 使用外部系統詞彙或 API 欄位名污染本 Context 的 ubiquitous language
- 公開 DTO 洩漏 owner 的 domain type
- 把內部實作細節包成公開合約，而不是設計穩定的跨 Context API
- 在沒有 deprecation / migration 流程下移除 public DTO 或 Integration Event 欄位
- 改變 enum 值語意，導致舊 consumer 用舊邏輯解讀新資料
- API request / response DTO 穿透到 application 或 domain 層
- 直接把 OpenAPI generated model 標成 `@Command`、`@ValueObject` 或 `@DomainEvent`

---

實作對應見 [Bounded Context 與跨 Context 實作](01-ddd-impl-bounded-context.md)。
