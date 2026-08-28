package com.example.versionedcachemp.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户更新事件，用于异步刷新缓存。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedEvent {

    private Long userId;
    private Long version;
}
