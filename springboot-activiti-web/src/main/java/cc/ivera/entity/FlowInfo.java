package cc.ivera.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class FlowInfo implements Serializable {
    private Long id;
    private String flow_name;
    private String flow_key;
    private String file_path;
    /**
     * 1- 没有部署  0- 已经部署
     */
    private Integer state;
    private Date create_time;
}
