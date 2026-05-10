---
name: new-domain-event
description: 在指定的 Bounded Context 建立 DomainEvent、handler 方法、event listener，並透過 ApplicationEventPublisher 發布
arguments:
  - EventName
  - context
---

在指定的 Bounded Context 建立完整的 DomainEvent 發布與接收流程。

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
public record <EventName>(<AggregateRoot>Id id, Instant occurredOn) {}
```

- 命名用**過去式**（OrderPlaced、CustomerRegistered）
- 欄位：相關 AggregateRoot 的 ID + `Instant occurredOn`
- 用 record 實作（不可變）

### 2. 在 AggregateRoot 加 `@DomainEventHandler` 方法

AggregateRoot 在 `domain/` 下，直接修改現有檔案：

```java
@DomainEventHandler  // org.jmolecules.event.annotation.DomainEventHandler
public <EventName> someAction() {
    // 更新 Aggregate 內部狀態...
    this.status = SomeStatus.DONE;
    return new <EventName>(this.id, Instant.now());
}
```

- 方法命名用動詞（`place()`、`cancel()`、`ship()`）
- 只負責**回傳** event，不自己發布（發布由 application service 負責）

### 3. 在 application service 用 `ApplicationEventPublisher` 發布

Application service 在 `application/` ring。若已有 `<Context>Service`，加入對應方法；若沒有，建立新檔案並同時建立 `application/package-info.java`：

```java
// application/package-info.java（若不存在）
@ApplicationServiceRing
package com.example.demo.<context>.application;
import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
```

```java
// application/<Context>Service.java
@Service  // org.springframework.stereotype.Service
public class <Context>Service {

    private final <AggregateRoot>Repository repository;
    private final ApplicationEventPublisher events;  // org.springframework.context.ApplicationEventPublisher

    public <AggregateRoot> someAction(<AggregateRoot>Id id) {
        <AggregateRoot> aggregate = findAggregate(id);
        events.publishEvent(aggregate.someAction());  // 發布 @DomainEventHandler 回傳的 event
        return repository.save(aggregate);
    }
}
```

> ⚠️ `ApplicationEventPublisher` 需要 `spring-modulith-events-api` 依賴（pom.xml 已加）。

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

- `@ApplicationModuleListener` 是 Spring Modulith 的 async-capable event listener
- 若要跨 module 接收，將 listener 放到接收方 context 的 `application/` package 下

完成後執行：
```bash
mvn spotless:apply && mvn test -Dtest=JMoleculesArchitectureTest
```
