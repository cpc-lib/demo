package cc.ivera.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@MapperScan("cc.ivera.multidbtx.mapper")
@Component
public class MapperScanCompoent {
}
