# Optimistic Locking and Concurrency

## Context

Aggregate 是一致性邊界，但多個 Command 仍可能同時修改同一個 Aggregate。
若沒有版本檢查，後寫入的 Command 可能覆蓋先寫入的狀態，造成 lost update。

## Pattern

對可被並發修改的 Aggregate 使用 optimistic locking：

```java
@Version
private Long version;
```

Command Handler 載入 Aggregate、呼叫 domain method、儲存。
若儲存時發生版本衝突，Application Service 依 use case 選擇：

- 重新載入後套用固定規則
- 回傳 conflict 錯誤
- 要求呼叫端重試
- 若 command 具備冪等語意，回傳既有結果

## Rules

- 不用跨 Aggregate 大交易掩蓋並發問題。
- Command 的冪等語意要和 optimistic locking 策略一致。
- 同一 Aggregate 的 concurrent command 要有測試。
- 版本欄位是 persistence concern，但它保護的是 Aggregate invariant。
- 悲觀鎖是例外手段，不是預設策略。
- 使用悲觀鎖前必須確認：高競爭、單一資料庫交易可控、鎖時間短、retry / compensation 不適合。
- 悲觀鎖必須定義 lock scope、timeout、deadlock handling、監控與壓力測試。

## Example

`PlaceOrderCommand` 若與 `CancelOrderCommand` 同時進來，其中一方成功後，另一方需要重新讀取最新狀態再依 domain rule 決定結果。
