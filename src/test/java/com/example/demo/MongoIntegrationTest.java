package com.example.demo;

import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;

/** Repository 整合測試基底類別：只載入 MongoDB slice，並以 Testcontainers 提供真實 MongoDB（需 Docker）。 */
@DataMongoTest
@Import(MongoTestContainersConfig.class)
public abstract class MongoIntegrationTest {}
