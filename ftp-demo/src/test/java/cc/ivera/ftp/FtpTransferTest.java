package cc.ivera.ftp;

import cc.ivera.ftp.client.FtpClient;
import cc.ivera.ftp.server.FtpServerApp;
import org.apache.ftpserver.FtpServer;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.Assert.assertArrayEquals;

public class FtpTransferTest {

    @Test
    public void uploadsAndDownloadsAFile() throws Exception {
        Path tempDir = Files.createTempDirectory("ftp-demo-test");
        Path homeDir = tempDir.resolve("uploads");
        Path userFile = tempDir.resolve("ftp-users.properties");
        Path sourceFile = tempDir.resolve("source.txt");
        Path downloadFile = tempDir.resolve("nested/downloaded.txt");
        byte[] expected = "ftp-demo round trip".getBytes(StandardCharsets.UTF_8);
        Files.write(sourceFile, expected);
        int port = findAvailablePort();

        FtpServer server = null;
        try {
            server = FtpServerApp.startServer(port, "test", "123456", homeDir, userFile);

            try (FtpClient client = new FtpClient()) {
                client.connect("127.0.0.1", port, "test", "123456");
                client.upload(sourceFile.toString(), "remote.txt");
                client.download("remote.txt", downloadFile.toString());
            }

            assertArrayEquals(expected, Files.readAllBytes(homeDir.resolve("remote.txt")));
            assertArrayEquals(expected, Files.readAllBytes(downloadFile));
        } finally {
            if (server != null) {
                server.stop();
            }
            deleteRecursively(tempDir);
        }
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
