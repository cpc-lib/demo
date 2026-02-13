package cc.ivera.ftp.client;

public class FtpClientDemo {

    public static void main(String[] args) throws Exception {
        try (FtpClient client = new FtpClient()) {
            client.connect("127.0.0.1", 2121, "test", "123456");
            client.upload("test.txt", "/test.txt");
            client.download("/test.txt", "download/test.txt");
        }
    }
}
