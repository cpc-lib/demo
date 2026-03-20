package cc.ivera.entity;

import lombok.Data;

/**
 * @version v1.0
 * @description
 * @since 2020/2/3 11:23
 */
@Data
public class TransactionalMessageContent {

    private Long id;
    private Long messageId;
    private String content;
}
