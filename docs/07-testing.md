# Chapter 7：測試策略與實作

本專案的測試依**架構層**切分：每一層只載入它需要的最小範圍，越內層越不依賴 Spring、越不需要 mock。

規則的權威來源是 [`.claude/rules/testing.md`](../.claude/rules/testing.md)；這一章說明「為什麼這樣分」「實際長什麼樣」「怎麼跑」，以及踩過的坑。

---

## 測試總覽

| 層 | 測什麼 | 框架 | Spring context | mock | 需要 Docker | 範例 |
|---|---|---|---|---|---|---|
| Domain Model | Aggregate 狀態機、不變量、已登記的事件 | JUnit 5 + AssertJ | 無 | 無，直接 `new` | 否 | `OrderTest`、`OrderItemTest` |
| Shared Kernel | Value Object 行為 | JUnit 5 + AssertJ | 無 | 無 | 否 | `MoneyTest` |
| Domain Service | 跨 Aggregate 的無狀態邏輯 | JUnit 5 + AssertJ | 無 | 無 | 否 | `PricingServiceTest` |
| Application Service | Command 處理、儲存、事件登記 | JUnit 5 + Mockito | 無 | 只 mock Repository | 否 | `OrderServiceTest` |
| Projection / Facade | 事件投影（含冪等）、`publicapi` 對應 | JUnit 5 + AssertJ | 無 | 無或手寫 stub | 否 | `OrderSummaryProjectionTest`、`ProductQueryFacadeTest` |
| Controller | HTTP 狀態碼、JSON 結構、驗證 | `@WebMvcTest` + MockMvc | web slice | `@MockitoBean` Service / QueryModel | 否 | `OrderControllerTest` |
| 第三方 API adapter | 外部 HTTP contract 的 mapping、錯誤處理 | WireMock 或 `@RestClientTest` | 無或 slice | 假的 HTTP server | 否 | 見下方（專案尚無範例） |
| Repository | 與真實 MongoDB 的讀寫與版本檢查 | `@DataMongoTest` + Testcontainers | data slice | 無 | **是** | `OrderRepositoryTest` |
| 架構 | DDD / Onion / CQRS 規則、模組邊界 | ArchUnit + Spring Modulith | 無 | 無 | 否 | `JMoleculesArchitectureTest`、`ModularityTest` |

**原則：能不啟動 Spring 就不啟動；需要真實行為（資料庫、版本檢查）時，用真實的依賴，不要 mock。**

---

## 速查：各類測試用什麼 package

> 以 Spring Boot 4.1.2 為準。Boot 4 把測試 slice 拆成獨立模組，**package 與 Boot 3 不同**，不要從舊文章複製 import。

| 要測的東西 | 測試類型 | 主要 import（package） | Maven 依賴（`test` scope） | 測試檔放哪個 package |
|---|---|---|---|---|
| Domain、Domain Service、Shared Kernel | 純單元測試 | `org.junit.jupiter.api.Test`<br>`org.assertj.core.api.Assertions` | `spring-boot-starter-test` | 與被測類別相同：`*.domain`、`*.domainservice`、`shared` |
| Application Service | Mockito 單元測試 | 上列加<br>`org.mockito.junit.jupiter.MockitoExtension`<br>`org.mockito.Mock` / `InjectMocks` / `ArgumentCaptor` | `spring-boot-starter-test` | `*.application` |
| **Controller** | `@WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`<br>`org.springframework.test.web.servlet.MockMvc`<br>`org.springframework.test.context.bean.override.mockito.MockitoBean` | `spring-boot-starter-webmvc-test` | **`*.infrastructure.web`**（與 Controller 同 package） |
| **Repository** | `@DataMongoTest` + Testcontainers | `org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest`<br>`org.springframework.boot.testcontainers.service.connection.ServiceConnection`<br>`org.testcontainers.mongodb.MongoDBContainer` | `spring-boot-starter-data-mongodb-test`<br>`spring-boot-testcontainers`<br>`testcontainers-mongodb` | `*.domain`（Repository 介面所在）；共用基底類別在 `com.example.demo` |
| **第三方 API adapter**（真實 HTTP） | WireMock | `com.github.tomakehurst.wiremock.junit5.WireMockTest`<br>`com.github.tomakehurst.wiremock.client.WireMock` | `org.wiremock:wiremock-standalone`（**要自己寫版本**，Boot BOM 不管理；目前用 `3.13.2`）<br>⚠️ pom 尚未加入 | adapter 所在 package：`*.infrastructure.<adapter>` |
| **第三方 API adapter**（Spring slice） | `@RestClientTest` | `org.springframework.boot.restclient.test.autoconfigure.RestClientTest`<br>`org.springframework.test.web.client.MockRestServiceServer` | `spring-boot-starter-restclient-test`<br>（main 端要 `spring-boot-starter-restclient`）<br>⚠️ pom 尚未加入 | 同上 |
| 架構規則 | ArchUnit + Modulith | `org.jmolecules.archunit.JMoleculesDddRules` / `JMoleculesArchitectureRules`<br>`com.tngtech.archunit.*`<br>`org.springframework.modulith.core.ApplicationModules` | `jmolecules-archunit`<br>`spring-modulith-starter-test` | `com.example.demo.arch` |

