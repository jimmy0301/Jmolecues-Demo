# Chapter 1：Domain-Driven Design 核心概念

## 什麼是 DDD？

DDD 是一套把「業務邏輯」放在程式設計核心的方法論，核心思想是：

> **讓程式碼的語言與業務的語言一致。**

傳統做法容易讓 `Order` 退化成只有 getter/setter 的資料袋，業務邏輯散落在各處的 Service。  
DDD 的做法是讓 `Order` 真正知道自己可以 `place()`（下單）、`cancel()`（取消），  
並在狀態改變時發布 `OrderPlaced` 事件，讓程式碼本身就能說明業務意圖。

---

## Building Blocks

DDD 定義了幾種戰術模式（Tactical Patterns），用來組織領域物件：

| 概念 | 說明 |
|---|---|
| **Aggregate Root** | 一群物件的統一入口，外部只能透過它操作內部；維護一致性邊界 |
| **Entity** | 有唯一 ID、有生命週期的物件，隸屬於某個 Aggregate |
| **Value Object** | 沒有 ID、以值定義相等、必須不可變 |
| **Domain Event** | Aggregate 狀態改變時發出的通知；過去式命名，代表已發生的事實 |
| **Repository** | 只為 Aggregate Root 提供的持久化抽象，封裝儲存細節 |
| **Domain Service** | 跨多個 Aggregate 的無狀態邏輯，不屬於任一 Aggregate |
| **Bounded Context** | 一個業務子域的邊界，有自己的 Ubiquitous Language |
| **Shared Kernel** | 多個 Context 協議共用的一小塊領域模型，修改需各方同意 |

---

## 架構模式

### Bounded Context — 業務邊界

每個 Bounded Context 是一個獨立的業務子域，有自己的模型與語言。  
Context 之間只能透過 ID 或 Domain Event 溝通，不允許直接持有對方的物件。

**跨 Context 持有物件的後果：**  
若 `ordering` 直接持有 `Customer` 物件，當 `customer` Context 的結構改變，`ordering` 也必須同步修改——兩個原本獨立的業務域就綁死了。

### Shared Kernel — 協議共用的型別

Shared Kernel 是 Bounded Context 規則的例外：  
當多個 Context 都需要同一個概念（如金額 `Money`），將它放在共享的小型 package，各 Context 都可以依賴。

**限制：**
- 修改 Shared Kernel 型別，**所有依賴它的 Context 都要同步調整**
- 因此應盡量小、盡量穩定
- 不要把「只有一個 Context 用到」的型別放進來

### Onion Architecture — 洋蔥分層

每個 Bounded Context 內部以環狀分層，依賴方向**只能由外往內**：

```
┌─────────────────────────────────────────┐
│  ApplicationService（最外層）            │ ← 協調者：連接外部請求與領域邏輯
│  ┌───────────────────────────────────┐  │
│  │  DomainService                    │  │ ← 跨 Aggregate 的領域邏輯
│  │  ┌─────────────────────────────┐  │  │
│  │  │  DomainModel（最內層）       │  │  │ ← 純業務邏輯，不依賴框架
│  │  └─────────────────────────────┘  │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

- **DomainModel**：Aggregate、Entity、Value Object、Domain Event、Repository 介面
- **DomainService**：跨 Aggregate 的計算邏輯（例如定價），不知道外層存在
- **ApplicationService**：協調 Repository、發布 Event，是領域邏輯對外的入口

---

→ 本專案的實作對應見 [DDD 實作說明](01-ddd-impl.md)
