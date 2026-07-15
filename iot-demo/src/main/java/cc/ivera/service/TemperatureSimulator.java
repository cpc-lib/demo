package cc.ivera.service;

import cc.ivera.model.Sensor;
import cc.ivera.model.TemperatureReading;
import cc.ivera.repository.TemperatureRepository;
import cc.ivera.websocket.TemperatureWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TemperatureSimulator {
    private static final Logger log = LoggerFactory.getLogger(TemperatureSimulator.class);
    private final SensorCatalog catalog;
    private final TemperatureRepository repository;
    private final TemperatureWebSocketHandler socketHandler;
    private final Random random = new Random();
    private final Map<String, Double> currentValues = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final double lowThreshold;
    private final double highThreshold;

    public TemperatureSimulator(SensorCatalog catalog,
                                TemperatureRepository repository,
                                TemperatureWebSocketHandler socketHandler,
                                @Value("${simulator.low-threshold:5}") double lowThreshold,
                                @Value("${simulator.high-threshold:35}") double highThreshold) {
        this.catalog = catalog;
        this.repository = repository;
        this.socketHandler = socketHandler;
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
        catalog.all().forEach(sensor -> currentValues.put(sensor.id(), sensor.baseline()));
    }

    @Scheduled(fixedDelayString = "${simulator.interval-ms:5000}",
            initialDelayString = "${simulator.initial-delay-ms:5000}")
    public void generate() {
        if (!running.get()) return;
        catalog.all().stream().filter(Sensor::enabled).forEach(sensor -> {
            try {
                double value = nextValue(sensor, currentValues.get(sensor.id()));
                currentValues.put(sensor.id(), value);
                TemperatureReading reading = reading(sensor, value, Instant.now());
                repository.save(reading);
                socketHandler.broadcast("temperature", reading);
                if (!"NORMAL".equals(reading.status())) {
                    socketHandler.broadcast("alert", reading);
                }
            } catch (Exception ex) {
                log.error("Failed to generate data for sensor {}", sensor.id(), ex);
            }
        });
    }

    double nextValue(Sensor sensor, double current) {
        double towardBaseline = (sensor.baseline() - current) * 0.12;
        double noise = random.nextGaussian() * 0.35;
        return Math.round((current + towardBaseline + noise) * 10.0) / 10.0;
    }

    public TemperatureReading generateOne(String sensorId) {
        Sensor sensor = catalog.require(sensorId);
        double value = nextValue(sensor, currentValues.get(sensor.id()));
        currentValues.put(sensor.id(), value);
        TemperatureReading reading = reading(sensor, value, Instant.now());
        repository.save(reading);
        socketHandler.broadcast("temperature", reading);
        return reading;
    }

    private TemperatureReading reading(Sensor sensor, double value, Instant at) {
        String status = value > highThreshold ? "HIGH" : value < lowThreshold ? "LOW" : "NORMAL";
        return new TemperatureReading(sensor.id(), sensor.name(), sensor.location(), value, status, at);
    }

    public boolean setRunning(boolean value) {
        running.set(value);
        socketHandler.broadcast("simulator", Map.of("running", value));
        return value;
    }

    public boolean isRunning() {
        return running.get();
    }
}