"測試檔放哪個 package" 的共同原則：**與被測類別同 package**（Controller 尤其必要，因為 Controller 是 package-private）；跨類別的架構測試與共用基底類別放在專屬位置。

---

## Domain 與 Domain Service：不 mock，直接 new

Domain 物件不依賴框架，測試只驗證**狀態**與**已登記的事件**，不驗證持久化：

```java
class OrderTest {

    @Test
    void newOrder_isPending() {
        var order = new Order(CUSTOMER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
```

不需要 `@ExtendWith`。事件用 `order.getRegisteredEvents()` 檢查（`Order` 繼承 `AbstractAggregateRoot`，`place()` / `cancel()` 呼叫 `registerEvent()`），不需要 `ApplicationEventPublisher`。

---

## Application Service：只 mock Repository

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @InjectMocks private OrderService orderService;

    @Test
    void handle_placeOrderCommand_registersEventAndSaves() {
        var order = new Order(UUID.randomUUID());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = orderService.handle(new PlaceOrderCommand(order.getId()));

        var captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getRegisteredEvents()).hasSize(1);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PLACED);
    }
}
```

- mock 的只有 Repository；**不 mock domain 物件**，也**不 mock `ApplicationEventPublisher`**。
- 事件是否發布，用儲存的 Aggregate 上的 `getRegisteredEvents()` 驗證。

### Projection 與 Facade

- `OrderSummaryProjectionTest`：直接 `new OrderSummaryProjection()`，對同一個 `OrderPlaced` 事件呼叫兩次，驗證**冪等**（只產生一筆 summary）。
- `ProductQueryFacadeTest`：`publicapi` Facade 依賴 `ProductQueryModel`，測試用手寫的 `StubProductQueryModel` 取代 Mockito，驗證 domain 物件被對應成對外公開的 `ProductSummary`，且找不到時回傳 empty。

---

## Controller：`@WebMvcTest` 只測 web 層

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean OrderService orderService;
    @MockitoBean OrderQueryModel orderQueryModel;

    @Test
    void getOrderById_whenNotFound_returns404() throws Exception {
        when(orderQueryModel.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
```

- 不啟動完整 context，**不需要 MongoDB**。
- 測試放在與 Controller **相同的 package**（`infrastructure.web`），才能存取 package-private 的 Controller。
- Application Service 與 QueryModel 一律 `@MockitoBean`。每個 Controller 至少涵蓋：成功路徑（2xx）、not found（404）、請求 body 欄位驗證。
- Boot 4 已移除 `@MockBean`，改用 `@MockitoBean`；`@WebMvcTest` 位於 `org.springframework.boot.webmvc.test.autoconfigure`，需要 `spring-boot-starter-webmvc-test`。

---

## Repository 整合測試：用真實 MongoDB

Repository 測試繼承 `MongoIntegrationTest`，由 Testcontainers 啟動 `mongo:7`：

```java
@DataMongoTest
@Import(MongoTestContainersConfig.class)
public abstract class MongoIntegrationTest {}

@TestConfiguration(proxyBeanMethods = false)
class MongoTestContainersConfig {

    @Bean
    @ServiceConnection   // 自動把容器連線資訊注入 Spring Data，不需手動設 spring.mongodb.*
    MongoDBContainer mongoDBContainer() { return new MongoDBContainer("mongo:7"); }
}
```

