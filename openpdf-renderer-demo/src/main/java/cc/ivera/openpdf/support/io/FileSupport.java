package cc.ivera.openpdf.support.io;

import cc.ivera.openpdf.support.exception.PdfRenderException;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件辅助类。
 */
public final class FileSupport {

    private FileSupport() {
    }

    public static void ensureParentDirectory(String filePath) {
        try {
            Path parent = Path.of(filePath).getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            throw new PdfRenderException("创建输出目录失败: " + filePath, e);
        }
    }
}
