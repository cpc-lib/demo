package cc.ivera.ftp.client;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;

public class FtpClient implements Closeable {

    private final FTPClient ftp = new FTPClient();

    public void connect(String host, int port, String user, String pass) throws IOException {
        ftp.connect(host, port);
        ftp.login(user, pass);
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        ftp.enterLocalPassiveMode();
    }

    public void upload(String localFile, String remoteFile) throws IOException {
        try (InputStream in = new FileInputStream(localFile)) {
            ftp.storeFile(remoteFile, in);
        }
    }

    public void download(String remoteFile, String localFile) throws IOException {
        try (OutputStream out = new FileOutputStream(localFile)) {
            ftp.retrieveFile(remoteFile, out);
        }
    }

    @Override
    public void close() throws IOException {
        ftp.logout();
        ftp.disconnect();
    }
}
