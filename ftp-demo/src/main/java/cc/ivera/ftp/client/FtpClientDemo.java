package cc.ivera.ftp.client;

import cc.ivera.ftp.util.ConfigSupport;

public class FtpClientDemo {

    public static void main(String[] args) throws Exception {
        String host = ConfigSupport.getString("ftp.client.host", "FTP_CLIENT_HOST", "127.0.0.1");
        int port = ConfigSupport.getInt("ftp.client.port", "FTP_CLIENT_PORT", 2121);
        String username = ConfigSupport.getString("ftp.client.username", "FTP_CLIENT_USERNAME", "test");
        String password = ConfigSupport.getString("ftp.client.password", "FTP_CLIENT_PASSWORD", "123456");
        String localUploadFile = ConfigSupport.getString("ftp.client.localUploadFile", "FTP_CLIENT_LOCAL_UPLOAD_FILE", "test.txt");
        String remoteFile = ConfigSupport.getString("ftp.client.remoteFile", "FTP_CLIENT_REMOTE_FILE", "test.txt");
        String localDownloadFile = ConfigSupport.getString("ftp.client.localDownloadFile", "FTP_CLIENT_LOCAL_DOWNLOAD_FILE", "download/temp.txt");

        try (FtpClient client = new FtpClient()) {
            client.connect(host, port, username, password);
            client.upload(localUploadFile, remoteFile);
            client.download(remoteFile, localDownloadFile);
        }
    }
}
