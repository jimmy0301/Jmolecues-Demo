---
description: jMolecules Onion Architecture Classical 分層 annotation 規則，編輯 package-info.java 時參照
paths:
  - src/main/java/**/package-info.java
---

# Onion Architecture Annotation 規則

套件：`org.jmolecules.architecture.onion.classical`

## 分層對應

每個 package 的 `package-info.java` 必須標記所屬的 Onion 環：

| Annotation | 套用層 | 放置的類型 |
|---|---|---|
| `@DomainModelRing` | 最內層 | AggregateRoot、Entity、ValueObject、Repository 介面、DomainEvent |
| `@DomainServiceRing` | 中層 | 跨 Aggregate 的無狀態領域邏輯（Domain Service） |
| `@ApplicationServiceRing` | 外層 | Command Handler、Application Service、Event Listener |
| `@InfrastructureRing` | 最外層 | Controller、DB adapter、外部整合 |

## 使用方式

```java
// <context>/domain/package-info.java
@DomainModelRing
package com.example.myapp.somecontext.domain;

import org.jmolecules.architecture.onion.classical.DomainModelRing;
```

```java
// <context>/domainservice/package-info.java
@DomainServiceRing
package com.example.myapp.somecontext.domainservice;

import org.jmolecules.architecture.onion.classical.DomainServiceRing;
```

```java
// <context>/application/package-info.java
@ApplicationServiceRing
package com.example.myapp.somecontext.application;

import org.jmolecules.architecture.onion.classical.ApplicationServiceRing;
```

```java
// <context>/infrastructure/web/package-info.java
@InfrastructureRing
package com.example.myapp.somecontext.infrastructure.web;

import org.jmolecules.architecture.onion.classical.InfrastructureRing;
```

## 依賴方向規則

依賴只能由外往內，內層不可依賴外層：

```
InfrastructureRing
  └── 可依賴 ApplicationServiceRing
        └── 可依賴 DomainServiceRing
              └── 可依賴 DomainModelRing（不依賴任何外層）
```

ArchUnit 測試 `shouldFollowOnionArchitecture` 自動驗證此規則，本專案目前通過。

## 新增 package 時

每個新建的 sub-package 都需要一個 `package-info.java` 並標記對應的 ring annotation，  
否則 ArchUnit 無法判斷該 package 屬於哪一層。