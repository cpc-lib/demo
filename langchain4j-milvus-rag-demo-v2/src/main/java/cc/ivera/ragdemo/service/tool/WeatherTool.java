package cc.ivera.ragdemo.service.tool;

import cc.ivera.ragdemo.service.trace.AgentTraceContext;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeatherTool {

    private final OpenMeteoWeatherClient weatherClient;

    @Tool("查询指定城市的天气预报。适用于今天、明天、未来几天的天气、温度、降雨、风力等问题，例如：明天上海天气如何。")
    public String weatherForecast(String city) {
        OpenMeteoWeatherClient.WeatherResult result = weatherClient.tomorrowWeather(city);
        AgentTraceContext.current().setWeatherUsed(true);
        AgentTraceContext.current().addSources(result.sources());
        AgentTraceContext.current().addToolTrace("weatherForecast", "查询城市=" + city);
        return result.summary();
    }
}
