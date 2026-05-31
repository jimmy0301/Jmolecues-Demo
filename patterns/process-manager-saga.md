# Process Manager and Saga

## Context

有些業務流程不能在單一 Aggregate 交易中完成，例如下單後要預留庫存、付款授權、通知出貨。
這些步驟跨 Context、跨外部系統，也可能需要重試和補償。

## Pattern

使用 Process Manager / Saga 保存流程狀態並推進下一步：

```text
OrderPlaced
  -> OrderFulfillmentProcessManager
  -> ReserveInventoryCommand
  -> PaymentAuthorizationRequested
  -> ShipOrderCommand
```

Process Manager / Saga 不擁有其他 Context 的資料，只保存流程狀態，例如 process id、correlation id、目前 step、已完成步驟、補償狀態。

## Rules

- 只有有狀態、多步驟流程才使用 `*ProcessManager` 或 `*Saga`。
- 單純事件反應仍命名為 listener。
- 只呼叫 owner Context 的公開 Command Facade 或發布 command/event。
- 不直接修改其他 Context 的 Repository 或資料庫。
- 每一步都要能重試或補償。
- 必須具備冪等性，同一事件重送不建立重複流程。
- 測試至少覆蓋 happy path、重送事件、單步失敗與補償。

## Why

流程狀態集中管理，Aggregate 邊界維持乾淨，跨 Context 失敗可以重試或補償，而不是依賴分散式大交易。
