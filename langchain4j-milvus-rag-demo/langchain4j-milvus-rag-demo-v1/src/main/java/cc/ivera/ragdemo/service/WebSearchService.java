package cc.ivera.ragdemo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class WebSearchService {

    @Value("${tavily.api.key}")
    private String apiKey;

    public String search(String query) {

        try {

            String api = "https://api.tavily.com/search";

            String body = """
                    {
                      "api_key":"%s",
                      "query":"%s",
                      "search_depth":"basic",
                      "max_results":5
                    }
                    """.formatted(apiKey, query);

            URL url = new URL(api);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            conn.getOutputStream().write(body.getBytes());

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder result = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            return result.toString();

        } catch (Exception e) {
            return "web search error";
        }

    }
}