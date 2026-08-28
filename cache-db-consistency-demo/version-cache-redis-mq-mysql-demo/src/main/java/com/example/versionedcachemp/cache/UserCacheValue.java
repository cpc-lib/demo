package com.example.versionedcachemp.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 缓存中的结构：数据 + version。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCacheValue {

    private Long id;
    private String username;
    private java.math.BigDecimal balance;
    private Long version;
}
