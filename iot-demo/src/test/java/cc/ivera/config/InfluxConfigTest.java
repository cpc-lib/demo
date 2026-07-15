package cc.ivera.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = InfluxConfig.class)
class InfluxConfigTest {
    @Autowired
    private InfluxProperties properties;

    @Test
    void localRunUsesComposeInfluxSettings() throws IOException {
        Map<String, String> envFile = Files.readAllLines(Path.of("env", ".env")).stream()
                .filter(line -> line.contains("="))
                .map(line -> line.split("=", 2))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));

        assertThat(properties.url()).isEqualTo("http://localhost:8086");
        assertThat(properties.token()).isEqualTo(envFile.get("INFLUXDB_TOKEN"));
        assertThat(properties.org()).isEqualTo(envFile.get("INFLUXDB_ORG"));
        assertThat(properties.bucket()).isEqualTo(envFile.get("INFLUXDB_BUCKET"));
    }
}
