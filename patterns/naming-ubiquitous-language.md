# Naming and Ubiquitous Language

## Context

DDD 的模型要反映業務語言。若 class 名稱來自 API schema、資料表欄位或其他 Context 的語言，domain model 會慢慢變成整合格式，而不是業務模型。

## Pattern

命名從所在 Bounded Context 的 ubiquitous language 出發：

| 類型 | 命名方式 | 範例 |
|---|---|---|
| Command | 祈使句 | `PlaceOrderCommand` |
| Event | 過去式 | `OrderPlaced` |
| Query | 讀取語意 | `FindOrderById` |
| Projection | 查詢結果語意 | `OrderSummaryProjection` |
| Reference | 本 Context 對外部身分的語言 | `CustomerReference` |
| Process Manager / Saga | 業務流程名稱 | `OrderFulfillmentProcessManager` |

## Rules

- 不用 `Manager`、`Processor`、`Data`、`Helper` 這類模糊名稱。
- 外部 DTO / enum 進 domain 前先轉譯。
- 同名概念在不同 Context 可以有不同模型。
- 名稱要暴露 side effect：Command 會改變狀態，Query 不改變狀態，Event 是已發生事實。

## Why

好的命名讓新人從檔名和方法名就看出角色、邊界和副作用，減少靠口耳相傳維護架構規則。
