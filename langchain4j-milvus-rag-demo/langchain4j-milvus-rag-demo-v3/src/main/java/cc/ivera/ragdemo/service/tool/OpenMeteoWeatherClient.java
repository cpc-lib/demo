package cc.ivera.ragdemo.service.tool;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.model.SourceType;
import cc.ivera.ragdemo.util.LogMasker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class OpenMeteoWeatherClient {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    public WeatherResult tomorrowWeather(String city) {
        if (!properties.getWeather().isEnabled()) {
            return new WeatherResult("天气工具未启用", List.of());
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getWeather().getTimeoutSeconds()))
                    .build();

            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String geoUrl = properties.getWeather().getGeocodingBaseUrl()
                    + "/v1/search?name=" + encodedCity
                    + "&count=1&language=zh&format=json";

            JsonNode geoRoot = getJson(client, geoUrl, properties.getWeather().getTimeoutSeconds());
            JsonNode first = geoRoot.path("results").isArray() && geoRoot.path("results").size() > 0
                    ? geoRoot.path("results").get(0)
                    : null;
            if (first == null) {
                return new WeatherResult("未找到城市“" + city + "”对应的天气位置数据。", List.of());
            }

            double latitude = first.path("latitude").asDouble();
            double longitude = first.path("longitude").asDouble();
            String resolvedName = first.path("name").asText(city);
            String country = first.path("country").asText("");
            String timezone = first.path("timezone").asText(properties.getWeather().getDefaultTimezone());

            String forecastUrl = properties.getWeather().getForecastBaseUrl()
                    + "/v1/forecast?latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max"
                    + "&timezone=" + URLEncoder.encode(timezone, StandardCharsets.UTF_8)
                    + "&forecast_days=3";

            JsonNode forecastRoot = getJson(client, forecastUrl, properties.getWeather().getTimeoutSeconds());
            JsonNode daily = forecastRoot.path("daily");
            if (!daily.isObject() || daily.path("time").size() < 2) {
                return new WeatherResult("天气服务返回数据不足，暂时无法给出“明天”的天气。", List.of());
            }

            int idx = 1;
            String date = daily.path("time").get(idx).asText();
            int weatherCode = daily.path("weather_code").get(idx).asInt();
            double maxTemp = daily.path("temperature_2m_max").get(idx).asDouble();
            double minTemp = daily.path("temperature_2m_min").get(idx).asDouble();
            int precipitationProbability = daily.path("precipitation_probability_max").get(idx).asInt();
            double maxWind = daily.path("wind_speed_10m_max").get(idx).asDouble();

            String summary = "城市：" + resolvedName + (country.isBlank() ? "" : (" / " + country)) + "\n"
                    + "日期：" + date + "\n"
                    + "天气：" + weatherCodeToChinese(weatherCode) + "\n"
                    + "最低温：" + minTemp + "°C\n"
                    + "最高温：" + maxTemp + "°C\n"
                    + "降水概率：" + precipitationProbability + "%\n"
                    + "最大风速：" + maxWind + " km/h\n"
                    + "来源：Open-Meteo Forecast API";

            List<SourceItem> sources = List.of(
                    SourceItem.builder()
                            .type(SourceType.WEATHER)
                            .title("Open-Meteo Geocoding API")
                            .url(geoUrl)
                            .content("用于城市地理编码解析：" + city)
                            .build(),
                    SourceItem.builder()
                            .type(SourceType.WEATHER)
                            .title("Open-Meteo Forecast API")
                            .url(forecastUrl)
                            .content(summary)
                            .build()
            );

            return new WeatherResult(summary, sources);
        } catch (Exception e) {
            throw new RuntimeException("天气查询失败: " + e.getMessage(), e);
        }
    }

    private JsonNode getJson(HttpClient client, String url, int timeoutSeconds) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP status=" + response.statusCode() + ", body=" + LogMasker.truncateAndMask(response.body()));
        }
        return objectMapper.readTree(response.body());
    }

    private String weatherCodeToChinese(int code) {
        return switch (code) {
            case 0 -> "晴朗";
            case 1 -> "大部晴";
            case 2 -> "局部多云";
            case 3 -> "阴天";
            case 45, 48 -> "雾";
            case 51, 53, 55 -> "毛毛雨";
            case 56, 57 -> "冻毛毛雨";
            case 61 -> "小雨";
            case 63 -> "中雨";
            case 65 -> "大雨";
            case 66, 67 -> "冻雨";
            case 71 -> "小雪";
            case 73 -> "中雪";
            case 75 -> "大雪";
            case 77 -> "冰粒";
            case 80 -> "阵雨";
            case 81 -> "中等阵雨";
            case 82 -> "强阵雨";
            case 85 -> "阵雪";
            case 86 -> "强阵雪";
            case 95 -> "雷暴";
            case 96, 99 -> "伴随冰雹的雷暴";
            default -> "未知天气(" + code + ")";
        };
    }

    public record WeatherResult(String summary, List<SourceItem> sources) {
    }
}
