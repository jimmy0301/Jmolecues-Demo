---
name: new-domain-event
description: 在指定的 Bounded Context 建立 DomainEvent、producer 方法、event listener，並透過 Aggregate registerEvent() 登記事件
arguments:
  - EventName
  - context
---

在指定的 Bounded Context 建立完整的 DomainEvent 登記與接收流程。

用法：/new-domain-event <EventName> <context>
範例：/new-domain-event OrderPlaced ordering

引數：$ARGUMENTS

## 目錄位置（Onion Architecture）

```
<context>/domain/       @DomainModelRing   → <EventName>.java（event record）
<context>/application/  @ApplicationServiceRing → <Context>Service.java、<EventName>Listener.java
```

## 步驟

### 1. `domain/<EventName>.java` — DomainEvent record

```java
@DomainEvent  // org.jmolecules.event.annotation.DomainEvent
public record <EventName>(<AggregateRoot>Id <aggregateRootId>, Instant occurredOn) {}
```

- 命名用**過去式**（OrderPlaced、CustomerRegistered）
- 欄位：相關 AggregateRoot 的 ID + `Instant occurredOn`
- 用 record 實作（不可變）
- 若事件會跨 Context 或跨系統消費，視為 Integration Event / public contract；欄位只用穩定型別，不包含 Aggregate、Entity、internal QueryModel 或 API DTO

### 2. 在 AggregateRoot 加 producer 方法

AggregateRoot 在 `domain/` 下，直接修改現有檔案：

```java
// 不加任何 annotation — producer 方法負責更新狀態並登記事件
public void someAction() {
    // 更新 Aggregate 內部狀態...
    this.status = SomeStatus.DONE;
    registerEvent(new <EventName>(this.id, Instant.now()));
}
```

- 方法命名用動詞（`place()`、`cancel()`、`ship()`）
- 在 Aggregate 內部保護 invariant，狀態改變後呼叫 `registerEvent()`
- 不直接注入 `ApplicationEventPublisher`，也不在 domain 層碰 Spring event publisher
- **不加 `@DomainEventHandler`**：該 annotation 是給消費（接收）event 的 handler，不是給 producer 用的

### 3. 在 application service 儲存 Aggregate

Application service 在 `application/` ring。若已有 `<Context>Service`，加入對應方法；若沒有，建立新檔案並同時建立 `application/package-info.java`：

```java
// application/package-info.java（若不存在）
@ApplicationServiceRing
package <base-package>.<context>.application;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
```

```java
// application/<Context>Service.java
@Service  // org.springframework.stereotype.Service
public class <Context>Service {

    private final <AggregateRoot>Repository repository;

    public <AggregateRoot> someAction(<AggregateRoot>Id id) {
        <AggregateRoot> aggregate = findAggregate(id);
        aggregate.someAction();       // event 已登記在 aggregate
        return repository.save(aggregate);
    }
}
```

Spring Data 會在 `save()` 時發布 Aggregate 已登記的 event。Application Service 不需要注入 `ApplicationEventPublisher`。
每個 command handler 原則上只修改一個 Aggregate；其他 Aggregate 或 Context 的後續動作由 event listener / Process Manager / Saga 協調。

### 4. 建立 `application/<EventName>Listener.java` — 接收 event

```java
// application/<EventName>Listener.java
@Component
class <EventName>Listener {

    private static final Logger log = LoggerFactory.getLogger(<EventName>Listener.class);

    @ApplicationModuleListener  // org.springframework.modulith.events.ApplicationModuleListener
    void on(<EventName> event) {
        log.info("<EventName>: id={}, at={}", event.id().id(), event.occurredOn());
    }
}
```

- `@ApplicationModuleListener` 是 Spring Modulith 的 async-capable event listener，語意等同 `@DomainEventHandler`（consumer）
- 若要跨 module 接收，將 listener 放到接收方 context 的 `application/` package 下
- listener 必須具備冪等性：同一 event 重送時，不可造成重複扣庫存、重複寄信、重複建立 projection 等 side effect
- 複雜跨步驟流程抽成 Process Manager / Saga，不塞在 listener 方法裡

完成後執行：
```bash
mvn spotless:apply && mvn test -Dtest=JMoleculesArchitectureTest
```
