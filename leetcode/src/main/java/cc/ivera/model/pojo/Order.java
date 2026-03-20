package cc.ivera.model.pojo;

public class Order {
    private static final long serialVersionUID = 1L;
    private Long Id;
    private Long shardingDbId;
    private Long shardingTableId;

    public Order(Long shardingDbId, Long shardingTableId) {
        this.shardingDbId = shardingDbId;
        this.shardingTableId = shardingTableId;
    }

    public Order() {
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Long getShardingDbId() {
        return shardingDbId;
    }

    public void setShardingDbId(Long shardingDbId) {
        this.shardingDbId = shardingDbId;
    }

    public Long getShardingTableId() {
        return shardingTableId;
    }

    public void setShardingTableId(Long shardingTableId) {
        this.shardingTableId = shardingTableId;
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }
}