package cc.ivera.service;

import cc.ivera.model.Sensor;
import cc.ivera.repository.TemperatureRepository;
import cc.ivera.websocket.TemperatureWebSocketHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TemperatureSimulatorTest {
    @Test
    void generatedValueStaysCloseToPhysicalBaseline() {
        SensorCatalog catalog = new SensorCatalog();
        TemperatureSimulator simulator = new TemperatureSimulator(
                catalog, mock(TemperatureRepository.class), mock(TemperatureWebSocketHandler.class), 5, 35);
        Sensor sensor = catalog.require("SN-10001");

        double value = simulator.nextValue(sensor, sensor.baseline());

        assertThat(value).isBetween(sensor.baseline() - 2.0, sensor.baseline() + 2.0);
    }
}
