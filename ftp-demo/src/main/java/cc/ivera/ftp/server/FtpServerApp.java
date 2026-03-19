package cc.ivera.ftp.server;

import cc.ivera.ftp.util.ConfigSupport;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class FtpServerApp {

    private static final Logger log = LoggerFactory.getLogger(FtpServerApp.class);

    public static void main(String[] args) throws Exception {
        int port = ConfigSupport.getInt("ftp.server.port", "FTP_SERVER_PORT", 2121);
        String username = ConfigSupport.getString("ftp.server.username", "FTP_SERVER_USERNAME", "test");
        String password = ConfigSupport.getString("ftp.server.password", "FTP_SERVER_PASSWORD", "123456");
        Path homeDir = Paths.get(ConfigSupport.getString("ftp.server.homeDir", "FTP_SERVER_HOME_DIR", "uploads"));
        Path userFile = Paths.get(ConfigSupport.getString("ftp.server.userFile", "FTP_SERVER_USER_FILE", "target/ftp-users.properties"));

        FtpServer server = startServer(port, username, password, homeDir, userFile);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopQuietly(server), "ftp-server-shutdown"));

        log.info("FTP Server started on port {} for user '{}' with home '{}'", port, username, homeDir.toAbsolutePath().normalize());
    }

    public static FtpServer startServer(int port, String username, String password, Path homeDir, Path userFile) throws Exception {
        Path normalizedHomeDir = homeDir.toAbsolutePath().normalize();
        Path normalizedUserFile = userFile.toAbsolutePath().normalize();

        Files.createDirectories(normalizedHomeDir);
        if (normalizedUserFile.getParent() != null) {
            Files.createDirectories(normalizedUserFile.getParent());
        }
        if (Files.notExists(normalizedUserFile)) {
            Files.createFile(normalizedUserFile);
        }

        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(port);

        FtpServerFactory serverFactory = new FtpServerFactory();
        serverFactory.addListener("default", listenerFactory.createListener());

        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setPasswordEncryptor(new SaltedPasswordEncryptor());
        userManagerFactory.setFile(normalizedUserFile.toFile());

        UserManager userManager = userManagerFactory.createUserManager();

        BaseUser user = new BaseUser();
        user.setName(username);
        user.setPassword(password);
        user.setHomeDirectory(normalizedHomeDir.toString());
        user.setAuthorities(Collections.singletonList(new WritePermission()));

        userManager.save(user);
        serverFactory.setUserManager(userManager);

        FtpServer server = serverFactory.createServer();
        server.start();
        return server;
    }

    private static void stopQuietly(FtpServer server) {
        try {
            server.stop();
        } catch (RuntimeException e) {
            log.warn("Failed to stop FTP server cleanly", e);
        }
    }
}
