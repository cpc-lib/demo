package com.example.fencing.repository;

import com.example.fencing.model.ProductSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {
    private final JdbcTemplate jdbc;

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ProductSnapshot> find(String productId) {
        return jdbc.query("""
                SELECT product_id, name, stock, sold, fence_token
                FROM seckill_product WHERE product_id = ?
                """, rs -> rs.next() ? Optional.of(new ProductSnapshot(
                rs.getString("product_id"),
                rs.getString("name"),
                rs.getLong("stock"),
                rs.getLong("sold"),
                rs.getLong("fence_token"))) : Optional.empty(),
                productId);
    }

    /** 数据库中的商品列表。 */
    public List<ProductSnapshot> findAll() {
        return jdbc.query("""
                SELECT product_id, name, stock, sold, fence_token
                FROM seckill_product ORDER BY product_id
                """, (rs, rowNum) -> new ProductSnapshot(
                rs.getString("product_id"),
                rs.getString("name"),
                rs.getLong("stock"),
                rs.getLong("sold"),
                rs.getLong("fence_token")));
    }
}
