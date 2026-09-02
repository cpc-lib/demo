package cc.ivera.vo;

import cc.ivera.entity.Product;
import cc.ivera.enums.ProductStatus;
import lombok.Data;

import java.util.Date;

@Data
public class ProductAdminView {

    private Long id;

    private String title;

    private Integer price;

    private ProductStatus status;

    private Integer availableStock;

    private Integer lockedStock;

    private Integer soldStock;

    private Long totalStock;

    private Integer version;

    private Date createTime;

    private Date updateTime;

    public static ProductAdminView from(Product product) {
        ProductAdminView view = new ProductAdminView();
        view.setId(product.getId());
        view.setTitle(product.getTitle());
        view.setPrice(product.getPrice());
        view.setStatus(product.getStatus());
        view.setAvailableStock(product.getAvailableStock());
        view.setLockedStock(product.getLockedStock());
        view.setSoldStock(product.getSoldStock());
        view.setTotalStock(stock(product.getAvailableStock())
                + stock(product.getLockedStock())
                + stock(product.getSoldStock()));
        view.setVersion(product.getVersion());
        view.setCreateTime(product.getCreateTime());
        view.setUpdateTime(product.getUpdateTime());
        return view;
    }

    private static long stock(Integer value) {
        return value == null ? 0L : value.longValue();
    }
}
