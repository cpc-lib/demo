package cc.ivera.ftp.client;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FtpClient implements Closeable {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int DATA_TIMEOUT_MILLIS = 30_000;

    private final FTPClient ftp = new FTPClient();

    public FtpClient() {
        ftp.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        ftp.setDefaultTimeout(CONNECT_TIMEOUT_MILLIS);
        ftp.setDataTimeout(DATA_TIMEOUT_MILLIS);
    }

    public void connect(String host, int port, String user, String pass) throws IOException {
        ftp.connect(host, port);
        if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            disconnectQuietly();
            throw new IOException("FTP server refused connection to " + host + ":" + port + ": " + replyMessage());
        }
        if (!ftp.login(user, pass)) {
            disconnectQuietly();
            throw new IOException("FTP login failed for user '" + user + "': " + replyMessage());
        }
        if (!ftp.setFileType(FTP.BINARY_FILE_TYPE)) {
            throw new IOException("Failed to switch to binary transfer mode: " + replyMessage());
        }
        ftp.enterLocalPassiveMode();
    }

    public void upload(String localFile, String remoteFile) throws IOException {
        Path localPath = Paths.get(localFile);
        if (!Files.isRegularFile(localPath)) {
            throw new FileNotFoundException("Local file does not exist: " + localPath.toAbsolutePath());
        }
        try (InputStream in = Files.newInputStream(localPath)) {
            if (!ftp.storeFile(remoteFile, in)) {
                throw new IOException("FTP upload failed for '" + remoteFile + "': " + replyMessage());
            }
        }
    }

    public void download(String remoteFile, String localFile) throws IOException {
        Path localPath = Paths.get(localFile);
        Path parent = localPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(localPath)) {
            if (!ftp.retrieveFile(remoteFile, out)) {
                throw new IOException("FTP download failed for '" + remoteFile + "': " + replyMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        if (ftp.isConnected()) {
            try {
                ftp.logout();
            } catch (IOException e) {
                failure = e;
            }
            try {
                ftp.disconnect();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private String replyMessage() {
        String reply = ftp.getReplyString();
        return reply == null ? "no reply from server" : reply.trim();
    }

    private void disconnectQuietly() {
        if (!ftp.isConnected()) {
            return;
        }
        try {
            ftp.disconnect();
        } catch (IOException ignored) {
            // Preserve the original connection failure for callers.
        }
    }
}
