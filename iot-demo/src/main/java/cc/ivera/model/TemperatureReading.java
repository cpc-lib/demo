package cc.ivera.model;

import java.time.Instant;

public record TemperatureReading(
        String sensorId,
        String sensorName,
        String location,
        double value,
        String status,
        Instant timestamp) {
}
