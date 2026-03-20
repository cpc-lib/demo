package cc.ivera.model.pojo;

import lombok.Data;
import org.springframework.data.geo.Metric;

@Data
public class GeoRadiusQuery {

    private String key;
    private double longitude;
    private double latitude;
    private double value;
    private int limit;
    private int sort;
    private String member;
    private Metric metric;

}
