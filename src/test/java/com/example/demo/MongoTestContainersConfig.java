package com.example.demo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;

@TestConfiguration(proxyBeanMethods = false)
class MongoTestContainersConfig {

    @Bean
    @ServiceConnection // 自動將容器連線資訊注入 Spring Data MongoDB，不需手動設定 spring.mongodb.*
    MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer("mongo:7");
    }
}
