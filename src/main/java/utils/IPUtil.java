package utils;

import org.apache.commons.lang3.StringUtils;

import java.net.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class IPUtil {
    public static String getIpV4() {
        String ip = "";
        Enumeration<NetworkInterface> networkInterface;
        try {
            networkInterface = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
             return ip;
        }
        Set<String> ips = new HashSet<String> ();
        while (networkInterface.hasMoreElements()) {
            NetworkInterface ni = networkInterface.nextElement();

            Enumeration<InetAddress> inetAddress = null;
            try {
                if (null != ni) {
                    inetAddress = ni.getInetAddresses();
                }
            } catch (Exception e) {
             }
            while (null != inetAddress && inetAddress.hasMoreElements()) {
                InetAddress ia = inetAddress.nextElement();
                if (ia instanceof Inet6Address) {
                    continue; // ignore ipv6
                }
                String thisIp = ia.getHostAddress();
                // 排除 回送地址
                if (!ia.isLoopbackAddress() && !thisIp.contains(":") && !"127.0.0.1".equals(thisIp)) {
                    ips.add(thisIp);
                    if (StringUtils.isEmpty (ip)) {
                        ip = thisIp;
                    }
                }
            }
        }

        if (StringUtils.isEmpty (ip)) {
             ip = "";
        }
        return ip;
    }

}