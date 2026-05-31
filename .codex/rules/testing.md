# Testing Rules

- 每次新增或修改 class 後執行 `mvn spotless:apply`。
- 架構相關異動後執行 `mvn test -Dtest=JMoleculesArchitectureTest`。
- Bounded Context 邊界異動後執行 `mvn test -Dtest=ModularityTest`。
- Controller 新增或修改時補 `@WebMvcTest`，測試放在相同 package：`infrastructure.web`。
- Controller 測試要驗證 API request DTO 會轉成正確 Command / Query 參數，response 不洩漏 Aggregate 內部細節。
- Application Service 測試用 mock Repository，驗證 Command Handler 流程與 Aggregate event registration。
- Domain Model 測試直接驗證業務規則與狀態轉換，不經由 Controller 或 API DTO。
- Domain Model 測試要覆蓋 Aggregate invariant 和非法狀態轉換。
- 若 Command 或 listener 宣告冪等，測試要覆蓋重送同一 command / event 的結果。
- Projection 測試要覆蓋重建與重複事件處理。
- 並發規則要有測試：同一 Aggregate 的 concurrent command、version conflict 或重送行為至少覆蓋一種。
- Process Manager / Saga 測試要覆蓋 happy path、重試、補償與重複事件。
