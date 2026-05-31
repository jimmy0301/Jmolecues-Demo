# CQRS Rules

- `@Command` 只能放在 `*.application.command` 套件，必須不可變。
- Command 是 application use case input，不是 HTTP request DTO 或 OpenAPI generated model。
- `@CommandHandler` 只在 application 層，負責載入 Aggregate、呼叫業務方法、儲存結果。
- 業務規則放在 Aggregate / Entity / Domain Service，不放在 Command Handler。
- Command Handler 優先回傳 `void`、ID、簡單 result 或 application result DTO；不直接為 API response 暴露 Aggregate。
- `@QueryModel` 只能讀取，不呼叫 `@CommandHandler`，不 save/delete，不 publish event，不呼叫 Aggregate mutating method。
- GET / query path 不 dispatch command，不造成狀態改變。
- Controller 負責 API DTO <-> Command / result DTO 的轉換。
