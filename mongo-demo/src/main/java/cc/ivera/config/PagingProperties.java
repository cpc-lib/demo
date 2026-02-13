package cc.ivera.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.paging")
public class PagingProperties {
    private int maxPageSize = 200;
    private int defaultPageSize = 20;
}
