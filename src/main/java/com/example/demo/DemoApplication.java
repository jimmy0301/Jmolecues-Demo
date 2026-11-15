package com.example.demo;

import org.jmolecules.spring.mongodb.NotNewCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    /**
     * jMolecules ByteBuddy 會讓 AggregateRoot 實作 MutablePersistable（帶 isNew 旗標）。 這個 callback
     * 在載入／儲存後把旗標設為 false；未註冊時，載入的 Aggregate 會被當成新物件， save() 變成 insert，@Version
     * 樂觀鎖失效（DuplicateKeyException）。
     */
    @Bean
    NotNewCallback<?> notNewCallback() {
        return new NotNewCallback<>();
    }
}
