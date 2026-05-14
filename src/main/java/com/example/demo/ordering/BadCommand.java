package com.example.demo.ordering;

import com.example.demo.shared.CustomerId;
import org.jmolecules.architecture.cqrs.Command;

/**
 * ⚠️ Violation Demo #4：@Command 持有可變狀態。
 *
 * <p>Command 應為不可變（record 或 final fields），代表某一時刻的操作意圖。可變 Command 可能被修改後重送，造成不一致。
 */
@Command
public class BadCommand {

    public CustomerId customerId; // non-final, public — 可被外部修改
    public String note;

    public BadCommand(CustomerId customerId, String note) {
        this.customerId = customerId;
        this.note = note;
    }
}
