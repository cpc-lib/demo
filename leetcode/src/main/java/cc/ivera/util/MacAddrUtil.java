package cc.ivera.util;

import cn.hutool.core.net.NetUtil;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class MacAddrUtil {

    /**
     * @param inetAddress
     * @return 本地mac地址
     */
    public static String getLocalMac(InetAddress inetAddress) {
        try {
            //获取网卡，获取地址
            byte[] mac = NetworkInterface.getByInetAddress(inetAddress).getHardwareAddress();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mac.length; i++) {
                if (i != 0) {
                    sb.append("-");
                }
                //字节转换为整数
                int temp = mac[i] & 0xff;
                String str = Integer.toHexString(temp);
                if (str.length() == 1) {
                    sb.append("0" + str);
                } else {
                    sb.append(str);
                }
            }
            return sb.toString();
        } catch (Exception exception) {
        }
        return null;
    }

    public static String getMacAddress(InetAddress inetAddress) {
        return NetUtil.getMacAddress(inetAddress, "-");
    }

}
