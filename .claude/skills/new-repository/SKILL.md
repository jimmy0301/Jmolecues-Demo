---
name: new-repository
description: 為指定的 AggregateRoot 建立 Repository，遵循 DDD Repository 三個特點與技術框架分離
arguments:
  - context
  - AggregateName
---

為指定的 AggregateRoot 建立 Repository。

用法：/new-repository <context> <AggregateName>
範例：/new-repository ordering Order

引數：$ARGUMENTS

## DDD Repository 的三個特點

1. **嚴格限制只能從 AggregateRoot 取得或修改領域物件** — 確保 Aggregate 不變規則
2. **隱藏持久化技術細節** — 提供客戶端技術無關的 façade 介面
3. **定義領域模型與資料模型的邊界**

## 方式 A — 框架耦合（快速，適合早期或簡單專案）

Repository interface 直接繼承 Spring Data，省去寫 infrastructure 實作類別。

```java
// <context>/<AggregateName>Repository.java
@Repository  // org.jmolecules.ddd.annotation.Repository
public interface <AggregateName>Repository
        extends CrudRepository<<AggregateName>, <AggregateName>Id> {

    List<<AggregateName>> findByCustomerId(CustomerId customerId);
}
```

**缺點：** Domain interface 依賴框架，更換持久化機制需修改 domain 層。

---

## 方式 B — 框架抽離，支援多種 Database（推薦）

Domain interface 是純 Java，不依賴任何框架。
每種資料庫各自在 infrastructure 層提供實作，透過 Spring Profile 切換。

### 目錄結構

```
<base-package>.<context>/                                       ← domain 層
├── <AggregateName>.java
├── <AggregateName>Id.java
└── <AggregateName>Repository.java                             ← 純 Java interface，無框架依賴

<base-package>.infrastructure.repository.mongo/               ← MongoDB infrastructure（外部不可見）
├── Mongo<AggregateName>Repository.java                        ← implements domain interface，@Profile("mongodb")
└── SpringData<AggregateName>Repository.java                   ← package-private，extends MongoRepository

<base-package>.infrastructure.repository.jpa/                 ← JPA infrastructure（外部不可見）
├── Jpa<AggregateName>Repository.java                          ← implements domain interface，@Profile("jpa")
└── SpringDataJpa<AggregateName>Repository.java                ← package-private，extends JpaRepository

<base-package>.infrastructure.repository.cache/               ← 快取 Decorator
└── Cache<AggregateName>Repository.java                        ← @Primary，包裝任一實作
```

### Step 1 — Domain Interface（純 Java，無框架）

只放業務需要的行為，**不繼承任何框架類別**。
`save()` 回傳 `void` — domain 只在乎儲存的動作，不在乎框架回傳的實體。

```java
// <context>/<AggregateName>Repository.java
@Repository  // org.jmolecules.ddd.annotation.Repository
public interface <AggregateName>Repository {

    void save(<AggregateName> aggregate);           // void，非框架的 return type
    Optional<<AggregateName>> findById(<AggregateName>Id id);
    void delete(<AggregateName> aggregate);

    // 依業務需求加入，用 Domain 語言命名
    List<<AggregateName>> findByCustomerId(CustomerId customerId);
}
```

**禁止在 domain interface 繼承任何框架：**
```java
// ❌ domain interface 不能這樣寫
public interface OrderRepository extends CrudRepository<Order, OrderId> { ... }
public interface OrderRepository extends MongoRepository<Order, OrderId> { ... }

// ✅ 正確：純 Java interface，無任何框架 import
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
}
```

### Step 2a — MongoDB 實作

```java
// infrastructure.repository.mongo/SpringData<AggregateName>Repository.java
// package-private（不加 public）— 外部不可見，只供同 package 的實作類別使用
interface SpringData<AggregateName>Repository
        extends MongoRepository<<AggregateName>, <AggregateName>Id> {
    // 享受 Spring Data Query Method 功能，但不對外暴露
    List<<AggregateName>> findByCustomerId(CustomerId customerId);
}

// infrastructure.repository.mongo/Mongo<AggregateName>RepositoryImpl.java
// 命名慣例：Mongo + AggregateName + RepositoryImpl
@Repository          // org.springframework.stereotype.Repository（Spring 的，非 jMolecules）
@Profile("mongodb")
@RequiredArgsConstructor  // Lombok，自動產生 constructor injection
class Mongo<AggregateName>RepositoryImpl implements <AggregateName>Repository {

    private final SpringData<AggregateName>Repository springDataRepo;  // 處理主表（Aggregate）
    private final MongoTemplate mongoTemplate;                          // 處理副表或複雜查詢

    @Override
    public void save(<AggregateName> aggregate) {
        // 若 <AggregateName> 有內部 Entity 存在獨立 collection（如 OrderItem）：
        // 1. 先用 mongoTemplate 存 Entity 副表
        //    aggregate.getItems().forEach(item -> mongoTemplate.save(item, "order_items"));
        // 2. 再存 AggregateRoot 主表
        springDataRepo.save(aggregate);
    }

    @Override
    public Optional<<AggregateName>> findById(<AggregateName>Id id) {
        return springDataRepo.findById(id);
    }

    @Override
    public void delete(<AggregateName> aggregate) {
        springDataRepo.delete(aggregate);
    }

    @Override
    public List<<AggregateName>> findByCustomerId(CustomerId customerId) {
        return springDataRepo.findByCustomerId(customerId);
    }
}
```

