package cc.ivera.ftp.server;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;

import java.io.File;
import java.util.Collections;

public class FtpServerApp {

    public static void main(String[] args) throws Exception {
        int port = 2121;
        String username = "test";
        String password = "123456";
        String homeDir = "uploads";

        new File(homeDir).mkdirs();

        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(port);

        FtpServerFactory serverFactory = new FtpServerFactory();
        serverFactory.addListener("default", listenerFactory.createListener());

        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setPasswordEncryptor(new SaltedPasswordEncryptor());

        UserManager userManager = userManagerFactory.createUserManager();

        BaseUser user = new BaseUser();
        user.setName(username);
        user.setPassword(password);
        user.setHomeDirectory(new File(homeDir).getAbsolutePath());
        user.setAuthorities(Collections.singletonList(new WritePermission()));

        userManager.save(user);
        serverFactory.setUserManager(userManager);

        FtpServer server = serverFactory.createServer();
        server.start();

        System.out.println("FTP Server started on port " + port);
    }
}
