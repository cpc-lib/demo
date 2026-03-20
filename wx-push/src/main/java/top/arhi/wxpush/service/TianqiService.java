package top.arhi.wxpush.service;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * @Description: TODO
 */
public interface TianqiService {
    JSONObject getWeatherByCity();

    JSONObject getWeatherByIP();
    Map<String, String> getTheNextThreeDaysWeather();

}
