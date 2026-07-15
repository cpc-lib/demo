package cc.ivera.repository;

import cc.ivera.model.TemperatureReading;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Repository
public class TemperatureRepository {
    private final InfluxDBClient client;

    public TemperatureRepository(InfluxDBClient client) {
        this.client = client;
    }

    public void save(TemperatureReading reading) {
        WriteApiBlocking writeApi = client.getWriteApiBlocking();
        writeApi.writePoint(toPoint(reading));
    }

    public void saveAll(List<TemperatureReading> readings) {
        client.getWriteApiBlocking().writePoints(readings.stream().map(this::toPoint).toList());
    }

    private Point toPoint(TemperatureReading reading) {
        return Point.measurement("temperature")
                .addTag("sensorId", reading.sensorId())
                .addTag("sensorName", reading.sensorName())
                .addTag("location", reading.location())
                .addTag("status", reading.status())
                .addField("value", reading.value())
                .time(reading.timestamp(), WritePrecision.MS);
    }

    public List<TemperatureReading> findHistory(String bucket, String sensorId, Duration duration) {
        String flux = String.format(Locale.ROOT, """
                from(bucket: \"%s\")
                  |> range(start: -%ds)
                  |> filter(fn: (r) => r._measurement == \"temperature\" and r._field == \"value\")
                  |> filter(fn: (r) => r.sensorId == \"%s\")
                  |> sort(columns: [\"_time\"])
                """, escape(bucket), duration.toSeconds(), escape(sensorId));
        return map(client.getQueryApi().query(flux));
    }

    public List<TemperatureReading> findLatest(String bucket) {
        String flux = String.format(Locale.ROOT, """
                from(bucket: \"%s\")
                  |> range(start: -24h)
                  |> filter(fn: (r) => r._measurement == \"temperature\" and r._field == \"value\")
                  |> group(columns: [\"sensorId\"])
                  |> last()
                """, escape(bucket));
        return map(client.getQueryApi().query(flux)).stream()
                .sorted(Comparator.comparing(TemperatureReading::sensorName))
                .toList();
    }

    private List<TemperatureReading> map(List<FluxTable> tables) {
        return tables.stream().flatMap(table -> table.getRecords().stream())
                .map(this::mapRecord)
                .filter(Objects::nonNull)
                .toList();
    }

    private TemperatureReading mapRecord(FluxRecord record) {
        Object raw = record.getValue();
        if (!(raw instanceof Number number) || record.getTime() == null) return null;
        return new TemperatureReading(
                stringValue(record, "sensorId"),
                stringValue(record, "sensorName"),
                stringValue(record, "location"),
                Math.round(number.doubleValue() * 10.0) / 10.0,
                stringValue(record, "status"),
                record.getTime());
    }

    private String stringValue(FluxRecord record, String key) {
        Object value = record.getValueByKey(key);
        return value == null ? "" : value.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
