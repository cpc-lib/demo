package com.example.sha256.worker;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Sha256AlgorithmTest {
    @Test
    void shouldCalculateKnownSha256() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String actual = HexFormat.of().formatHex(digest.digest("hello".getBytes()));
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", actual);
    }
}
