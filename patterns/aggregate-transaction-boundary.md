# Aggregate Transaction Boundary

## Context

DDD 中 Aggregate 是一致性邊界，也是最小交易邊界。新人常會在同一個 application service 裡同時修改多個 Aggregate，讓模型之間互相鎖死，最後演變成跨 Context 大交易。

## Pattern

一個 Command Handler 原則上只修改一個 Aggregate：

```java
Order order = orderRepository.findById(command.orderId()).orElseThrow();
order.place();
orderRepository.save(order);
```

其他 Aggregate 或 Context 的後續動作透過事件協調：

```text
Order.place()
  -> OrderPlaced
  -> Inventory listener reserves stock
  -> Customer listener updates reward points
```

## Rules

- Aggregate Root 保護 invariant，外部只能呼叫有業務語意的方法。
- Command Handler 不同時修改多個 Aggregate。
- 跨 Context 不直接開大交易；使用 Domain Event / Integration Event。
- 多步驟長流程使用 Process Manager / Saga。
- 每個 event consumer 必須冪等，能處理重送與重試。

## Why

- Aggregate 邊界清楚，業務規則不會散落在 application service。
- 避免跨 Context 耦合與分散式交易。
- 事件流程可擴充，新 consumer 不需要改 producer。
- 失敗可以用重試或補償處理，而不是把所有資料鎖在同一個交易。
