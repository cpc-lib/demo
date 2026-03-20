package cc.ivera.test.base;

import cc.ivera.util.MacAddrUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Demo19 {

    public static void main(String[] args) throws UnknownHostException, UnknownHostException {
        InetAddress inetAddress = InetAddress.getLocalHost();
        String localMacAddress1 = MacAddrUtil.getLocalMac(inetAddress);
        System.out.println("localMacAddress1 = " + localMacAddress1);
        String localMacAddress2 = MacAddrUtil.getMacAddress(inetAddress);
        System.out.println("localMacAddress2 = " + localMacAddress2);
    }
}
