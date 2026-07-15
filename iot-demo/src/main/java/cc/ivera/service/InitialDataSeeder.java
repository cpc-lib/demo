package cc.ivera.service;

import cc.ivera.config.InfluxProperties;
import cc.ivera.model.TemperatureReading;
import cc.ivera.repository.TemperatureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class InitialDataSeeder {
    private static final Logger log = LoggerFactory.getLogger(InitialDataSeeder.class);
    private final SensorCatalog catalog;
    private final TemperatureRepository repository;
    private final InfluxProperties properties;

    public InitialDataSeeder(SensorCatalog catalog, TemperatureRepository repository, InfluxProperties properties) {
        this.catalog = catalog;
        this.repository = repository;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedWhenEmpty() {
        try {
            if (!repository.findLatest(properties.bucket()).isEmpty()) return;
            Random random = new Random(42);
            Instant start = Instant.now().minus(24, ChronoUnit.HOURS);
            List<TemperatureReading> seed = new ArrayList<>();
            catalog.all().forEach(sensor -> {
                for (int minute = 0; minute < 24 * 60; minute += 5) {
                    double dailyWave = Math.sin((minute / 1440.0) * Math.PI * 2 - 1.5) * 2.8;
                    double value = Math.round((sensor.baseline() + dailyWave + random.nextGaussian() * 0.25) * 10.0) / 10.0;
                    String state = value > 35 ? "HIGH" : value < 5 ? "LOW" : "NORMAL";
                    seed.add(new TemperatureReading(sensor.id(), sensor.name(), sensor.location(), value, state,
                            start.plus(minute, ChronoUnit.MINUTES)));
                }
            });
            repository.saveAll(seed);
            log.info("Seeded {} historical IoT temperature points", seed.size());
        } catch (Exception ex) {
            log.warn("Historical seed skipped; realtime simulator will continue", ex);
        }
    }
}
