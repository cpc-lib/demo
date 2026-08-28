package com.example.rag.service.tools;

import dev.langchain4j.agent.tool.Tool;

public class WeatherTool {

    private final TavilyTool tavily;

    public WeatherTool(TavilyTool tavily) {
        this.tavily = tavily;
    }

    @Tool("Get a weather forecast by searching the web (Tavily). Input should be a city name, e.g., Beijing.")
    public String weatherForecast(String city) {
        String q = city + " weather forecast today and next 7 days";
        return tavily.webSearch(q);
    }
}
