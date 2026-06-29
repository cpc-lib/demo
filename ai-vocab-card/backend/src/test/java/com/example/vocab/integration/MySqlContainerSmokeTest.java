package com.example.vocab.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class MySqlContainerSmokeTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("ai_vocab")
            .withUsername("root")
            .withPassword("root");

    @Test
    void mysqlContainerShouldStart() {
        assertTrue(mysql.isRunning());
    }
}
