# Onion Architecture Rules

- 依賴方向只能由外往內：`infrastructure` -> `application` -> `domainservice` -> `domain`。
- 每個 ring 的 package 必須有對應 `package-info.java` 與 jMolecules Onion annotation。
- Domain Model 不依賴 Spring Web、OpenAPI generated DTO、Controller、Application Service 或 infrastructure implementation。
- Domain Service 只依賴 Domain Model，不處理資料庫、HTTP、訊息佇列或外部 API client。
- Application Service 可協調 Repository interface、Domain Service 與 Aggregate，但不可依賴 Controller、HTTP status、API request / response DTO 或 OpenAPI generated model。
- `infrastructure.web` 是 API adapter，負責把 HTTP DTO 轉成 Command、Query 參數或 application result。
- Aggregate / Entity / ValueObject 不接收、不回傳、不保存 API DTO。
