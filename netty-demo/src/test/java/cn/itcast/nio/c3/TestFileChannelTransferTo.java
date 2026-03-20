package cn.itcast.nio.c3;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class TestFileChannelTransferTo {
    public static void main(String[] args) throws IOException {
        //fileCopy0();
        //fileCopy1();
        fileCopy2();
    }


    private static void fileCopy0() {
        File sourceFile = new File("反贪风暴5.mkv");
        File destFile = new File("反贪风暴5-backup.mkv");

        try (
                // 创建文件输入流和缓冲输入流  
                FileInputStream fis = new FileInputStream(sourceFile);
                BufferedInputStream bis = new BufferedInputStream(fis);

                // 创建文件输出流和缓冲输出流  
                FileOutputStream fos = new FileOutputStream(destFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
        ) {
            byte[] buffer = new byte[1024]; // 创建一个长度为1024的字节数组作为缓冲区
            int bytesRead; // 用于存储每次读取的字节数  
            long start = System.currentTimeMillis();
            // 循环读取文件内容，并写入目标文件  
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            long end = System.currentTimeMillis();
            log.info("耗时: {} 秒", (end - start) / 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //FileChannel是阻塞的
    private static void fileCopy1() {
        try (
                FileChannel from = new FileInputStream("反贪风暴5.mkv").getChannel();
                FileChannel to = new FileOutputStream("反贪风暴5-backup.mkv").getChannel();
        ) {
            // 效率高，底层会利用操作系统的零拷贝进行优化, 2g 数据
            long size = from.size();
            long start = System.currentTimeMillis();
            // left 变量代表还剩余多少字节
            for (long left = size; left > 0; ) {
                System.out.println("position:" + (size - left) + " left:" + left);
                left -= from.transferTo((size - left), left, to);
            }
            long end = System.currentTimeMillis();
            log.info("耗时: {} 秒", (end - start) / 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void fileCopy2() throws IOException {
        long start = System.currentTimeMillis();
        String source = "D:\\develop\\code\\netty-demo\\反贪风暴5.mkv";
        String target = "D:\\develop\\code\\netty-demo\\反贪风暴5-backup.mkv";

        Files.walk(Paths.get(source)).forEach(path -> {
            try {
                String targetName = path.toString().replace(source, target);
                // 是目录
                if (Files.isDirectory(path)) {
                    Files.createDirectory(Paths.get(targetName));
                }
                // 是普通文件
                else if (Files.isRegularFile(path)) {
                    Files.copy(path, Paths.get(targetName));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        long end = System.currentTimeMillis();
        log.info("耗时: {} 秒", (end - start) / 1000);
    }


}
