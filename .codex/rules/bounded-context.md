# Bounded Context Rules

- 跨 Context 用 Reference Object 保存關聯，例如 `@ValueObject record ProductReference(UUID id)`。
- 不 import 其他 Context 的 `domain`、`application`、`repository` package。
- 跨 Context 讀取資料只透過 owner Context 的 `publicapi`、Query Facade、read model 或 snapshot。
- 跨 Context 更新狀態只透過 owner Context 的 Command Facade 或事件，不直接使用對方 Repository 或資料庫。
- Public DTO 是穩定公開合約，不回傳 Aggregate、Entity、Repository、internal Value Object、Application Service 或 QueryModel。
- Public DTO 不重用 HTTP request / response DTO 或 OpenAPI generated model。
- Integration Event / public event 不包含 owner Context 的 internal domain type。
- Integration Event / public event consumer 必須冪等，能處理重送、重試與順序不保證。
- Public API、Integration Event、public DTO 預設只做 additive change；breaking change 必須有 deprecation 與 migration 流程。
- Consumer 必須能忽略未知欄位；producer 不要求所有 consumer 同步部署。
- Data migration 使用 Flamingock Change；migration class 不放在 domain layer，不直接建立跨 Context internal type 依賴。
- 外部系統或其他 Context 的模型必須透過 Anti-Corruption Layer 轉成本 Context 的語言。
- Process Manager / Saga 只透過 owner Context 的公開合約推進流程，不直接修改其他 Context 的資料庫。
- 跨 Context 流程必須有 correlation id / process id、重試策略與補償策略。
- 歷史資料需要穩定呈現時，在本 Context 保存 snapshot。
