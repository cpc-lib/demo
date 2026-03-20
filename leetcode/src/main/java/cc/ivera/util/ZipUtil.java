package cc.ivera.util;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.util.StreamUtils.BUFFER_SIZE;

public class ZipUtil {

    /**
     * 传入文件file
     *
     * @param outputStream
     * @param fileList
     */
    public static void downloadZipForFiles(OutputStream outputStream, List<File> fileList) {
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(outputStream);
            for (File file : fileList) {
                ZipEntry zipEntry = new ZipEntry(file.getName());
                zipOutputStream.putNextEntry(zipEntry);
                byte[] buf = new byte[BUFFER_SIZE];
                int len;
                FileInputStream in = new FileInputStream(file);
                while ((len = in.read(buf)) != -1) {
                    zipOutputStream.write(buf, 0, len);
                    zipOutputStream.flush();
                }
            }
            zipOutputStream.flush();
            zipOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭流
            try {
                if (zipOutputStream != null) {
                    zipOutputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 传入文件的 byte[]
     * Map<String,byte[]> fileBufMap key是文件名（包含后缀），value是文件的byte[]
     *
     * @param outputStream
     * @param fileBufMap
     */
    public static void downloadZipForByteMore(OutputStream outputStream, Map<String, byte[]> fileBufMap) {
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new ZipOutputStream(outputStream);
            for (String fileName : fileBufMap.keySet()) {
                ZipEntry zipEntry = new ZipEntry(fileName);
                zipOutputStream.putNextEntry(zipEntry);
                if (Objects.nonNull(fileBufMap.get(fileName))) {
                    byte[] fileBytes = fileBufMap.get(fileName);
                    zipOutputStream.write(fileBytes);
                    zipOutputStream.flush();
                }
            }
            zipOutputStream.flush();
            zipOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭流
            try {
                if (zipOutputStream != null) {
                    zipOutputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 返回zip包的 byte[]
     *
     * @param fileBufMap
     * @return
     */
    public static byte[] getZipForByteMore(Map<String, byte[]> fileBufMap) {
        ByteArrayOutputStream totalZipBytes = null;
        ZipOutputStream zipOutputStream = null;
        try {
            totalZipBytes = new ByteArrayOutputStream();
            zipOutputStream = new ZipOutputStream(totalZipBytes);
            for (String fileName : fileBufMap.keySet()) {
                ZipEntry zipEntry = new ZipEntry(fileName);
                zipOutputStream.putNextEntry(zipEntry);
                if (Objects.nonNull(fileBufMap.get(fileName))) {
                    byte[] fileBytes = fileBufMap.get(fileName);
                    zipOutputStream.write(fileBytes);
                    zipOutputStream.flush();
                }
            }
            zipOutputStream.close();
            byte[] bytes = totalZipBytes.toByteArray();
            totalZipBytes.close();// 关闭流
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭流
            try {
                if (totalZipBytes != null) {
                    totalZipBytes.close();
                }
                if (zipOutputStream != null) {
                    zipOutputStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}