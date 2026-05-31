# DDD Annotation Rules

- 每個 DDD building block 必須使用對應 jMolecules annotation。
- Aggregate Root 使用 `@AggregateRoot`，Entity 使用 `@Entity`，Value Object 使用 `@ValueObject`，Domain Event 使用 `@DomainEvent`，Repository 使用 `@Repository`。
- Repository 只能管理 Aggregate Root，不為 Entity 或 Value Object 建立 Repository。
- Value Object 必須不可變，優先使用 `record`；若使用 class，欄位必須為 `final` 且不可提供 setter。
- Domain Event 代表已發生事實，命名使用過去式，不包含 API request / response DTO、OpenAPI generated model 或 internal QueryModel。
- API DTO、OpenAPI generated model、Spring MVC interface 屬於 `infrastructure.web` adapter，不可標成 DDD building block。
- Aggregate Root 必須保護 invariant，外部不可透過 setter 或可修改 collection 繞過狀態轉換方法。
- 單一 Command Handler 原則上只修改一個 Aggregate；跨 Aggregate 後續動作用 event、Process Manager 或 Saga。
- 可被並發修改的 Aggregate 應有 optimistic locking / version 策略，並定義衝突時的錯誤、重試或重新載入規則。
- 悲觀鎖只能作為例外策略；使用時必須記錄原因、鎖範圍、timeout、deadlock 處理與測試。
- `*ProcessManager` / `*Saga` 只能用於有狀態、多步驟、跨 Aggregate / Context / 外部系統的流程協調。
