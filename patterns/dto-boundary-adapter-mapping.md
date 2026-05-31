# DTO Boundary and Adapter Mapping

## Context

Spring MVC、OpenAPI Generator、GraphQL 或 messaging framework 都會產生自己的 transport DTO。
這些 DTO 很容易因為欄位方便而一路傳進 application service 或 domain model，最後讓領域模型被 API contract 綁死。

## Pattern

把 DTO 分成三種語言：

| 類型 | 位置 | 語言 |
|---|---|---|
| Transport DTO | `infrastructure.*` | HTTP、JSON、OpenAPI、message schema |
| Use case input / result | `application.*` | Command、Query、application result |
| Domain model | `domain.*` | Aggregate、Entity、ValueObject、Domain Event |

Adapter 負責轉換：

```java
// transport -> use case
var command = new CreateOrderCommand(new CustomerReference(request.customerId()));

// domain/application result -> transport
var response = new OrderResponse(orderId.id(), status.name());
```

## Rules

- Transport DTO 只能留在 adapter 層。
- Application service 不接收 HTTP request DTO，不回傳 HTTP response DTO。
- Domain model 不 import、不保存、不回傳 transport DTO。
- Command / Query object 不直接拿來當 API schema。
- Public API DTO 和 integration event 要設計成穩定合約，不重用 internal QueryModel 或 web DTO。

## Why

- Domain 不會被 HTTP / OpenAPI / JSON 細節污染。
- API contract 可以演進，不會強迫 Aggregate 跟著改。
- Use case 的語言更接近業務意圖，review 時比較容易看出 side effect。
- 邊界轉換集中在 adapter，測試也更清楚。
