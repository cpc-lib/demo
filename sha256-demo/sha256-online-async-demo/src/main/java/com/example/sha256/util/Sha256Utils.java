package com.example.sha256.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.LongConsumer;

public final class Sha256Utils {

    private static final int BUFFER_SIZE = 1024 * 1024;

    private Sha256Utils() {
    }

    public static String calculateFileSha256(Path path, LongConsumer progressConsumer)
            throws IOException, NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[BUFFER_SIZE];
        long processed = 0L;

        try (InputStream inputStream = Files.newInputStream(path)) {
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
                processed += length;
                progressConsumer.accept(processed);
            }
        }

        return toHex(digest.digest());
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }
}
