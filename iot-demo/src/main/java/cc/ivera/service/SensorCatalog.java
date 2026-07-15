package cc.ivera.service;

import cc.ivera.model.Sensor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SensorCatalog {
    private final List<Sensor> sensors = List.of(
            new Sensor("SN-10001", "产线A-电机1", "一号车间", 24.6, true),
            new Sensor("SN-10002", "配电柜-3号", "配电室", 31.2, true),
            new Sensor("SN-10003", "冷却系统-出口", "冷却站", 18.7, true),
            new Sensor("SN-10004", "轴承-2号", "二号车间", 28.4, true),
            new Sensor("SN-10005", "仓库-温度1", "成品仓", 22.1, true)
    );

    public List<Sensor> all() {
        return sensors;
    }

    public Sensor require(String id) {
        return sensors.stream().filter(sensor -> sensor.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知传感器: " + id));
    }
}
