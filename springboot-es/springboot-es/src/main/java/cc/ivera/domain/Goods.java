package cc.ivera.domain;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Date;
import java.util.Map;

public class Goods {
    private Long id;
    private String title;
    private Double price;
    private Integer stock;
    private Integer saleNum;
    private Date createTime;
    private String categoryName;
    private String brandName;
    private Map spec;

    /**
     * Goods对象转换成json串时，忽略此字段
     */
    @JSONField(serialize = false)
    private String specStr;

    public Goods() {
    }

    public Goods(Long id, String title, Double price, Integer stock,
                 Integer saleNum, Date createTime, String categoryName,
                 String brandName, Map spec, String specStr) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.stock = stock;
        this.saleNum = saleNum;
        this.createTime = createTime;
        this.categoryName = categoryName;
        this.brandName = brandName;
        this.spec = spec;
        this.specStr = specStr;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getSaleNum() {
        return saleNum;
    }

    public void setSaleNum(Integer saleNum) {
        this.saleNum = saleNum;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public Map getSpec() {
        return spec;
    }

    public void setSpec(Map spec) {
        this.spec = spec;
    }

    public String getSpecStr() {
        return specStr;
    }

    public void setSpecStr(String specStr) {
        this.specStr = specStr;
    }

    @Override
    public String toString() {
        return "Goods{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", saleNum=" + saleNum +
                ", createTime=" + createTime +
                ", categoryName='" + categoryName + '\'' +
                ", brandName='" + brandName + '\'' +
                ", spec=" + spec +
                ", specStr='" + specStr + '\'' +
                '}';
    }
}