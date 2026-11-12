package com.demo.seata.common.enums;

import lombok.Getter;

/**
 * 幂等状态枚举
 */
@Getter
public enum IdempotentStatusEnum {

    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "处理成功"),
    FAILED("FAILED", "处理失败");

    private final String code;
    private final String desc;

    IdempotentStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
