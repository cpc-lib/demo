package cc.ivera.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 出差申请
 */
@Data
public class Evection implements Serializable {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 用户id
     */
    private Long user_id;
    /**
     * 出差申请单名称
     */
    private String evection_name;
    /**
     * 出差天数
     */
    private Double num;
    /**
     * 预计开始时间
     */
    private Date begin_date;
    /**
     * 预计结束时间
     */
    private Date end_date;
    /**
     * 目的地
     */
    private String destination;
    /**
     * 出差事由
     */
    private String reason;
    /**
     * 0-初始录入   1-开始审批     2-审批完成
     */
    private int state;
}
