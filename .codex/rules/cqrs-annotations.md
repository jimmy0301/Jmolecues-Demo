# CQRS Rules

- `@Command` 只能放在 `*.application.command` 套件，必須不可變。
- Command 是 application use case input，不是 HTTP request DTO 或 OpenAPI generated model。
- `@CommandHandler` 只在 application 層，負責載入 Aggregate、呼叫業務方法、儲存結果。
- 業務規則放在 Aggregate / Entity / Domain Service，不放在 Command Handler。
- 一個 `@CommandHandler` 原則上只修改一個 Aggregate。
- 對外部可能重送的 Command，必須定義冪等策略或重送語意。
- 同一 Aggregate 可能收到並發 Command；Command Handler 必須定義 optimistic locking failure、retry 或 conflict response 的處理方式。
- 悲觀鎖只用於高競爭且 retry / compensation 不適合的 Command，並需明確 lock scope、timeout 與 deadlock handling；持鎖期間只能涵蓋載入 Aggregate、執行 domain method、儲存。
- Command Handler 優先回傳 `void`、ID、簡單 result 或 application result DTO；不直接為 API response 暴露 Aggregate。
- `@QueryModel` 只能讀取，不呼叫 `@CommandHandler`，不 save/delete，不 publish event，不呼叫 Aggregate mutating method。
- Projection / read model 可以 eventual consistent，但必須能重建且重複處理同一事件不產生重複資料。
- GET / query path 不 dispatch command，不造成狀態改變。
- Validation 分層：Controller/API DTO 處理 transport validation，Application 處理 use case validation，Domain 處理 invariant。
- Controller 負責 API DTO <-> Command / result DTO 的轉換。
- Command、Query、Event、Projection 命名必須揭露 side effect：Command 祈使句、Event 過去式、Query / Projection 讀取語意。
