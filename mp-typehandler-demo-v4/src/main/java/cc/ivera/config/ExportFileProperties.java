package cc.ivera.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "export.file")
public class ExportFileProperties {

    /**
     * 导出文件过期小时数
     */
    private long expireMinutes = 1L;
}
