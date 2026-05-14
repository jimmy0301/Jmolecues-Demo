/**
 * Shared Kernel — 跨 Bounded Context 共用的 Value Object。
 *
 * <p>此 package 收錄在多個 Context 之間流通的型別：
 *
 * <ul>
 *   <li>{@link com.example.demo.shared.Money} — 金額（catalog 定價、ordering 訂單項目）
 *   <li>{@link com.example.demo.shared.ProductId} — 商品識別碼（catalog AggregateRoot ID、ordering 跨
 *       Context ID 參照）
 *   <li>{@link com.example.demo.shared.CustomerId} — 顧客識別碼（customer AggregateRoot ID、ordering 跨
 *       Context ID 參照）
 * </ul>
 *
 * <p>Shared Kernel 的修改需要所有依賴方協調，異動頻率應盡量低。
 */
package com.example.demo.shared;
