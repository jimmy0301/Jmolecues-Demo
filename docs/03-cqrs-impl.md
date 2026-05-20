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
catalog/
  application/
    ProductService.java        ← @CommandHandler methods
    ProductQueryModel.java     ← @QueryModel
    command/
      CreateProductCommand.java ← @Command

customer/
  application/
    CustomerService.java       ← @CommandHandler methods
    CustomerQueryModel.java    ← @QueryModel
    command/
      CreateCustomerCommand.java ← @Command

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
        order.place();                            // 業務規則在 Order，event 已登記在 aggregate
        return orderRepository.save(order);       // Spring Data 在 save() 時自動發布已登記的 event
    }

    @CommandHandler
    public Order handle(CancelOrderCommand command) {
        Order order = findOrder(command.orderId());
        order.cancel();
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

## 單元測試

Application Service 用 Mockito mock Repository，驗證 command handler 流程；event 驗證改為捕捉傳入 `save()` 的 aggregate，再檢查 `getRegisteredEvents()`：

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @InjectMocks private OrderService orderService;

    @Test
    void handle_placeOrderCommand_registersEventAndSaves() {
        var order = new Order(UUID.randomUUID());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = orderService.handle(new PlaceOrderCommand(order.getId()));

        var orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        var savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getRegisteredEvents()).hasSize(1);
        var event = (OrderPlaced) savedOrder.getRegisteredEvents().iterator().next();
        assertThat(event.orderId()).isEqualTo(order.getId());
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PLACED);
    }
}
```

測試檔案：[`OrderServiceTest`](../src/test/java/com/example/demo/ordering/application/OrderServiceTest.java)、[`ProductServiceTest`](../src/test/java/com/example/demo/catalog/application/ProductServiceTest.java)、[`CustomerServiceTest`](../src/test/java/com/example/demo/customer/application/CustomerServiceTest.java)

## Controller API 測試

Controller 用 `@WebMvcTest` 做 HTTP slice 測試，只載入 web 層，不啟動 MongoDB。Application Service 與 QueryModel 以 `@MockBean` 替換：

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean OrderService orderService;
    @MockBean OrderQueryModel orderQueryModel;

    @Test
    void createOrder_returns201WithPendingStatus() throws Exception {
        var customerId = UUID.randomUUID();
        var order = new Order(customerId);
        when(orderService.handle(any(CreateOrderCommand.class))).thenReturn(order);

        mockMvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"customerId\":\"" + customerId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void placeOrder_returns200WithPlacedStatus() throws Exception {
        var order = new Order(UUID.randomUUID());
        order.place();
        when(orderService.handle(any(PlaceOrderCommand.class))).thenReturn(order);

        mockMvc.perform(post("/orders/{id}/place", order.getId().id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"));
    }
}
```

測試放在與 Controller **相同的 package**（`infrastructure.web`）以存取 package-private class。

測試檔案：[`OrderControllerTest`](../src/test/java/com/example/demo/ordering/infrastructure/web/OrderControllerTest.java)、[`ProductControllerTest`](../src/test/java/com/example/demo/catalog/infrastructure/web/ProductControllerTest.java)、[`CustomerControllerTest`](../src/test/java/com/example/demo/customer/infrastructure/web/CustomerControllerTest.java)

---

## Violation Demo

以下 class 刻意違規，ArchUnit 會自動偵測：

| Class | 違規 | 觸發測試 |
|---|---|---|
| [`BadCommand`](../src/main/java/com/example/demo/ordering/BadCommand.java) | ① `@Command` 有 public 可變欄位；② 放在 context root 而非 `application.command` | `commandsShouldBeImmutable`、`commandsShouldResideInCommandPackage` |
| [`BadQueryModel`](../src/main/java/com/example/demo/ordering/BadQueryModel.java) | `@QueryModel` 呼叫 `@CommandHandler` | `queryModelsShouldNotTriggerCommands` |
| [`BadDomainHandler`](../src/main/java/com/example/demo/ordering/BadDomainHandler.java) | `@CommandHandler` 在 domain 層 | `commandHandlersShouldBeInApplicationLayer` |
