---
description: jMolecules CQRS annotation 使用規則與 ArchUnit 驗證
paths:
  - src/main/java/**/*.java
  - src/test/**/*.java
---

# jMolecules CQRS Annotation 規則

套件：`org.jmolecules.architecture.cqrs`（非 `.annotation` 子套件，後者已棄用）

## Annotation 總覽

| Annotation | 套用對象 | 說明 |
|---|---|---|
| `@Command` | class / record | 操作意圖，必須不可變 |
| `@CommandHandler` | method | 處理 Command、改變狀態，只能在 application 層 |
| `@CommandDispatcher` | method | 分派 Command 給對應 Handler（method-level only） |
| `@QueryModel` | class | read side，不可觸發狀態改變 |

## 規則 #4：@Command 必須不可變

```java
// ✅ 正確：record（天生不可變）
@Command
public record CreateOrderCommand(CustomerId customerId) {}

// ❌ 違規：non-final field（BadCommand）
@Command
public class BadCommand {
    public CustomerId customerId; // 可被外部修改
}
```

- 用 `record` 是最直接的實作方式
- 若用 class，所有欄位必須 `final`，且不可有 setter

## 規則 #5：@QueryModel 不可呼叫 @CommandHandler

```java
// ✅ 正確：query 只讀取，不觸發狀態改變
@QueryModel
public class OrderSummaryView {
    public Order findById(OrderId id) {
        return orderRepository.findById(id).orElseThrow();
    }
}

// ❌ 違規：query 內呼叫 @CommandHandler（BadQueryModel）
@QueryModel
public class BadQueryModel {
    public Order findOrCreate(CustomerId customerId) {
        return orderService.handle(new CreateOrderCommand(customerId)); // 觸發 side effect
    }
}
```

- ArchUnit 透過 `MethodCallTarget.resolveMember()` 在 bytecode 中追蹤方法呼叫
- 即使間接呼叫鏈也應避免 Query → Command 的依賴

## 規則 #6：@Command 必須放在 application.command 套件

```
<bounded-context>/
  application/
    command/        ← @Command 唯一合法位置
```

```java
// ✅ 正確：package *.application.command
@Command
public record PlaceOrderCommand(OrderId orderId) {}

// ❌ 違規：放在 context root 或其他套件
@Command
public class BadCommand { ... }
```

## 規則 #7：@CommandHandler 只能在 application 層

```java
// ✅ 正確：在 *.application.* 套件
@Service
public class OrderService {
    @CommandHandler
    public Order handle(CreateOrderCommand command) { ... }
}

// ❌ 違規：在 domain 層或 context root
public class BadDomainHandler {
    @CommandHandler
    public Order handle(CreateOrderCommand command) { ... }
}
```

- `@CommandHandler` 協調 repository 與 domain event，屬應用層職責
- 出現在 domain 層代表應用層邏輯滲透進領域模型

## @CommandDispatcher 注意事項

- 只能套用在 **method**，不可套用在 class（Type target 不存在）
- 有必填屬性 `dispatches()`，指定分派的 Command 類名稱
- 實務上 `@CommandHandler` 已足夠表達意圖，`@CommandDispatcher` 屬選用標記

```java
// @CommandDispatcher 正確用法（method-level）
@CommandDispatcher(dispatches = "CreateOrderCommand")
public Order dispatch(CreateOrderCommand command) { ... }
```
