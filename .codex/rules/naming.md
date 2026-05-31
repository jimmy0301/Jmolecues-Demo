# Naming and Ubiquitous Language Rules

- Class、method、Command、Event、Query、DTO 名稱都使用所在 Bounded Context 的 ubiquitous language。
- 外部系統、其他 Context、API schema 的詞彙不可直接污染 domain；先透過 Anti-Corruption Layer 轉譯。
- Command 用祈使句，表達操作意圖，例如 `PlaceOrderCommand`。
- Event 用過去式，表達已發生事實，例如 `OrderPlaced`。
- Query 用讀取語意，例如 `FindOrderById`、`GetCustomerOrders`。
- Projection / read model 用查詢結果語意，例如 `OrderSummary`、`OrderSummaryProjection`。
- 避免 `Manager`、`Processor`、`Data`、`Helper` 這類模糊名稱。
- 只有有狀態、多步驟流程協調者才能命名為 `*ProcessManager` 或 `*Saga`。
- 同一個詞跨 Context 意義不同時，各 Context 各自命名，不共用對方 domain type。
