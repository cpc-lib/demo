package com.example.sha256;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha256Utils {

    private static final int BUFFER_SIZE = 8192;

    private Sha256Utils() {
    }

    public static String calculateFileSha256(Path path)
            throws IOException, NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream inputStream = Files.newInputStream(path)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }
        }

        byte[] hashBytes = digest.digest();

        return bytesToHex(hashBytes);
    }

    private static String bytesToHex(byte[] bytes) {

        StringBuilder result = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            result.append(String.format("%02x", b & 0xff));
        }

        return result.toString();
    }
}