package cc.ivera.model.pojo;

import lombok.Data;

@Data
public class GeoQuery {

    // 经度
    private double x;
    // 纬度
    private double y;
    // 范围大小
    private double value;

    private String member;

    private String member1;
    private String member2;
}
