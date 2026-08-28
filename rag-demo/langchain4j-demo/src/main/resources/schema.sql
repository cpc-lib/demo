CREATE TABLE `chat_memory_message`
(
    `id`         bigint                                  NOT NULL AUTO_INCREMENT,
    `memory_id`  varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
    `msg_index`  int                                     NOT NULL,
    `role`       varchar(32) COLLATE utf8mb4_general_ci  NOT NULL,
    `content`    text COLLATE utf8mb4_general_ci         NOT NULL,
    `created_at` timestamp                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_memory_idx` (`memory_id`,`msg_index`),
    KEY          `idx_memory_id` (`memory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
