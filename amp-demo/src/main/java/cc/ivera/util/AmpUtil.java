package cc.ivera.util;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class AmpUtil {

    //在高德申请的应用Key
    private static final String key = "659f057c4e1b3c72ae58027363b3afb9";

    //申请的账户Key

    /**
     * 0.根据地址名称得到两个地址间的距离
     *
     * @param start 起始位置
     * @param start 结束位置
     * @return 两个地址间的距离
     */
    public static long getDistanceByAddress(String start, String end) {
        String startLonLat = getLonLat(start);
        String endLonLat = getLonLat(end);
        long dis = getDistance(startLonLat, endLonLat);
        return dis;
    }

    /**
     * 1.地址转换为经纬度
     *
     * @param address 地址
     * @return 经纬度
     */
    public static String getLonLat(String address) {
        // 返回输入地址address的经纬度信息, 格式是 经度,纬度
        String queryUrl = "http://restapi.amap.com/v3/geocode/geo?key=" + key + "&address=" + address;
        String queryResult = getResponse(queryUrl); // 高德接品返回的是JSON格式的字符串
        JSONObject job = JSONObject.parseObject(queryResult);
        JSONObject jobJSON = JSONObject
                .parseObject(job.get("geocodes").toString().substring(1, job.get("geocodes").toString().length() - 1));
        String DZ = jobJSON.get("location").toString();
//		System.out.println("经纬度：" + DZ);
        return DZ;
    }

    /**
     * 将经纬度getLng， getLat 通过getAMapByLngAndLat方法转换地址
     *
     * @param getLng 经度
     * @param getLat 纬度
     * @return 地址名称
     * @throws Exception
     */
    public static String getAMapByLngAndLat(String getLng, String getLat) throws Exception {
        String url;
        //https://restapi.amap.com/v3/geocode/regeo?output=json&location=116.310003,39.991957&key=<用户的key>&radius=1000&extensions=base
        //https://restapi.amap.com/v3/geocode/regeo?output=xml&location=116.310003,39.991957&key=<用户的key>&radius=1000&extensions=base
        url = "http://restapi.amap.com/v3/geocode/regeo?output=xml&location=" + getLng + "," + getLat
                + "&key=" + key + "&radius=0&extensions=base";
        // 高德接品返回的是JSON格式的字符串
        String queryResult = getResponse(url);
        return queryResult;
    }

    /**
     * 2.根据两个定位点的经纬度算出两点间的距离
     *
     * @param startLonLat 起始经纬度
     * @param endLonLat   结束经纬度（目标经纬度）
     * @return 两个定位点之间的距离
     */
    private static long getDistance(String startLonLat, String endLonLat) {
        // 返回起始地startAddr与目的地endAddr之间的距离，单位：米
        Long result = Long.valueOf(0L);
        String queryUrl = "http://restapi.amap.com/v3/distance?key=" + key + "&origins=" + startLonLat + "&destination="
                + endLonLat;
        String queryResult = getResponse(queryUrl);
        JSONObject job = JSONObject.parseObject(queryResult);
        JSONArray ja = job.getJSONArray("results");
        JSONObject jobO = JSONObject.parseObject(ja.getString(0));
        result = Long.parseLong(jobO.get("distance").toString());
//		System.out.println("距离2：" + result);
        return result;
    }

    /**
     * 3.发送请求
     *
     * @param serverUrl 请求地址
     */
    private static String getResponse(String serverUrl) {
        // 用JAVA发起http请求，并返回json格式的结果
        StringBuffer result = new StringBuffer();
        try {
            URL url = new URL(serverUrl);
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

}
