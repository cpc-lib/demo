package com.example.sha256;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Sha256Application {

    public static void main(String[] args) {

        // 直接修改这里的文件路径即可
        Path path = Paths.get("D:\\temp\\2026破亿神曲.mp4");

        try {
            if (!Files.exists(path)) {
                System.err.println("文件不存在: " + path);
                return;
            }

            if (!Files.isRegularFile(path)) {
                System.err.println("不是普通文件: " + path);
                return;
            }

            System.out.println("开始计算 SHA-256...");
            System.out.println("文件路径: " + path.toAbsolutePath());
            System.out.println("文件大小: " + Files.size(path) + " bytes");

            String sha256 = Sha256Utils.calculateFileSha256(path);

            System.out.println();
            System.out.println("计算完成！");
            System.out.println("SHA-256:");
            System.out.println(sha256);

        } catch (Exception e) {
            System.err.println("SHA-256 计算失败:");
            e.printStackTrace();
        }
    }
}