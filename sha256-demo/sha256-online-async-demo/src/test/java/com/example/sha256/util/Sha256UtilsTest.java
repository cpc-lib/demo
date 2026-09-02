package com.example.sha256.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Sha256UtilsTest {

    @Test
    void shouldCalculateFileSha256() throws Exception {
        Path tempFile = Files.createTempFile("sha256-test-", ".txt");
        try {
            Files.writeString(tempFile, "hello", StandardCharsets.UTF_8);

            String sha256 = Sha256Utils.calculateFileSha256(tempFile, ignored -> {
            });

            assertEquals(
                    "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                    sha256
            );
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
