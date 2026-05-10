# Product Requirements Document

## 1. 背景與目標

本專案是一個電商領域的 DDD（Domain-Driven Design）實作 demo，用於展示如何在 Java / Spring Boot 生態系中落地 DDD 戰術設計（Tactical Design），並透過 jMolecules 將架構意圖直接表達在程式碼中。

**核心目標：**
- 示範各 DDD Building Block 的正確用法與邊界
- 透過 ArchUnit 自動驗證架構規則，讓違規在 CI 階段就被阻擋
- 展示 Onion Architecture 的 package 分層與 Spring Modulith 的模組邊界管理

---

## 2. 使用者與情境

| 角色 | 情境 |
|---|---|
| 後端開發者 | 學習如何在 Spring Boot 專案中套用 DDD 戰術設計 |
| 架構師 | 評估 jMolecules + Spring Modulith 的可行性與效益 |
| 技術 Lead | 建立可供團隊參考的 DDD Codebase 範本 |

---

## 3. 功能範圍

### 3.1 Bounded Context

| Context | 職責 | 核心 Aggregate |
|---|---|---|
| **catalog** | 商品目錄管理，維護商品資訊與定價 | `Product` |
| **customer** | 顧客管理，維護顧客資料與地址 | `Customer` |
| **ordering** | 訂單生命週期管理（建立、下單、取消） | `Order` |

### 3.2 核心流程

```
顧客建立訂單
  → Order 建立（PENDING）
  → 加入 OrderItem（含 ProductId、Quantity、Money）
  → 下單（PENDING → PLACED），發布 OrderPlaced event
  → （可選）取消（→ CANCELLED），發布 OrderCancelled event
```

### 3.3 Demo 展示項目

| 項目 | 說明 |
|---|---|
| DDD Annotation 用法 | @AggregateRoot / @Entity / @ValueObject / @Repository / @DomainEvent |
| Onion Architecture | Classical 四環分層：DomainModel → DomainService → ApplicationService → Infrastructure |
| 跨 Context 邊界 | 只允許 ID 參照，違規由 ArchUnit 偵測 |
| 違規示範（3 個） | BadOrder（#1）、OrderItemRepository（#2）、MutablePrice（#3） |
| 事件驅動 | ApplicationEventPublisher + @ApplicationModuleListener |

---

## 4. 非功能需求

| 類別 | 要求 |
|---|---|
| 架構驗證 | 每次 CI 自動執行 ArchUnit 與 Spring Modulith 測試 |
| 程式碼格式 | Spotless + GJF AOSP，validate phase 強制檢查 |
| JDK 相容性 | JDK 17（開發環境） |
| 可擴展性 | 新增 Bounded Context 只需依照 `/add-bounded-context` skill 操作 |

---

## 5. 不在範圍內（Out of Scope）

- 生產環境部署設定
- 完整 REST API（目前無 Controller）
- 認證授權
- 資料庫 Migration 腳本
