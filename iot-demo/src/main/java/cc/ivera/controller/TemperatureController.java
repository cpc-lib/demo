package cc.ivera.controller;

import cc.ivera.config.InfluxProperties;
import cc.ivera.model.Sensor;
import cc.ivera.model.TemperatureReading;
import cc.ivera.repository.TemperatureRepository;
import cc.ivera.service.SensorCatalog;
import cc.ivera.service.TemperatureSimulator;
import cc.ivera.websocket.TemperatureWebSocketHandler;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api")
public class TemperatureController {
    private final SensorCatalog catalog;
    private final TemperatureRepository repository;
    private final TemperatureSimulator simulator;
    private final TemperatureWebSocketHandler socketHandler;
    private final InfluxProperties properties;

    public TemperatureController(SensorCatalog catalog, TemperatureRepository repository,
                                 TemperatureSimulator simulator, TemperatureWebSocketHandler socketHandler,
                                 InfluxProperties properties) {
        this.catalog = catalog;
        this.repository = repository;
        this.simulator = simulator;
        this.socketHandler = socketHandler;
        this.properties = properties;
    }

    @GetMapping("/sensors")
    public List<Sensor> sensors() {
        return catalog.all();
    }

    @GetMapping("/temperature/latest")
    public List<TemperatureReading> latest() {
        return repository.findLatest(properties.bucket());
    }

    @GetMapping("/temperature/history")
    public List<TemperatureReading> history(@RequestParam String sensorId,
                                            @RequestParam(defaultValue = "24") @Min(1) @Max(720) int hours) {
        catalog.require(sensorId);
        return repository.findHistory(properties.bucket(), sensorId, Duration.ofHours(hours));
    }

    @PostMapping("/simulator/generate/{sensorId}")
    public TemperatureReading generate(@PathVariable String sensorId) {
        return simulator.generateOne(sensorId);
    }

    @PostMapping("/simulator/{action}")
    public Map<String, Object> simulator(@PathVariable String action) {
        boolean running = switch (action) {
            case "start" -> simulator.setRunning(true);
            case "stop" -> simulator.setRunning(false);
            default -> throw new IllegalArgumentException("action 只支持 start 或 stop");
        };
        return Map.of("running", running, "webSocketConnections", socketHandler.connectionCount());
    }

    @GetMapping("/system/status")
    public Map<String, Object> status() {
        return Map.of("simulatorRunning", simulator.isRunning(),
                "webSocketConnections", socketHandler.connectionCount(), "bucket", properties.bucket());
    }
}
