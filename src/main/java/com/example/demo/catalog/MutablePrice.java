package com.example.demo.catalog;

import java.math.BigDecimal;
import lombok.Setter;
import org.jmolecules.ddd.annotation.ValueObject;

// 違規示範 #3：禁止在 ValueObject 加可變狀態
// 改用 Lombok @Setter — ArchUnit 分析 bytecode，仍然看得到生成的 setter
@ValueObject
@Setter
public class MutablePrice {

    // 違規：非 final、有 Lombok 生成的 setter
    private BigDecimal amount;
    private String currency;

    public MutablePrice(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