### Step 2b — JPA 實作（關聯式資料庫）

```java
// infrastructure.repository.jpa/SpringDataJpa<AggregateName>Repository.java
// package-private — 外部不可見
interface SpringDataJpa<AggregateName>Repository
        extends JpaRepository<<AggregateName>, <AggregateName>Id> {
    List<<AggregateName>> findByCustomerId(CustomerId customerId);
}

// infrastructure.repository.jpa/Jpa<AggregateName>Repository.java
@Profile("jpa")
@Component
class Jpa<AggregateName>RepositoryImpl implements <AggregateName>Repository {

    private final SpringDataJpa<AggregateName>Repository springData;

    // 同 MongoDB 實作，委派給 springData（save 同樣回傳 void）
    @Override public void save(<AggregateName> aggregate) { springData.save(aggregate); }

    @Override public Optional<<AggregateName>> findById(<AggregateName>Id id) {
        return springData.findById(id);
    }
    // ...其他方法
}
```

### Step 2c — 快取 Decorator（包裝任一實作）

```java
// infrastructure/<context>/cache/Cache<AggregateName>Repository.java
@Profile("cache")
@Primary   // 優先注入，快取 miss 再 delegate 給底層
@Component
class Cache<AggregateName>Repository implements <AggregateName>Repository {

    private final <AggregateName>Repository delegate;  // 注入 MongoDB 或 JPA 實作
    private final Cache cache;

    Cache<AggregateName>Repository(
            @Qualifier("mongo<AggregateName>Repository") <AggregateName>Repository delegate,
            Cache cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override public Optional<<AggregateName>> findById(<AggregateName>Id id) {
        return cache.get(id).or(() -> {
            var result = delegate.findById(id);
            result.ifPresent(a -> cache.put(id, a));
            return result;
        });
    }

    @Override public void save(<AggregateName> aggregate) {
        cache.evict(aggregate.getId());
        delegate.save(aggregate);
    }
    // ...其他方法
}
```

### Step 3 — 透過 Spring Profile 切換實作

```yaml
# application.yml
spring:
  profiles:
    active: mongodb   # 切換成 jpa 或 cache 不影響 domain / application service
```

| Profile | 使用實作 | 適用場景 |
|---|---|---|
| `mongodb` | `MongoOrderRepository` | 預設，無明確 schema 的資料 |
| `jpa` | `JpaOrderRepository` | 需要強 schema、ACID 交易 |
| `cache` | `CacheOrderRepository` + delegate | 高讀取頻率，效能優化 |

## 命名規範

| 層 | 命名 | 範例 |
|---|---|---|
| Domain interface | `<AggregateName>Repository` | `OrderRepository` |
| MongoDB 實作 | `Mongo<AggregateName>Repository` | `MongoOrderRepository` |
| JPA 實作 | `Jpa<AggregateName>Repository` | `JpaOrderRepository` |
| ES 實作 | `Es<AggregateName>Repository` | `EsOrderRepository` |
| 快取 Decorator | `Cache<AggregateName>Repository` | `CacheOrderRepository` |
| Spring Data 內部 | `SpringDataMongo/Jpa/Es<AggregateName>Repository` | （package-private） |

## 方法命名原則

用 **Domain 語言**，不暴露技術細節：

```
✅ findByCustomerId(CustomerId id)
✅ findPendingOrders()
✅ findByStatus(OrderStatus status)

❌ selectByCustomerIdFromOrdersTable(UUID id)
❌ queryWithMongoFilter(Bson filter)
```

## 禁止事項

- 禁止為非 `@AggregateRoot` 的 Entity 建立 Repository（違反特點 1）
- 禁止在方法名稱中暴露技術細節（違反特點 2）
- Domain interface 禁止 import 任何框架（Spring Data、MongoDB、JPA）

完成後執行：
```bash
mvn spotless:apply && mvn test -Dtest=JMoleculesArchitectureTest
```
