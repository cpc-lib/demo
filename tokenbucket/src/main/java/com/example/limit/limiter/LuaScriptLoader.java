package com.example.limit.limiter;

import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LuaScriptLoader {
    public static String load(String path) {
        try {
            ClassPathResource r = new ClassPathResource(path);
            InputStream is = r.getInputStream();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Lua加载失败:" + path, e);
        }
    }
}
