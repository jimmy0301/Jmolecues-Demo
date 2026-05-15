# CQRS 實作說明 — jMolecules Annotations

本文件說明本專案如何用 jMolecules CQRS annotations 實踐 [CQRS 概念](03-cqrs.md)。

---

## Annotation 總覽

| Annotation | 套用對象 | 職責 |
|---|---|---|
| `@Command` | class / record | 封裝操作意圖，必須不可變，放在 `application.command` 套件 |
| `@CommandHandler` | method | 接收 Command 執行狀態改變，只能在 application 層 |
| `@QueryModel` | class | 只讀取，不可呼叫任何 `@CommandHandler` |
| `@CommandDispatcher` | method | 將 Command 分派給 Handler（選用標記） |

套件：`org.jmolecules.architecture.cqrs`

---

## 本專案結構

```
ordering/
  application/
    OrderService.java          ← @CommandHandler methods
    OrderQueryModel.java       ← @QueryModel
    command/
      CreateOrderCommand.java  ← @Command
      PlaceOrderCommand.java   ← @Command
      CancelOrderCommand.java  ← @Command
```

---

## @Command

Command 必須**不可變**，且只能放在 `*.application.command` 套件。

```java
// ✅ 正確：record 天生不可變，位於 application.command
@Command
public record PlaceOrderCommand(OrderId orderId) {}

// ✅ 正確：final fields
@Command
public class PlaceOrderCommand {
    private final OrderId orderId;
    public PlaceOrderCommand(OrderId orderId) { this.orderId = orderId; }
}
```

---

## @CommandHandler

只能在 application 層（`*.application.*`）。Handler 協調 Repository 與 Event，業務邏輯留在 Aggregate。

```java
@Service
public class OrderService {

    @CommandHandler
    public Order handle(CreateOrderCommand command) {
        return orderRepository.save(new Order(command.customerId()));
    }

    @CommandHandler
    public Order handle(PlaceOrderCommand command) {
        Order order = findOrder(command.orderId());
        events.publishEvent(order.place());   // 業務規則在 Order，不在這裡
        return orderRepository.save(order);
    }

    @CommandHandler
    public Order handle(CancelOrderCommand command) {
        Order order = findOrder(command.orderId());
        events.publishEvent(order.cancel());
        return orderRepository.save(order);
    }
}
```

---

## @QueryModel

不可呼叫任何 `@CommandHandler` 方法。

```java
// ✅ 正確：只讀
@QueryModel
@Service
public class OrderQueryModel {
    public Optional<Order> findById(OrderId id) {
        return orderRepository.findById(id);
    }
    public List<Order> findAll() {
        return orderRepository.findAll();
    }
}
```

---

## Violation Demo

以下 class 刻意違規，ArchUnit 會自動偵測：

| Class | 違規 | 觸發測試 |
|---|---|---|
| [`BadCommand`](../src/main/java/com/example/demo/ordering/BadCommand.java) | ① `@Command` 有 public 可變欄位；② 放在 context root 而非 `application.command` | `commandsShouldBeImmutable`、`commandsShouldResideInCommandPackage` |
| [`BadQueryModel`](../src/main/java/com/example/demo/ordering/BadQueryModel.java) | `@QueryModel` 呼叫 `@CommandHandler` | `queryModelsShouldNotTriggerCommands` |
| [`BadDomainHandler`](../src/main/java/com/example/demo/ordering/BadDomainHandler.java) | `@CommandHandler` 在 domain 層 | `commandHandlersShouldBeInApplicationLayer` |