- **需要 Docker**，且**包含在 `mvn test`**：沒開 Docker，這些測試會失敗，整個 build 也會失敗。
- 相同設定的 Spring test context 會被快取，容器只啟動一次（第一個 Repository 測試約 3 秒，之後各類別是毫秒級）。
- 每個測試的 `@BeforeEach` 先 `deleteAll()`，測試之間不共用資料。
- `MongoDBContainer` 要用 `org.testcontainers.mongodb`（Testcontainers 2.x），詳見 [Testcontainers 說明](02-tech-stack.md#testcontainers--repository-整合測試)。

### 為什麼樂觀鎖一定要用真實資料庫測

`OrderRepositoryTest#save_withStaleVersion_throwsOptimisticLockingFailure` 先後載入兩份同一個 `Order`，第一份儲存後，用過期的那份再儲存，預期得到 `OptimisticLockingFailureException`：

```java
var saved = orderRepository.save(new Order(UUID.randomUUID()));
var firstCopy = orderRepository.findById(saved.getId()).orElseThrow();
var staleCopy = orderRepository.findById(saved.getId()).orElseThrow();

firstCopy.place();
orderRepository.save(firstCopy);
staleCopy.cancel();

assertThatThrownBy(() -> orderRepository.save(staleCopy))
        .isInstanceOf(OptimisticLockingFailureException.class);
```

這個行為取決於 jMolecules ByteBuddy、Spring Data 與 `NotNewCallback` bean 的互動，**mock 的 Repository 完全測不到**。這個測試曾經失敗，原因就是 `NotNewCallback` 沒註冊：`@Version` 靜默失效，載入的 Aggregate 被當成新物件而 insert（`DuplicateKeyException`）。詳見 [技術棧](02-tech-stack.md) 的 ByteBuddy 一節。

---

## 第三方 API：測 infrastructure adapter

> **目前專案沒有真實的外部 HTTP adapter**，這一節的範例用假想的 `ExternalPriceClient`（用 `RestClient` 呼叫 `GET /prices/{sku}`）。三種寫法都已在 Boot 4.1.2 + WireMock 3.13.2 上實際跑過，但程式碼不在本專案內；第一次實作真實 adapter 時再把依賴加進 pom。

### adapter 與測試的位置

- adapter 放在 `<context>/infrastructure/<名稱>/`（例如 `catalog.infrastructure.client`），該 package 的 `package-info.java` 要標 `@InfrastructureRing`。
- 外部 API 的 payload / DTO **只留在 infrastructure**；adapter 對外回傳的是本 Context 的語言（Value Object / application result）。
- 測試放在**同一個 package**。Application Service 的測試只 mock 這個 adapter 的**介面（port）**，不碰 HTTP。

### 怎麼選

| 寫法 | 真的走 HTTP | Spring context | 適合 |
|---|---|---|---|
| **WireMock**（預設） | 是（本機 stub server） | 無 | 驗證 request（path、query、header、body）與 response mapping；404 / 429 / 5xx / timeout / 延遲等錯誤情境；與 HTTP client 套件無關 |
| **`@RestClientTest`** | 否 | 只載入 RestClient 相關 slice | 驗證 Boot 自動設定的 `RestClient.Builder`（序列化設定、客製化）與 bean 組裝是否正確 |
| 純 `MockRestServiceServer` | 否 | 無 | 最快；只驗證 mapping，不需要 Spring |

### WireMock（走真實 HTTP）

```java
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@WireMockTest(httpPort = 0)   // com.github.tomakehurst.wiremock.junit5.WireMockTest
class ExternalPriceClientWireMockTest {

    @Test
    void getPrice_mapsExternalPayloadToQuote(WireMockRuntimeInfo wm) {
        stubFor(get("/prices/abc").willReturn(okJson("""
                {"sku":"abc","amount":12.50,"currency":"USD"}
                """)));
        var client = new ExternalPriceClient(RestClient.builder(), wm.getHttpBaseUrl());

        var quote = client.getPrice("abc");

        assertThat(quote.amount()).isEqualByComparingTo("12.50");
        verify(getRequestedFor(urlEqualTo("/prices/abc")));
    }

    @Test
    void getPrice_when404_throwsPriceNotFound(WireMockRuntimeInfo wm) {
        stubFor(get("/prices/none").willReturn(aResponse().withStatus(404)));
        var client = new ExternalPriceClient(RestClient.builder(), wm.getHttpBaseUrl());

        assertThatThrownBy(() -> client.getPrice("none"))
                .isInstanceOf(ExternalPriceClient.PriceNotFoundException.class);
    }
}
```

`httpPort = 0` 讓 WireMock 挑隨機可用埠，避免埠衝突；base URL 從 `WireMockRuntimeInfo` 取得。

### `@RestClientTest`（Spring slice，不走網路）

```java
// org.springframework.boot.restclient.test.autoconfigure.RestClientTest
@RestClientTest(value = ExternalPriceClient.class, properties = "external.price.base-url=http://prices.test")
class ExternalPriceClientRestClientTest {

    @Autowired ExternalPriceClient client;
    @Autowired MockRestServiceServer server;   // org.springframework.test.web.client

    @Test
    void getPrice_mapsResponse() {
        server.expect(requestTo("http://prices.test/prices/abc"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"sku\":\"abc\",\"amount\":12.50,\"currency\":\"USD\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.getPrice("abc").currency()).isEqualTo("USD");
        server.verify();
    }
}
```

不用 Spring 時：`var builder = RestClient.builder(); var server = MockRestServiceServer.bindTo(builder).build();`，再把 `builder` 傳給 adapter。

### 規則

- WireMock 只出現在 **test code**，不進 production。
- 斷言 adapter 輸出的是**本 Context 的語言**，不是外部 payload 原樣外洩。
- 大型 stub JSON 集中成 fixture / helper，不要在每個測試複製。
- 版本：WireMock 目前穩定版是 3.x（4.0 仍是 beta）；`wiremock-standalone` **不受 Boot BOM 管理**，要自己指定版本，否則 Maven 會報 `dependencies.dependency.version ... is missing`。
- WireMock 不取代其他測試：Controller 仍用 `@WebMvcTest`、Repository 仍用 Testcontainers、Domain 仍用純單元測試。

---

## 架構測試：把規則變成測試

### `JMoleculesArchitectureTest`

| 測試 | 驗證 |
|---|---|
| `shouldFollowDddRules` | jMolecules 內建 DDD 規則（跨 Context 物件參照、`@Identity` 必要性） |
| `shouldFollowOnionArchitecture` | Onion Classical 依賴方向 |
| `repositoriesShouldOnlyManageAggregateRoots` | `@Repository` 只管理 `@AggregateRoot` |
| `valueObjectsShouldBeImmutable` | `@ValueObject` 沒有非 final 欄位 |
| `commandsShouldBeImmutable` | `@Command` 沒有非 final 欄位 |
| `queryModelsShouldNotTriggerCommands` | `@QueryModel` 不呼叫 `@CommandHandler` |
| `commandsShouldResideInApplicationServiceRing` | `@Command` 在標記 `@ApplicationServiceRing` 的 package |
| `commandHandlersShouldBeInApplicationLayer` | `@CommandHandler` 在標記 `@ApplicationServiceRing` 的 package |
| `dddBuildingBlocksShouldNotDependOnApiDtos` | domain / application 不依賴 API DTO 與 OpenAPI 產生的 model |

### `ModularityTest`

- `verifiesModularStructure`：驗證 Spring Modulith 模組邊界。
- `documentModules`：產生模組文件到 `target/spring-modulith-docs/`。

### `mvn test` 失敗是正常的

專案刻意保留 violation demo（見 [DDD Violation Demo](01-ddd-impl-violations.md)），所以**完整的 `mvn test` 預期有失敗**：

| 預期失敗的測試 | 原因 |
|---|---|
| `shouldFollowDddRules` | `BadOrder`、`MutablePrice` |
| `repositoriesShouldOnlyManageAggregateRoots` | `OrderItemRepository` |
| `valueObjectsShouldBeImmutable` | `MutablePrice` |
| `commandsShouldBeImmutable` | `BadCommand` |
| `queryModelsShouldNotTriggerCommands` | `BadQueryModel` |
| `commandsShouldResideInApplicationServiceRing` | `BadCommand` |
| `commandHandlersShouldBeInApplicationLayer` | `BadDomainHandler` |
| `ModularityTest#verifiesModularStructure` | `BadOrder` |

`shouldFollowOnionArchitecture`、`dddBuildingBlocksShouldNotDependOnApiDtos` 與 `documentModules` 應該通過。**判斷標準：失敗的測試不在上表內，才是真正的問題。** 新增的 class 造成表外的失敗，就是架構違規，需要修正。

---

## 執行指令

```bash
mvn spotless:apply                                   # 格式化（先做）
mvn test                                             # 全部（含架構、整合測試；需要 Docker；有預期失敗）
mvn test -Dtest=JMoleculesArchitectureTest           # 只跑 ArchUnit
mvn test -Dtest=ModularityTest                       # 只跑 Modulith
mvn test -Dtest='*RepositoryTest'                    # 只跑 Repository 整合測試
mvn test -Dtest='ProductControllerTest,CustomerControllerTest,OrderControllerTest'  # 只跑 API 測試
mvn test -Dtest='OrderTest,OrderServiceTest'         # 只跑指定單元測試
```

Spotless 的 `check` 在 `validate` phase 執行，格式錯誤會讓 build 在編譯前就中止，所以提交前一定先 `mvn spotless:apply`。

---

## 命名與位置規則

- 所有測試一律以 `Test` 結尾（包含整合測試）。**不要用 `IT` 結尾**：surefire 預設只執行 `*Test`，本專案沒有設定 failsafe，`*IT` 會永遠不被執行，問題也就沒人發現。
- 測試與被測類別放在**相同的 package**（Controller 測試尤其必要）。
- 檔案位置對照見 [`.claude/rules/testing.md`](../.claude/rules/testing.md)。

---

## 新增程式碼時要補什麼測試

| 新增 | 補的測試 |
|---|---|
| Aggregate / Entity / Value Object | Domain 單元測試（狀態、不變量、事件） |
| Domain Service | Domain Service 單元測試 |
| Command Handler / Application Service | Application Service 測試（mock Repository） |
| Event listener / Projection | 單元測試，含重複事件的冪等 |
| Controller | 同 package 的 `@WebMvcTest`（2xx、404、欄位驗證） |
| Repository 或持久化行為（含 `@Version`） | `*RepositoryTest`（Testcontainers） |
| 任何新 class | 跑 `mvn spotless:apply`，再跑 ArchUnit 測試 |
| Flamingock Change | `@Apply` / `@Rollback` 測試，見 [Flamingock 說明](02-tech-stack-flamingock.md) |
| 外部 HTTP 整合（第三方 API adapter） | WireMock contract 測試，見本章「第三方 API」（目前專案沒有外部 HTTP 依賴） |

---

## 已知陷阱

| 陷阱 | 說明 |
|---|---|
| `spring.data.mongodb.uri` 被靜默忽略 | Boot 4 改名為 `spring.mongodb.uri`，舊名稱不會報錯，只是退回預設的 `localhost:27017` |
| UUID 編碼沒有預設值 | Boot 4 需要 `spring.mongodb.representation.uuid`（本專案設 `java-legacy` 維持舊資料相容） |
| `@MockBean` 不存在 | Boot 4 改用 `@MockitoBean` |
| Lombok `@Setter` 在 bytecode 看不到 | `RetentionPolicy.SOURCE`，ArchUnit 改用「非 final 欄位」檢查涵蓋 |
| mock 掩蓋持久化問題 | 樂觀鎖、UUID 編碼、索引這類行為只有真實資料庫測得出來 |

---

## 相關文件

- [`.claude/rules/testing.md`](../.claude/rules/testing.md) — 測試規則與檔案位置（規則來源）
- [技術棧說明](02-tech-stack.md) — Testcontainers、WireMock、ByteBuddy
- [CQRS 實作說明](03-cqrs-impl.md) — Command / QueryModel 的測試範例
- [Onion 實作說明](04-onion-impl.md) — ArchUnit 對 ring 的驗證
- [DDD Violation Demo](01-ddd-impl-violations.md) — 刻意保留的錯誤與抓到它們的測試
- [架構 Review Checklist](06-architecture-review-checklist.md) — 開 PR 前的測試檢查
