-- Consolidated SQL bootstrap for langchain4j-milvus-rag-demo-v2
-- Generated from src/main/resources/sql/*.sql in dependency order.
-- Keep this file as the single source SQL entrypoint for local MySQL init.
SET NAMES utf8mb4;


-- ============================================================================
-- Source: db.sql
-- ============================================================================

CREATE TABLE `work_order`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    `ticket_no`   VARCHAR(64)  NOT NULL COMMENT '工单编号（唯一）',
    `title`       VARCHAR(255) NOT NULL COMMENT '工单标题',

    `status`      VARCHAR(32)  NOT NULL COMMENT '工单状态（NEW/PROCESSING/DONE/CLOSED）',
    `priority`    VARCHAR(32)           DEFAULT NULL COMMENT '优先级（LOW/MEDIUM/HIGH/URGENT）',

    `assignee_id` VARCHAR(64)           DEFAULT NULL COMMENT '处理人ID',
    `assignee`    VARCHAR(64)           DEFAULT NULL COMMENT '处理人姓名',

    `creator`     VARCHAR(64)           DEFAULT NULL COMMENT '创建人',

    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_ticket_no` (`ticket_no`),

    KEY           `idx_status` (`status`),
    KEY           `idx_assignee_id` (`assignee_id`),
    KEY           `idx_created_at` (`created_at`)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_general_ci
COMMENT='工单表';


-- ============================================================================
-- Source: rag_schema.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS `rag_knowledge_base`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Knowledge base ID',
    `tenant_id`           BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
    `kb_code`             VARCHAR(64)     NOT NULL COMMENT 'Knowledge base code',
    `name`                VARCHAR(128)    NOT NULL COMMENT 'Knowledge base name',
    `description`         VARCHAR(1000)            DEFAULT NULL COMMENT 'Description',
    `vector_store_type`   VARCHAR(32)     NOT NULL DEFAULT 'milvus' COMMENT 'Vector store type',
    `vector_collection`   VARCHAR(128)    NOT NULL COMMENT 'Vector collection',
    `embedding_model`     VARCHAR(128)    NOT NULL COMMENT 'Embedding model',
    `embedding_dimension` INT UNSIGNED    NOT NULL COMMENT 'Embedding dimension',
    `chunk_strategy`      VARCHAR(32)     NOT NULL DEFAULT 'recursive' COMMENT 'Chunk strategy',
    `chunk_size`          INT UNSIGNED    NOT NULL DEFAULT 900 COMMENT 'Chunk size',
    `chunk_overlap`       INT UNSIGNED    NOT NULL DEFAULT 120 COMMENT 'Chunk overlap',
    `retrieval_top_k`     INT UNSIGNED    NOT NULL DEFAULT 6 COMMENT 'Default top k',
    `min_score`           DECIMAL(8, 6)            DEFAULT 0.550000 COMMENT 'Minimum score',
    `config_json`         JSON                     DEFAULT NULL COMMENT 'Extended config',
    `status`              TINYINT         NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
    `lock_version`        BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    `created_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`          TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_kb_code` (`tenant_id`, `kb_code`),
    UNIQUE KEY `uk_vector_collection` (`vector_collection`),
    KEY `idx_tenant_status` (`tenant_id`, `status`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG knowledge base';

CREATE TABLE IF NOT EXISTS `rag_document`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Document ID',
    `tenant_id`          BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id`  BIGINT UNSIGNED NOT NULL,
    `document_uid`       VARCHAR(64)     NOT NULL COMMENT 'Stable document UID',
    `document_name`      VARCHAR(255)    NOT NULL,
    `source_type`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1 upload, 2 web, 3 api, 4 db, 5 object store',
    `source_uri`         VARCHAR(1000)            DEFAULT NULL,
    `object_key`         VARCHAR(512)             DEFAULT NULL,
    `original_filename`  VARCHAR(255)             DEFAULT NULL,
    `file_extension`     VARCHAR(32)              DEFAULT NULL,
    `mime_type`          VARCHAR(128)             DEFAULT NULL,
    `file_size`          BIGINT UNSIGNED          DEFAULT 0,
    `file_hash`          CHAR(64)                 DEFAULT NULL,
    `current_version_id` BIGINT UNSIGNED          DEFAULT NULL,
    `current_version_no` INT UNSIGNED    NOT NULL DEFAULT 1,
    `page_count`         INT UNSIGNED             DEFAULT NULL,
    `chunk_count`        INT UNSIGNED    NOT NULL DEFAULT 0,
    `character_count`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `token_count`        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `parse_status`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
    `chunk_status`       TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
    `embedding_status`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
    `document_status`    TINYINT         NOT NULL DEFAULT 0 COMMENT '0 processing, 1 available, 2 failed, 3 disabled',
    `error_code`         VARCHAR(64)              DEFAULT NULL,
    `error_message`      VARCHAR(2000)            DEFAULT NULL,
    `metadata_json`      JSON                     DEFAULT NULL,
    `created_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_document_uid` (`tenant_id`, `document_uid`),
    KEY `idx_kb_status` (`tenant_id`, `knowledge_base_id`, `document_status`, `is_deleted`),
    KEY `idx_kb_hash` (`knowledge_base_id`, `file_hash`),
    KEY `idx_embedding_status` (`embedding_status`, `updated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG document';

CREATE TABLE IF NOT EXISTS `rag_document_version`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Document version ID',
    `tenant_id`           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id`   BIGINT UNSIGNED NOT NULL,
    `document_id`         BIGINT UNSIGNED NOT NULL,
    `version_no`          INT UNSIGNED    NOT NULL,
    `version_uid`         VARCHAR(64)     NOT NULL COMMENT 'Stable document version UID',
    `document_name`       VARCHAR(255)    NOT NULL,
    `source_type`         TINYINT         NOT NULL DEFAULT 1,
    `source_uri`          VARCHAR(1000)            DEFAULT NULL,
    `object_key`          VARCHAR(512)             DEFAULT NULL,
    `original_filename`   VARCHAR(255)             DEFAULT NULL,
    `file_extension`      VARCHAR(32)              DEFAULT NULL,
    `mime_type`           VARCHAR(128)             DEFAULT NULL,
    `file_size`           BIGINT UNSIGNED          DEFAULT 0,
    `file_hash`           CHAR(64)                 DEFAULT NULL,
    `page_count`          INT UNSIGNED             DEFAULT NULL,
    `chunk_count`         INT UNSIGNED    NOT NULL DEFAULT 0,
    `character_count`     BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `token_count`         BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `parse_status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
    `chunk_status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
    `embedding_status`    TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
    `version_status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '0 processing, 1 available, 2 failed, 3 disabled, 4 deleted',
    `is_current`          TINYINT         NOT NULL DEFAULT 0,
    `version_note`        VARCHAR(2000)            DEFAULT NULL COMMENT 'Version note',
    `approval_status`     VARCHAR(32)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, PENDING_REVIEW, APPROVED, REJECTED, PUBLISHED',
    `approval_comment`    VARCHAR(2000)            DEFAULT NULL COMMENT 'Approval comment',
    `approved_by`         VARCHAR(128)             DEFAULT NULL COMMENT 'Reviewer or operator',
    `approved_at`         DATETIME(3)              DEFAULT NULL COMMENT 'Review timestamp',
    `published_at`        DATETIME(3)              DEFAULT NULL COMMENT 'Publish timestamp',
    `error_code`          VARCHAR(64)              DEFAULT NULL,
    `error_message`       VARCHAR(2000)            DEFAULT NULL,
    `metadata_json`       JSON                     DEFAULT NULL,
    `created_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`          TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_document_version_no` (`document_id`, `version_no`),
    UNIQUE KEY `uk_tenant_document_version_uid` (`tenant_id`, `version_uid`),
    KEY `idx_document_current` (`document_id`, `is_current`, `is_deleted`),
    KEY `idx_kb_version_status` (`tenant_id`, `knowledge_base_id`, `version_status`, `is_deleted`),
    KEY `idx_version_approval` (`approval_status`, `updated_at`),
    KEY `idx_version_hash` (`knowledge_base_id`, `file_hash`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG document version';

CREATE TABLE IF NOT EXISTS `rag_document_chunk`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Chunk ID',
    `tenant_id`           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id`   BIGINT UNSIGNED NOT NULL,
    `document_id`         BIGINT UNSIGNED NOT NULL,
    `source_document_id`  VARCHAR(128)             DEFAULT NULL COMMENT 'Logical/external document ID used by chunk APIs',
    `document_version_id` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `chunk_uid`           VARCHAR(128)    NOT NULL,
    `chunk_version`       INT UNSIGNED    NOT NULL DEFAULT 1,
    `chunk_status`        VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, SUPERSEDED, DISABLED, DELETED',
    `is_current`          TINYINT         NOT NULL DEFAULT 1,
    `chunk_index`         INT UNSIGNED    NOT NULL,
    `parent_chunk_id`     BIGINT UNSIGNED          DEFAULT NULL,
    `parent_chunk_uid`    VARCHAR(128)             DEFAULT NULL,
    `source`              VARCHAR(64)              DEFAULT NULL,
    `file_name`           VARCHAR(255)             DEFAULT NULL,
    `content_type`        VARCHAR(32)     NOT NULL DEFAULT 'text',
    `page_start`          INT UNSIGNED             DEFAULT NULL,
    `page_end`            INT UNSIGNED             DEFAULT NULL,
    `char_start`          INT UNSIGNED             DEFAULT NULL,
    `char_end`            INT UNSIGNED             DEFAULT NULL,
    `title`               VARCHAR(500)             DEFAULT NULL,
    `section_path`        VARCHAR(1000)            DEFAULT NULL,
    `image_url`           VARCHAR(1000)            DEFAULT NULL,
    `image_caption`       VARCHAR(1000)            DEFAULT NULL,
    `image_number`        VARCHAR(64)              DEFAULT NULL,
    `permission_tags`     VARCHAR(1000)            DEFAULT NULL,
    `tenant_external_id`  VARCHAR(128)             DEFAULT NULL,
    `content`             MEDIUMTEXT      NOT NULL,
    `content_summary`     TEXT                     DEFAULT NULL,
    `content_hash`        CHAR(64)        NOT NULL,
    `character_count`     INT UNSIGNED    NOT NULL DEFAULT 0,
    `token_count`         INT UNSIGNED    NOT NULL DEFAULT 0,
    `vector_store_type`   VARCHAR(32)     NOT NULL DEFAULT 'milvus',
    `vector_collection`   VARCHAR(128)    NOT NULL,
    `milvus_alias`        VARCHAR(128)             DEFAULT NULL,
    `vector_id`           VARCHAR(128)             DEFAULT NULL,
    `text_vector_ids`     JSON                     DEFAULT NULL,
    `image_vector_ids`    JSON                     DEFAULT NULL,
    `embedding_model`     VARCHAR(128)             DEFAULT NULL,
    `embedding_dimension` INT UNSIGNED             DEFAULT NULL,
    `embedding_status`    TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
    `metadata_json`       JSON                     DEFAULT NULL,
    `created_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`          TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_chunk_uid_version` (`tenant_id`, `chunk_uid`, `chunk_version`),
    KEY `idx_document_chunk_index` (`document_id`, `document_version_id`, `chunk_index`),
    KEY `idx_chunk_current` (`chunk_uid`, `is_current`, `chunk_status`),
    KEY `idx_source_document` (`source_document_id`, `is_current`, `is_deleted`),
    KEY `idx_kb_document` (`tenant_id`, `knowledge_base_id`, `document_id`, `is_deleted`),
    KEY `idx_content_hash` (`knowledge_base_id`, `content_hash`),
    KEY `idx_embedding_status` (`embedding_status`, `updated_at`),
    KEY `idx_vector_id` (`vector_collection`, `vector_id`),
    KEY `idx_store_current` (`milvus_alias`, `vector_collection`, `is_current`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG document chunk';

CREATE TABLE IF NOT EXISTS `rag_ingestion_task`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Task ID',
    `tenant_id`           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id`   BIGINT UNSIGNED NOT NULL,
    `document_id`         BIGINT UNSIGNED NOT NULL,
    `document_version_id` BIGINT UNSIGNED          DEFAULT NULL,
    `task_no`             VARCHAR(64)     NOT NULL,
    `task_type`           VARCHAR(32)     NOT NULL,
    `task_status`         TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed, 4 retry_wait, 5 cancelled, 6 partial_success',
    `progress`            INT UNSIGNED    NOT NULL DEFAULT 0,
    `current_stage`       VARCHAR(64)              DEFAULT NULL,
    `stage_progress`      INT UNSIGNED    NOT NULL DEFAULT 0,
    `total_count`         INT UNSIGNED    NOT NULL DEFAULT 0,
    `success_count`       INT UNSIGNED    NOT NULL DEFAULT 0,
    `failed_count`        INT UNSIGNED    NOT NULL DEFAULT 0,
    `retry_count`         INT UNSIGNED    NOT NULL DEFAULT 0,
    `max_retry_count`     INT UNSIGNED    NOT NULL DEFAULT 3,
    `next_retry_at`       DATETIME(3)              DEFAULT NULL,
    `cancel_requested`    TINYINT         NOT NULL DEFAULT 0,
    `cancel_requested_at` DATETIME(3)              DEFAULT NULL,
    `cancel_requested_by` VARCHAR(128)             DEFAULT NULL,
    `partial_success`     TINYINT         NOT NULL DEFAULT 0,
    `last_event_id`       BIGINT UNSIGNED          DEFAULT NULL,
    `heartbeat_at`        DATETIME(3)              DEFAULT NULL,
    `error_code`          VARCHAR(64)              DEFAULT NULL,
    `error_message`       VARCHAR(2000)            DEFAULT NULL,
    `trace_id`            VARCHAR(64)              DEFAULT NULL,
    `idempotency_key`     VARCHAR(128)             DEFAULT NULL,
    `lock_version`        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `started_at`          DATETIME(3)              DEFAULT NULL,
    `finished_at`         DATETIME(3)              DEFAULT NULL,
    `created_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_task_no` (`tenant_id`, `task_no`),
    UNIQUE KEY `uk_tenant_idempotency_key` (`tenant_id`, `idempotency_key`),
    KEY `idx_task_schedule` (`task_status`, `next_retry_at`, `created_at`),
    KEY `idx_document_task` (`document_id`, `task_type`, `task_status`),
    KEY `idx_task_stage` (`current_stage`, `heartbeat_at`),
    KEY `idx_trace_id` (`trace_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG ingestion task';

CREATE TABLE IF NOT EXISTS `rag_ingestion_task_stage`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`       BIGINT UNSIGNED NOT NULL,
    `stage_code`    VARCHAR(64)     NOT NULL,
    `stage_name`    VARCHAR(128)    NOT NULL,
    `stage_order`   INT UNSIGNED    NOT NULL DEFAULT 0,
    `stage_weight`  INT UNSIGNED    NOT NULL DEFAULT 0,
    `stage_status`  VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    `progress`      INT UNSIGNED    NOT NULL DEFAULT 0,
    `total_count`   INT UNSIGNED    NOT NULL DEFAULT 0,
    `success_count` INT UNSIGNED    NOT NULL DEFAULT 0,
    `failed_count`  INT UNSIGNED    NOT NULL DEFAULT 0,
    `started_at`    DATETIME(3)              DEFAULT NULL,
    `finished_at`   DATETIME(3)              DEFAULT NULL,
    `error_code`    VARCHAR(128)             DEFAULT NULL,
    `error_message` VARCHAR(2000)            DEFAULT NULL,
    `metadata_json` JSON                     DEFAULT NULL,
    `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_stage` (`task_id`, `stage_code`),
    KEY `idx_stage_status` (`stage_status`, `updated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG ingestion task stage progress';

CREATE TABLE IF NOT EXISTS `rag_ingestion_task_shard`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`             BIGINT UNSIGNED NOT NULL,
    `stage_code`          VARCHAR(64)              DEFAULT NULL,
    `document_id`         BIGINT UNSIGNED          DEFAULT NULL,
    `document_version_id` BIGINT UNSIGNED          DEFAULT NULL,
    `shard_key`           VARCHAR(128)    NOT NULL,
    `shard_type`          VARCHAR(32)     NOT NULL,
    `shard_index`         INT UNSIGNED             DEFAULT NULL,
    `shard_status`        VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    `retry_count`         INT UNSIGNED    NOT NULL DEFAULT 0,
    `max_retry_count`     INT UNSIGNED    NOT NULL DEFAULT 3,
    `next_retry_at`       DATETIME(3)              DEFAULT NULL,
    `error_code`          VARCHAR(128)             DEFAULT NULL,
    `error_message`       VARCHAR(2000)            DEFAULT NULL,
    `input_hash`          CHAR(64)                 DEFAULT NULL,
    `output_ref`          VARCHAR(512)             DEFAULT NULL,
    `metadata_json`       JSON                     DEFAULT NULL,
    `created_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_shard` (`task_id`, `shard_key`, `shard_type`),
    KEY `idx_shard_retry` (`shard_status`, `next_retry_at`),
    KEY `idx_shard_document` (`document_id`, `document_version_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG ingestion task shard progress';

CREATE TABLE IF NOT EXISTS `rag_ingestion_task_event`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`        BIGINT UNSIGNED NOT NULL,
    `event_type`     VARCHAR(64)     NOT NULL,
    `stage_code`     VARCHAR(64)              DEFAULT NULL,
    `shard_key`      VARCHAR(128)             DEFAULT NULL,
    `progress`       INT UNSIGNED             DEFAULT NULL,
    `stage_progress` INT UNSIGNED             DEFAULT NULL,
    `message`        VARCHAR(1000)            DEFAULT NULL,
    `payload_json`   JSON                     DEFAULT NULL,
    `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_event_task` (`task_id`, `id`),
    KEY `idx_event_created` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG ingestion task event stream';

CREATE TABLE IF NOT EXISTS `rag_query_log`
(
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Query log ID',
    `tenant_id`               BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `trace_id`                VARCHAR(64)              DEFAULT NULL,
    `conversation_id`         VARCHAR(128)             DEFAULT NULL,
    `query_type`              VARCHAR(16)     NOT NULL COMMENT 'QUERY or SEARCH',
    `query_text`              TEXT            NOT NULL,
    `retrieval_mode`          VARCHAR(32)              DEFAULT NULL,
    `knowledge_base_ids_json` JSON                     DEFAULT NULL,
    `top_k`                   INT UNSIGNED             DEFAULT NULL,
    `min_score`               DECIMAL(8, 6)            DEFAULT NULL,
    `content_types_json`      JSON                     DEFAULT NULL,
    `permission_tags_json`    JSON                     DEFAULT NULL,
    `multimodal_trace_json`   JSON                     DEFAULT NULL,
    `prompt_text`             MEDIUMTEXT               DEFAULT NULL,
    `answer_text`             MEDIUMTEXT               DEFAULT NULL,
    `knowledge_hit`           TINYINT         NOT NULL DEFAULT 0,
    `hit_count`               INT UNSIGNED    NOT NULL DEFAULT 0,
    `prompt_tokens`           INT UNSIGNED             DEFAULT NULL,
    `completion_tokens`       INT UNSIGNED             DEFAULT NULL,
    `total_tokens`            INT UNSIGNED             DEFAULT NULL,
    `llm_provider`            VARCHAR(64)              DEFAULT NULL,
    `llm_model`               VARCHAR(128)             DEFAULT NULL,
    `embedding_provider`      VARCHAR(64)              DEFAULT NULL,
    `embedding_model`         VARCHAR(128)             DEFAULT NULL,
    `estimated_input_cost`    DECIMAL(18, 8)           DEFAULT NULL,
    `estimated_output_cost`   DECIMAL(18, 8)           DEFAULT NULL,
    `estimated_embedding_cost` DECIMAL(18, 8)          DEFAULT NULL,
    `estimated_total_cost`    DECIMAL(18, 8)           DEFAULT NULL,
    `cost_currency`           VARCHAR(16)              DEFAULT NULL,
    `latency_ms`              BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `status`                  VARCHAR(16)     NOT NULL DEFAULT 'SUCCESS',
    `archive_status`          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    `retention_until`         DATETIME(3)              DEFAULT NULL,
    `deleted_at`              DATETIME(3)              DEFAULT NULL,
    `deleted_by`              VARCHAR(128)             DEFAULT NULL,
    `delete_reason`           VARCHAR(1000)            DEFAULT NULL,
    `is_deleted`              TINYINT         NOT NULL DEFAULT 0,
    `error_code`              VARCHAR(64)              DEFAULT NULL,
    `error_message`           VARCHAR(2000)            DEFAULT NULL,
    `created_at`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_query_tenant_created` (`tenant_id`, `created_at`),
    KEY `idx_query_trace` (`trace_id`),
    KEY `idx_query_conversation` (`conversation_id`, `created_at`),
    KEY `idx_query_type_status` (`query_type`, `status`, `created_at`),
    KEY `idx_query_log_archive_status` (`archive_status`, `retention_until`, `created_at`),
    KEY `idx_query_log_deleted` (`is_deleted`, `deleted_at`),
    KEY `idx_query_cost_time` (`tenant_id`, `llm_model`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query audit log';

CREATE TABLE IF NOT EXISTS `rag_query_hit`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Query hit ID',
    `query_log_id`       BIGINT UNSIGNED NOT NULL,
    `rank_no`            INT UNSIGNED    NOT NULL,
    `score`              DECIMAL(12, 10)          DEFAULT NULL,
    `knowledge_base_id`  BIGINT UNSIGNED          DEFAULT NULL,
    `document_id`        VARCHAR(128)             DEFAULT NULL,
    `document_name`      VARCHAR(255)             DEFAULT NULL,
    `chunk_id`           VARCHAR(128)             DEFAULT NULL,
    `chunk_version`      INT UNSIGNED             DEFAULT NULL,
    `content_type`       VARCHAR(32)              DEFAULT NULL,
    `modality`           VARCHAR(32)              DEFAULT NULL,
    `retrieval_source`   VARCHAR(64)              DEFAULT NULL,
    `image_asset_id`     BIGINT UNSIGNED          DEFAULT NULL,
    `fusion_score`       DECIMAL(12, 10)          DEFAULT NULL,
    `page_no`            INT UNSIGNED             DEFAULT NULL,
    `section_title`      VARCHAR(500)             DEFAULT NULL,
    `image_url`          VARCHAR(1000)            DEFAULT NULL,
    `content_snippet`    TEXT                     DEFAULT NULL,
    `metadata_json`      JSON                     DEFAULT NULL,
    `created_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_query_hit_log` (`query_log_id`, `rank_no`),
    KEY `idx_query_hit_chunk` (`chunk_id`),
    KEY `idx_query_hit_document` (`document_id`),
    KEY `idx_query_hit_multimodal` (`modality`, `retrieval_source`, `image_asset_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query retrieval hit';

CREATE TABLE IF NOT EXISTS `rag_query_feedback`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Query feedback ID',
    `query_log_id`     BIGINT UNSIGNED NOT NULL,
    `rating`           VARCHAR(32)     NOT NULL COMMENT 'HELPFUL, NOT_HELPFUL, CORRECTION',
    `created_by`       VARCHAR(128)             DEFAULT NULL,
    `comment`          VARCHAR(2000)            DEFAULT NULL,
    `corrected_answer` MEDIUMTEXT               DEFAULT NULL,
    `feedback_status`  VARCHAR(32)     NOT NULL DEFAULT 'OPEN',
    `priority`         VARCHAR(32)     NOT NULL DEFAULT 'MEDIUM',
    `assignee`         VARCHAR(128)             DEFAULT NULL,
    `review_result`    VARCHAR(32)              DEFAULT NULL,
    `review_comment`   VARCHAR(2000)            DEFAULT NULL,
    `resolved_at`      DATETIME(3)              DEFAULT NULL,
    `closed_at`        DATETIME(3)              DEFAULT NULL,
    `reopened_count`   INT             NOT NULL DEFAULT 0,
    `created_at`       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_query_feedback_log` (`query_log_id`, `created_at`),
    KEY `idx_query_feedback_rating` (`rating`, `created_at`),
    KEY `idx_feedback_status` (`feedback_status`, `priority`, `updated_at`),
    KEY `idx_feedback_assignee` (`assignee`, `feedback_status`, `updated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query feedback';

CREATE TABLE IF NOT EXISTS `rag_query_log_delete_audit`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Delete audit ID',
    `delete_no`          VARCHAR(64)     NOT NULL COMMENT 'Operation batch number',
    `operator`           VARCHAR(128)             DEFAULT NULL,
    `delete_mode`        VARCHAR(32)     NOT NULL COMMENT 'SOFT_DELETE, ARCHIVE, PURGE, RESTORE',
    `reason`             VARCHAR(1000)            DEFAULT NULL,
    `query_log_ids_json` JSON                     DEFAULT NULL,
    `matched_count`      INT             NOT NULL DEFAULT 0,
    `success_count`      INT             NOT NULL DEFAULT 0,
    `failed_count`       INT             NOT NULL DEFAULT 0,
    `filter_json`        JSON                     DEFAULT NULL,
    `result_json`        JSON                     DEFAULT NULL,
    `created_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_query_log_delete_no` (`delete_no`),
    KEY `idx_query_log_delete_mode` (`delete_mode`, `created_at`),
    KEY `idx_query_log_delete_operator` (`operator`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query log delete audit';

CREATE TABLE IF NOT EXISTS `rag_query_log_archive`
(
    `id`                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Archive row ID',
    `source_query_log_id`      BIGINT UNSIGNED NOT NULL,
    `delete_no`                VARCHAR(64)              DEFAULT NULL,
    `tenant_id`                BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `trace_id`                 VARCHAR(64)              DEFAULT NULL,
    `conversation_id`          VARCHAR(128)             DEFAULT NULL,
    `query_type`               VARCHAR(16)     NOT NULL,
    `query_text`               TEXT            NOT NULL,
    `retrieval_mode`           VARCHAR(32)              DEFAULT NULL,
    `knowledge_base_ids_json`  JSON                     DEFAULT NULL,
    `top_k`                    INT UNSIGNED             DEFAULT NULL,
    `min_score`                DECIMAL(8, 6)            DEFAULT NULL,
    `content_types_json`       JSON                     DEFAULT NULL,
    `permission_tags_json`     JSON                     DEFAULT NULL,
    `prompt_text`              MEDIUMTEXT               DEFAULT NULL,
    `answer_text`              MEDIUMTEXT               DEFAULT NULL,
    `knowledge_hit`            TINYINT         NOT NULL DEFAULT 0,
    `hit_count`                INT UNSIGNED    NOT NULL DEFAULT 0,
    `prompt_tokens`            INT UNSIGNED             DEFAULT NULL,
    `completion_tokens`        INT UNSIGNED             DEFAULT NULL,
    `total_tokens`             INT UNSIGNED             DEFAULT NULL,
    `llm_provider`             VARCHAR(64)              DEFAULT NULL,
    `llm_model`                VARCHAR(128)             DEFAULT NULL,
    `embedding_provider`       VARCHAR(64)              DEFAULT NULL,
    `embedding_model`          VARCHAR(128)             DEFAULT NULL,
    `estimated_input_cost`     DECIMAL(18, 8)           DEFAULT NULL,
    `estimated_output_cost`    DECIMAL(18, 8)           DEFAULT NULL,
    `estimated_embedding_cost` DECIMAL(18, 8)           DEFAULT NULL,
    `estimated_total_cost`     DECIMAL(18, 8)           DEFAULT NULL,
    `cost_currency`            VARCHAR(16)              DEFAULT NULL,
    `latency_ms`               BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `status`                   VARCHAR(16)     NOT NULL DEFAULT 'SUCCESS',
    `archive_status`           VARCHAR(32)     NOT NULL DEFAULT 'ARCHIVED',
    `retention_until`          DATETIME(3)              DEFAULT NULL,
    `error_code`               VARCHAR(64)              DEFAULT NULL,
    `error_message`            VARCHAR(2000)            DEFAULT NULL,
    `query_created_at`         DATETIME(3)              DEFAULT NULL,
    `query_updated_at`         DATETIME(3)              DEFAULT NULL,
    `archived_at`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_query_archive_source` (`source_query_log_id`, `archived_at`),
    KEY `idx_query_archive_tenant_time` (`tenant_id`, `archived_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query log archive';

CREATE TABLE IF NOT EXISTS `rag_query_retention_policy`
(
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Retention policy ID',
    `tenant_id`             BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `policy_name`           VARCHAR(128)    NOT NULL,
    `query_type`            VARCHAR(32)     NOT NULL DEFAULT 'ALL',
    `status_filter`         VARCHAR(32)     NOT NULL DEFAULT 'ALL',
    `retention_days`        INT             NOT NULL DEFAULT 180,
    `archive_before_delete` TINYINT         NOT NULL DEFAULT 1,
    `enabled`               TINYINT         NOT NULL DEFAULT 1,
    `created_at`            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_retention_policy_match` (`tenant_id`, `query_type`, `status_filter`, `enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query retention policy';

CREATE TABLE IF NOT EXISTS `rag_model_pricing`
(
    `id`                        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Model pricing ID',
    `provider`                  VARCHAR(64)     NOT NULL,
    `model`                     VARCHAR(128)    NOT NULL,
    `input_cost_per_1k_tokens`  DECIMAL(18, 8)  NOT NULL DEFAULT 0,
    `output_cost_per_1k_tokens` DECIMAL(18, 8)  NOT NULL DEFAULT 0,
    `currency`                  VARCHAR(16)     NOT NULL DEFAULT 'USD',
    `effective_from`            DATETIME(3)              DEFAULT NULL,
    `effective_to`              DATETIME(3)              DEFAULT NULL,
    `enabled`                   TINYINT         NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    KEY `idx_model_pricing_effective` (`provider`, `model`, `enabled`, `effective_from`, `effective_to`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG model token pricing';

CREATE TABLE IF NOT EXISTS `rag_query_cost_anomaly`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Cost anomaly ID',
    `tenant_id`      BIGINT UNSIGNED          DEFAULT NULL,
    `anomaly_type`   VARCHAR(64)     NOT NULL,
    `severity`       VARCHAR(32)     NOT NULL DEFAULT 'LOW',
    `metric_name`    VARCHAR(64)     NOT NULL,
    `metric_value`   DECIMAL(18, 6)           DEFAULT NULL,
    `baseline_value` DECIMAL(18, 6)           DEFAULT NULL,
    `window_start`   DATETIME(3)              DEFAULT NULL,
    `window_end`     DATETIME(3)              DEFAULT NULL,
    `status`         VARCHAR(32)     NOT NULL DEFAULT 'OPEN',
    `metadata_json`  JSON                     DEFAULT NULL,
    `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_query_cost_anomaly` (`tenant_id`, `status`, `severity`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query cost anomaly';

CREATE TABLE IF NOT EXISTS `rag_query_feedback_event`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Feedback event ID',
    `feedback_id`  BIGINT UNSIGNED NOT NULL,
    `event_type`   VARCHAR(64)     NOT NULL,
    `from_status`  VARCHAR(32)              DEFAULT NULL,
    `to_status`    VARCHAR(32)              DEFAULT NULL,
    `operator`     VARCHAR(128)             DEFAULT NULL,
    `comment`      VARCHAR(2000)            DEFAULT NULL,
    `payload_json` JSON                     DEFAULT NULL,
    `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_feedback_event` (`feedback_id`, `created_at`),
    KEY `idx_feedback_event_type` (`event_type`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query feedback event';

CREATE TABLE IF NOT EXISTS `rag_feedback_revision_task`
(
    `id`                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Feedback revision task ID',
    `revision_no`              VARCHAR(64)     NOT NULL,
    `feedback_id`              BIGINT UNSIGNED NOT NULL,
    `query_log_id`             BIGINT UNSIGNED          DEFAULT NULL,
    `tenant_id`                BIGINT UNSIGNED          DEFAULT NULL,
    `knowledge_base_id`        BIGINT UNSIGNED          DEFAULT NULL,
    `document_id`              BIGINT UNSIGNED          DEFAULT NULL,
    `chunk_uid`                VARCHAR(128)             DEFAULT NULL,
    `revision_type`            VARCHAR(32)     NOT NULL DEFAULT 'OTHER',
    `revision_status`          VARCHAR(32)     NOT NULL DEFAULT 'PLANNED',
    `before_snapshot_json`     JSON                     DEFAULT NULL,
    `after_snapshot_json`      JSON                     DEFAULT NULL,
    `expected_fix`             VARCHAR(2000)            DEFAULT NULL,
    `verification_query`       TEXT                     DEFAULT NULL,
    `verification_result_json` JSON                     DEFAULT NULL,
    `created_by`               VARCHAR(128)             DEFAULT NULL,
    `assignee`                 VARCHAR(128)             DEFAULT NULL,
    `created_at`               DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`               DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_feedback_revision_no` (`revision_no`),
    KEY `idx_feedback_revision_feedback` (`feedback_id`, `created_at`),
    KEY `idx_feedback_revision_status` (`revision_status`, `updated_at`),
    KEY `idx_feedback_revision_kb` (`tenant_id`, `knowledge_base_id`, `revision_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG feedback revision task';

CREATE TABLE IF NOT EXISTS `rag_image_asset`
(
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Image asset ID',
    `tenant_id`             BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `document_id`           BIGINT UNSIGNED          DEFAULT NULL,
    `document_version_id`   BIGINT UNSIGNED          DEFAULT NULL,
    `source_document_id`    VARCHAR(128)             DEFAULT NULL COMMENT 'Logical document UID',
    `image_id`              VARCHAR(128)    NOT NULL COMMENT 'Stable extracted image ID',
    `chunk_uid`             VARCHAR(128)             DEFAULT NULL COMMENT 'Chunk UID generated from this image',
    `content_type`          VARCHAR(32)     NOT NULL DEFAULT 'image',
    `asset_path`            VARCHAR(1000)            DEFAULT NULL,
    `image_url`             VARCHAR(1000)            DEFAULT NULL,
    `page_no`               INT UNSIGNED             DEFAULT NULL,
    `coordinate_json`       JSON                     DEFAULT NULL,
    `section_title`         VARCHAR(500)             DEFAULT NULL,
    `image_caption`         VARCHAR(1000)            DEFAULT NULL,
    `image_number`          VARCHAR(64)              DEFAULT NULL,
    `ocr_text`              MEDIUMTEXT               DEFAULT NULL,
    `ocr_status`            VARCHAR(32)              DEFAULT NULL,
    `ocr_confidence`        DECIMAL(6, 5)            DEFAULT NULL,
    `ocr_provider`          VARCHAR(64)              DEFAULT NULL,
    `ocr_model`             VARCHAR(128)             DEFAULT NULL,
    `ocr_error_message`     VARCHAR(2000)            DEFAULT NULL,
    `visual_status`         VARCHAR(32)     NOT NULL DEFAULT 'EMPTY' COMMENT 'SUCCESS, INVALID, FAILED, EMPTY',
    `visual_schema_valid`   TINYINT         NOT NULL DEFAULT 0,
    `visual_confidence`     DECIMAL(6, 5)            DEFAULT NULL,
    `visual_json`           JSON                     DEFAULT NULL,
    `visual_schema_errors`  JSON                     DEFAULT NULL,
    `text_vector_ids`       JSON                     DEFAULT NULL,
    `image_vector_ids`      JSON                     DEFAULT NULL,
    `image_embedding_status` VARCHAR(32)             DEFAULT NULL,
    `image_embedding_model` VARCHAR(128)             DEFAULT NULL,
    `image_embedding_dimension` INT UNSIGNED         DEFAULT NULL,
    `image_embedding_error_message` VARCHAR(2000)    DEFAULT NULL,
    `image_embedding_updated_at` DATETIME(3)         DEFAULT NULL,
    `review_status`         VARCHAR(32)     NOT NULL DEFAULT 'EMPTY',
    `review_comment`        VARCHAR(1000)            DEFAULT NULL,
    `reviewed_by`           VARCHAR(128)             DEFAULT NULL,
    `reviewed_at`           DATETIME(3)              DEFAULT NULL,
    `review_updated_visual_json` JSON                DEFAULT NULL,
    `review_updated_ocr_text` MEDIUMTEXT             DEFAULT NULL,
    `created_at`            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`            TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_image_id` (`tenant_id`, `image_id`),
    KEY `idx_image_asset_kb_doc` (`tenant_id`, `knowledge_base_id`, `source_document_id`, `is_deleted`),
    KEY `idx_image_asset_chunk` (`chunk_uid`),
    KEY `idx_image_asset_status` (`visual_status`, `visual_confidence`),
    KEY `idx_image_asset_review` (`review_status`, `updated_at`),
    KEY `idx_image_asset_ocr` (`ocr_status`, `ocr_confidence`),
    KEY `idx_image_asset_embedding` (`image_embedding_status`, `image_embedding_updated_at`),
    KEY `idx_image_asset_updated` (`updated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG image asset and multimodal governance metadata';

CREATE TABLE IF NOT EXISTS `rag_retrieval_eval_case`
(
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Evaluation case ID',
    `tenant_id`               BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id`       BIGINT UNSIGNED          DEFAULT NULL,
    `version_tag`             VARCHAR(128)    NOT NULL DEFAULT 'default' COMMENT 'Knowledge base or document version label',
    `case_id`                 VARCHAR(128)    NOT NULL,
    `query_text`              TEXT            NOT NULL,
    `retrieval_mode`          VARCHAR(32)     NOT NULL DEFAULT 'hybrid',
    `query_category`          VARCHAR(64)     NOT NULL DEFAULT 'definition',
    `difficulty_level`        VARCHAR(32)     NOT NULL DEFAULT 'easy',
    `language`                VARCHAR(32)     NOT NULL DEFAULT 'mixed',
    `expected_answer_type`    VARCHAR(64)     NOT NULL DEFAULT 'fact',
    `top_k`                   INT UNSIGNED             DEFAULT NULL,
    `min_score`               DECIMAL(8, 6)            DEFAULT NULL,
    `content_types_json`      JSON                     DEFAULT NULL,
    `permission_tags_json`    JSON                     DEFAULT NULL,
    `expected_chunk_ids_json` JSON            NOT NULL,
    `enabled`                 TINYINT         NOT NULL DEFAULT 1,
    `metadata_json`           JSON                     DEFAULT NULL,
    `created_at`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`              TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_eval_case_id` (`tenant_id`, `case_id`),
    KEY `idx_eval_case_kb_version` (`tenant_id`, `knowledge_base_id`, `version_tag`, `enabled`, `is_deleted`),
    KEY `idx_eval_case_category` (`tenant_id`, `knowledge_base_id`, `query_category`, `enabled`, `is_deleted`),
    KEY `idx_eval_case_updated` (`updated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG retrieval evaluation case';

CREATE TABLE IF NOT EXISTS `rag_retrieval_eval_run`
(
    `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Evaluation run ID',
    `run_no`               VARCHAR(64)     NOT NULL,
    `tenant_id`            BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id`    BIGINT UNSIGNED          DEFAULT NULL,
    `version_tag`          VARCHAR(128)    NOT NULL DEFAULT 'default',
    `retrieval_mode`       VARCHAR(32)     NOT NULL DEFAULT 'hybrid',
    `total_cases`          INT UNSIGNED    NOT NULL DEFAULT 0,
    `hit_rate`             DECIMAL(10, 6)  NOT NULL DEFAULT 0,
    `mean_reciprocal_rank` DECIMAL(10, 6)  NOT NULL DEFAULT 0,
    `mean_recall`          DECIMAL(10, 6)  NOT NULL DEFAULT 0,
    `source`               VARCHAR(32)     NOT NULL DEFAULT 'mysql',
    `metadata_json`        JSON                     DEFAULT NULL,
    `created_at`           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_eval_run_no` (`tenant_id`, `run_no`),
    KEY `idx_eval_run_kb_version` (`tenant_id`, `knowledge_base_id`, `version_tag`, `retrieval_mode`, `created_at`),
    KEY `idx_eval_run_created` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG retrieval evaluation run history';

CREATE TABLE IF NOT EXISTS `rag_retrieval_eval_case_result`
(
    `id`                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Evaluation case result ID',
    `run_id`                   BIGINT UNSIGNED NOT NULL,
    `case_db_id`               BIGINT UNSIGNED          DEFAULT NULL,
    `case_id`                  VARCHAR(128)             DEFAULT NULL,
    `query_text`               TEXT            NOT NULL,
    `top_k`                    INT UNSIGNED    NOT NULL DEFAULT 0,
    `expected_chunk_ids_json`  JSON            NOT NULL,
    `retrieved_chunk_ids_json` JSON            NOT NULL,
    `hit`                      TINYINT         NOT NULL DEFAULT 0,
    `reciprocal_rank`          DECIMAL(10, 6)  NOT NULL DEFAULT 0,
    `recall_value`             DECIMAL(10, 6)  NOT NULL DEFAULT 0,
    `failure_type`             VARCHAR(64)              DEFAULT NULL,
    `failure_reason`           VARCHAR(1000)            DEFAULT NULL,
    `retrieval_trace_json`     JSON                     DEFAULT NULL,
    `cluster_key`              VARCHAR(128)             DEFAULT NULL,
    `created_at`               DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_eval_result_run` (`run_id`, `id`),
    KEY `idx_eval_result_case` (`case_db_id`, `created_at`),
    KEY `idx_eval_result_hit` (`hit`, `created_at`),
    KEY `idx_eval_result_failure` (`failure_type`, `cluster_key`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG retrieval evaluation per-case result';

CREATE TABLE IF NOT EXISTS `rag_retrieval_eval_cluster`
(
    `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Evaluation failure cluster ID',
    `run_id`               BIGINT UNSIGNED NOT NULL,
    `cluster_key`          VARCHAR(128)    NOT NULL,
    `cluster_label`        VARCHAR(255)    NOT NULL,
    `failure_type`         VARCHAR(64)     NOT NULL,
    `case_count`           INT UNSIGNED    NOT NULL DEFAULT 0,
    `sample_case_ids_json` JSON                     DEFAULT NULL,
    `suggestion`           VARCHAR(2000)            DEFAULT NULL,
    `created_at`           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eval_cluster_run_key` (`run_id`, `cluster_key`),
    KEY `idx_eval_cluster_run` (`run_id`, `case_count`),
    KEY `idx_eval_cluster_type` (`failure_type`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG retrieval evaluation failure cluster';

CREATE TABLE IF NOT EXISTS `rag_rerank_call_log`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Rerank call log ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `provider`        VARCHAR(64)     NOT NULL,
    `model`           VARCHAR(128)    NOT NULL,
    `query_hash`      CHAR(64)        NOT NULL,
    `api_key_hash`    CHAR(16)                 DEFAULT NULL,
    `tenant_external_id` VARCHAR(128)          DEFAULT NULL,
    `request_window`  DATETIME(3)              DEFAULT NULL,
    `candidate_count` INT UNSIGNED    NOT NULL DEFAULT 0,
    `top_k`           INT UNSIGNED    NOT NULL DEFAULT 0,
    `input_tokens`    INT UNSIGNED    NOT NULL DEFAULT 0,
    `output_tokens`   INT UNSIGNED    NOT NULL DEFAULT 0,
    `total_tokens`    INT UNSIGNED    NOT NULL DEFAULT 0,
    `latency_ms`      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `success`         TINYINT         NOT NULL DEFAULT 0,
    `fallback`        TINYINT         NOT NULL DEFAULT 0,
    `estimated_cost`  DECIMAL(18, 8)  NOT NULL DEFAULT 0,
    `error_code`      VARCHAR(128)             DEFAULT NULL,
    `http_status`     INT UNSIGNED             DEFAULT NULL,
    `error_code_normalized` VARCHAR(64)        DEFAULT NULL,
    `degraded_reason` VARCHAR(64)              DEFAULT NULL,
    `retry_count`     INT UNSIGNED    NOT NULL DEFAULT 0,
    `cache_hit`       TINYINT         NOT NULL DEFAULT 0,
    `error_message`   VARCHAR(2000)            DEFAULT NULL,
    `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_rerank_model_created` (`tenant_id`, `provider`, `model`, `created_at`),
    KEY `idx_rerank_api_key_time` (`api_key_hash`, `created_at`),
    KEY `idx_rerank_tenant_time` (`tenant_id`, `created_at`),
    KEY `idx_rerank_error_time` (`error_code_normalized`, `created_at`),
    KEY `idx_rerank_window` (`request_window`, `provider`, `model`),
    KEY `idx_rerank_success` (`success`, `fallback`, `created_at`),
    KEY `idx_rerank_latency` (`latency_ms`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG external rerank call observability log';


-- ============================================================================
-- Source: rag_multi_tenant_isolation_upgrade.sql
-- ============================================================================

-- Multi-tenant isolation framework upgrade.
-- Run after rag_schema.sql on existing deployments. Existing unique-key changes may need manual review
-- if duplicate cross-tenant data already exists.

CREATE TABLE IF NOT EXISTS `sys_tenant`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_code`        VARCHAR(64)     NOT NULL,
    `tenant_name`        VARCHAR(128)    NOT NULL,
    `external_id`        VARCHAR(128)             DEFAULT NULL,
    `status`             TINYINT         NOT NULL DEFAULT 1,
    `created_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_tenant_code` (`tenant_code`),
    KEY `idx_sys_tenant_status` (`status`, `is_deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Tenant registry';

CREATE TABLE IF NOT EXISTS `sys_platform_admin`
(
    `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `singleton_key`        TINYINT         NOT NULL DEFAULT 1,
    `admin_username`       VARCHAR(128)    NOT NULL,
    `display_name`         VARCHAR(128)             DEFAULT NULL,
    `email`                VARCHAR(255)             DEFAULT NULL,
    `password_hash`        VARCHAR(255)             DEFAULT NULL COMMENT 'BCrypt password hash',
    `password_updated_at`  DATETIME(3)              DEFAULT NULL COMMENT 'Last password update time',
    `must_change_password` TINYINT         NOT NULL DEFAULT 0 COMMENT 'Whether admin must change password after login',
    `status`               TINYINT         NOT NULL DEFAULT 1,
    `created_at`           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`           TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_platform_admin_singleton` (`singleton_key`),
    UNIQUE KEY `uk_sys_platform_admin_username` (`admin_username`),
    CONSTRAINT `chk_sys_platform_admin_singleton` CHECK (`singleton_key` = 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Platform super administrator';

CREATE TABLE IF NOT EXISTS `sys_user`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `external_user_id` VARCHAR(128)    NOT NULL,
    `username`     VARCHAR(128)             DEFAULT NULL,
    `display_name` VARCHAR(128)             DEFAULT NULL,
    `email`        VARCHAR(255)             DEFAULT NULL,
    `password_hash` VARCHAR(255)             DEFAULT NULL COMMENT 'BCrypt password hash',
    `password_updated_at` DATETIME(3)        DEFAULT NULL COMMENT 'Last password update time',
    `must_change_password` TINYINT  NOT NULL DEFAULT 0 COMMENT 'Whether user must change password after login',
    `status`       TINYINT         NOT NULL DEFAULT 1,
    `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`   TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_tenant_user` (`tenant_id`, `external_user_id`),
    KEY `idx_sys_user_email` (`tenant_id`, `email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Tenant user';

CREATE TABLE IF NOT EXISTS `sys_role`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `role_code`   VARCHAR(64)     NOT NULL,
    `role_name`   VARCHAR(128)    NOT NULL,
    `role_scope`  VARCHAR(64)              DEFAULT NULL,
    `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`  TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_tenant_code` (`tenant_id`, `role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Tenant role';

CREATE TABLE IF NOT EXISTS `sys_user_role`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `user_id`    VARCHAR(128)    NOT NULL,
    `role_id`    BIGINT UNSIGNED          DEFAULT NULL,
    `role_code`  VARCHAR(64)     NOT NULL,
    `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `is_deleted` TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_role` (`tenant_id`, `user_id`, `role_code`),
    KEY `idx_sys_user_role_user` (`tenant_id`, `user_id`, `is_deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Tenant user role';

CREATE TABLE IF NOT EXISTS `rag_workspace`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `workspace_code` VARCHAR(64)     NOT NULL,
    `workspace_name` VARCHAR(128)    NOT NULL,
    `description`    VARCHAR(1000)            DEFAULT NULL,
    `status`         TINYINT         NOT NULL DEFAULT 1,
    `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workspace_tenant_code` (`tenant_id`, `workspace_code`),
    KEY `idx_workspace_tenant` (`tenant_id`, `is_deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='RAG workspace';

CREATE TABLE IF NOT EXISTS `rag_workspace_member`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `workspace_id`   BIGINT UNSIGNED NOT NULL,
    `user_id`        VARCHAR(128)    NOT NULL,
    `member_role`    VARCHAR(32)     NOT NULL DEFAULT 'READER',
    `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workspace_member` (`tenant_id`, `workspace_id`, `user_id`),
    KEY `idx_workspace_member_user` (`tenant_id`, `user_id`, `is_deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='RAG workspace member';

CREATE TABLE IF NOT EXISTS `rag_knowledge_base_member`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id` BIGINT UNSIGNED NOT NULL,
    `user_id`           VARCHAR(128)    NOT NULL,
    `member_role`       VARCHAR(32)     NOT NULL DEFAULT 'READER',
    `permission_tags`   VARCHAR(1000)            DEFAULT NULL,
    `created_at`        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`        TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_member` (`tenant_id`, `knowledge_base_id`, `user_id`),
    KEY `idx_kb_member_user` (`tenant_id`, `user_id`, `is_deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Knowledge base member';

CREATE TABLE IF NOT EXISTS `rag_resource_permission_tag`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`         BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id` BIGINT UNSIGNED          DEFAULT NULL,
    `resource_type`     VARCHAR(64)     NOT NULL,
    `resource_id`       VARCHAR(128)    NOT NULL,
    `permission_tag`    VARCHAR(128)    NOT NULL,
    `created_at`        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`        TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_resource_permission_tag` (`tenant_id`, `resource_type`, `resource_id`, `permission_tag`),
    KEY `idx_resource_permission_kb` (`tenant_id`, `knowledge_base_id`, `permission_tag`, `is_deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Resource permission tag';

CREATE TABLE IF NOT EXISTS `sys_admin_impersonation_session`
(
    `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `session_no`             VARCHAR(64)     NOT NULL,
    `operator_user_id`       VARCHAR(128)    NOT NULL,
    `operator_tenant_id`     BIGINT UNSIGNED NOT NULL,
    `target_tenant_id`       BIGINT UNSIGNED NOT NULL,
    `impersonation_reason`   VARCHAR(1000)   NOT NULL,
    `request_id`             VARCHAR(128)             DEFAULT NULL,
    `source_ip`              VARCHAR(128)             DEFAULT NULL,
    `started_at`             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `expires_at`             DATETIME(3)     NOT NULL,
    `revoked_at`             DATETIME(3)              DEFAULT NULL,
    `created_at`             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_impersonation_session_no` (`session_no`),
    KEY `idx_impersonation_operator` (`operator_user_id`, `started_at`),
    KEY `idx_impersonation_target` (`target_tenant_id`, `started_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Admin impersonation session';

CREATE TABLE IF NOT EXISTS `sys_operation_audit_log`
(
    `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `operator_user_id`     VARCHAR(128)    NOT NULL,
    `operator_tenant_id`   BIGINT UNSIGNED          DEFAULT NULL,
    `target_tenant_id`     BIGINT UNSIGNED          DEFAULT NULL,
    `impersonation_reason` VARCHAR(1000)            DEFAULT NULL,
    `request_id`           VARCHAR(128)             DEFAULT NULL,
    `source_ip`            VARCHAR(128)             DEFAULT NULL,
    `operation`            VARCHAR(128)    NOT NULL,
    `resource_type`        VARCHAR(128)    NOT NULL,
    `resource_id`          VARCHAR(128)             DEFAULT NULL,
    `result`               VARCHAR(32)     NOT NULL DEFAULT 'SUCCESS',
    `detail_json`          JSON                     DEFAULT NULL,
    `created_at`           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_audit_target_time` (`target_tenant_id`, `created_at`),
    KEY `idx_audit_operator_time` (`operator_user_id`, `created_at`),
    KEY `idx_audit_operation_time` (`operation`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Tenant operation audit log';

CREATE TABLE IF NOT EXISTS `rag_tenant_model_config`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`          BIGINT UNSIGNED NOT NULL,
    `provider`           VARCHAR(64)     NOT NULL,
    `model_type`         VARCHAR(32)     NOT NULL,
    `model_name`         VARCHAR(128)    NOT NULL,
    `base_url`           VARCHAR(512)             DEFAULT NULL,
    `api_key_secret_ref` VARCHAR(2048)            DEFAULT NULL,
    `temperature`        DECIMAL(3,2)             DEFAULT 0.20 COMMENT 'LLM temperature (0-2)',
    `dimension`          INT                      DEFAULT 1536 COMMENT 'Embedding dimension',
    `image_size`         VARCHAR(64)              DEFAULT NULL COMMENT 'Text-to-image output size',
    `image_quality`      VARCHAR(64)              DEFAULT NULL COMMENT 'Text-to-image quality option',
    `poll_interval_millis` INT                    DEFAULT NULL COMMENT 'Text-to-image polling interval milliseconds',
    `rate_limit_qps`     INT                      DEFAULT NULL COMMENT 'Rate limit (requests per second)',
    `monthly_budget_cents` BIGINT                 DEFAULT NULL COMMENT 'Monthly budget in cents',
    `timeout_seconds`    INT                      DEFAULT NULL COMMENT 'Request timeout seconds',
    `max_retries`        INT                      DEFAULT NULL COMMENT 'Maximum retry count',
    `max_tokens`         INT                      DEFAULT NULL COMMENT 'Maximum output tokens',
    `frequency_penalty`  DECIMAL(4,2)             DEFAULT NULL COMMENT 'Frequency penalty',
    `presence_penalty`   DECIMAL(4,2)             DEFAULT NULL COMMENT 'Presence penalty',
    `top_p`              DECIMAL(4,2)             DEFAULT NULL COMMENT 'Top-p sampling value',
    `enabled`            TINYINT         NOT NULL DEFAULT 1,
    `created_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_model_config` (`tenant_id`, `model_type`, `provider`, `model_name`),
    KEY `idx_tenant_model_enabled` (`tenant_id`, `model_type`, `enabled`, `is_deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Tenant model configuration';

-- Enhance rag_tenant_model_config with model parameters
-- Note: MySQL does not support ADD COLUMN IF NOT EXISTS; run conditionally
ALTER TABLE `rag_tenant_model_config`
    ADD COLUMN `temperature` DECIMAL(3,2) DEFAULT 0.20 COMMENT 'LLM temperature (0-2)',
    ADD COLUMN `dimension` INT DEFAULT 1536 COMMENT 'Embedding dimension',
    ADD COLUMN `rate_limit_qps` INT DEFAULT NULL COMMENT 'Rate limit (requests per second)',
    ADD COLUMN `monthly_budget_cents` BIGINT DEFAULT NULL COMMENT 'Monthly budget in cents';

-- Seed default model configs for tenant 0 (global)
INSERT INTO `rag_tenant_model_config` (`tenant_id`, `provider`, `model_type`, `model_name`, `base_url`, `api_key_secret_ref`, `temperature`, `dimension`, `enabled`)
SELECT 0, 'openai-compatible', 'LLM', 'qwen-plus',
       'https://dashscope.aliyuncs.com/compatible-mode/v1', NULL, 0.20, NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM `rag_tenant_model_config` WHERE `tenant_id` = 0 AND `model_type` = 'LLM' AND `is_deleted` = 0);

INSERT INTO `rag_tenant_model_config` (`tenant_id`, `provider`, `model_type`, `model_name`, `base_url`, `api_key_secret_ref`, `temperature`, `dimension`, `enabled`)
SELECT 0, 'openai-compatible', 'EMBEDDING', 'text-embedding-v4',
       'https://dashscope.aliyuncs.com/compatible-mode/v1', NULL, NULL, 1024, 1
WHERE NOT EXISTS (SELECT 1 FROM `rag_tenant_model_config` WHERE `tenant_id` = 0 AND `model_type` = 'EMBEDDING' AND `is_deleted` = 0);

INSERT INTO `rag_tenant_model_config` (`tenant_id`, `provider`, `model_type`, `model_name`, `base_url`, `api_key_secret_ref`, `temperature`, `dimension`, `image_size`, `image_quality`, `poll_interval_millis`, `timeout_seconds`, `enabled`)
SELECT 0, 'openai-compatible', 'IMAGE', 'wanx-v1',
       'https://dashscope.aliyuncs.com/api/v1', NULL, NULL, NULL, '1024x1024', 'standard', 2000, 60, 1
WHERE NOT EXISTS (SELECT 1 FROM `rag_tenant_model_config` WHERE `tenant_id` = 0 AND `model_type` = 'IMAGE' AND `is_deleted` = 0);

CREATE TABLE IF NOT EXISTS `rag_tenant_quota`
(
    `id`                                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`                           BIGINT UNSIGNED NOT NULL,
    `max_documents`                       BIGINT UNSIGNED          DEFAULT NULL,
    `max_storage_bytes`                   BIGINT UNSIGNED          DEFAULT NULL,
    `max_file_bytes`                      BIGINT UNSIGNED          DEFAULT NULL,
    `daily_ocr_limit`                     BIGINT UNSIGNED          DEFAULT NULL,
    `daily_embedding_tokens`              BIGINT UNSIGNED          DEFAULT NULL,
    `max_concurrent_ingestion_tasks`      BIGINT UNSIGNED          DEFAULT NULL,
    `daily_query_limit`                   BIGINT UNSIGNED          DEFAULT NULL,
    `monthly_budget_cents`                BIGINT UNSIGNED          DEFAULT NULL,
    `enabled`                             TINYINT         NOT NULL DEFAULT 1,
    `created_at`                          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`                          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_quota` (`tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Tenant quota';

CREATE TABLE IF NOT EXISTS `rag_tenant_usage_daily`
(
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`               BIGINT UNSIGNED NOT NULL,
    `usage_date`              DATE            NOT NULL,
    `document_count`          BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `storage_bytes`           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `ocr_count`               BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `embedding_tokens`        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `vector_count`            BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `query_count`             BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `llm_tokens`              BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `estimated_cost_cents`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_usage_day` (`tenant_id`, `usage_date`),
    KEY `idx_tenant_usage_date` (`usage_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Tenant daily usage';

CREATE TABLE IF NOT EXISTS `tenant_data_deletion_task`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_no`          VARCHAR(64)     NOT NULL,
    `tenant_id`        BIGINT UNSIGNED NOT NULL,
    `requested_by`     VARCHAR(128)    NOT NULL,
    `reason`           VARCHAR(1000)            DEFAULT NULL,
    `task_status`      VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    `started_at`       DATETIME(3)              DEFAULT NULL,
    `finished_at`      DATETIME(3)              DEFAULT NULL,
    `error_message`    VARCHAR(2000)            DEFAULT NULL,
    `created_at`       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_deletion_task_no` (`task_no`),
    KEY `idx_tenant_deletion_task` (`tenant_id`, `task_status`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Tenant data deletion task';

CREATE TABLE IF NOT EXISTS `tenant_data_deletion_stage`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`       BIGINT UNSIGNED NOT NULL,
    `stage_code`    VARCHAR(64)     NOT NULL,
    `stage_status`  VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    `deleted_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `error_message` VARCHAR(2000)            DEFAULT NULL,
    `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_deletion_stage` (`task_id`, `stage_code`),
    KEY `idx_deletion_stage_status` (`stage_status`, `updated_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Tenant data deletion stage';

DROP PROCEDURE IF EXISTS `rag_replace_unique_index_if_needed`;

DELIMITER //

CREATE PROCEDURE `rag_replace_unique_index_if_needed`(
    IN table_name_value VARCHAR(128),
    IN old_index_name_value VARCHAR(128),
    IN new_index_name_value VARCHAR(128),
    IN new_index_columns_value VARCHAR(512)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND index_name = old_index_name_value
    ) THEN
        SET @drop_sql = CONCAT('ALTER TABLE `', table_name_value, '` DROP INDEX `', old_index_name_value, '`');
        PREPARE drop_stmt FROM @drop_sql;
        EXECUTE drop_stmt;
        DEALLOCATE PREPARE drop_stmt;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND index_name = new_index_name_value
    ) THEN
        SET @add_sql = CONCAT('ALTER TABLE `', table_name_value, '` ADD UNIQUE KEY `', new_index_name_value, '` (', new_index_columns_value, ')');
        PREPARE add_stmt FROM @add_sql;
        EXECUTE add_stmt;
        DEALLOCATE PREPARE add_stmt;
    END IF;
END//

DELIMITER ;

CALL `rag_replace_unique_index_if_needed`('rag_document', 'uk_document_uid', 'uk_tenant_document_uid', '`tenant_id`, `document_uid`');
CALL `rag_replace_unique_index_if_needed`('rag_document_version', 'uk_document_version_uid', 'uk_tenant_document_version_uid', '`tenant_id`, `version_uid`');
CALL `rag_replace_unique_index_if_needed`('rag_document_chunk', 'uk_chunk_uid_version', 'uk_tenant_chunk_uid_version', '`tenant_id`, `chunk_uid`, `chunk_version`');
CALL `rag_replace_unique_index_if_needed`('rag_ingestion_task', 'uk_task_no', 'uk_tenant_task_no', '`tenant_id`, `task_no`');
CALL `rag_replace_unique_index_if_needed`('rag_ingestion_task', 'uk_idempotency_key', 'uk_tenant_idempotency_key', '`tenant_id`, `idempotency_key`');
CALL `rag_replace_unique_index_if_needed`('rag_image_asset', 'uk_image_asset_image_id', 'uk_tenant_image_id', '`tenant_id`, `image_id`');
CALL `rag_replace_unique_index_if_needed`('rag_retrieval_eval_case', 'uk_eval_case_id', 'uk_tenant_eval_case_id', '`tenant_id`, `case_id`');
CALL `rag_replace_unique_index_if_needed`('rag_retrieval_eval_run', 'uk_eval_run_no', 'uk_tenant_eval_run_no', '`tenant_id`, `run_no`');

DROP PROCEDURE `rag_replace_unique_index_if_needed`;


-- ============================================================================
-- Source: rag_chunk_authority_upgrade.sql
-- ============================================================================

-- Upgrade rag_document_chunk for MySQL-authoritative chunk versions.
-- Review before running on an existing database and back up data first.

ALTER TABLE `rag_document_chunk`
    ADD COLUMN `source_document_id` VARCHAR(128) DEFAULT NULL COMMENT 'Logical/external document ID used by chunk APIs' AFTER `document_id`,
    ADD COLUMN `chunk_version` INT UNSIGNED NOT NULL DEFAULT 1 AFTER `chunk_uid`,
    ADD COLUMN `chunk_status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, SUPERSEDED, DISABLED, DELETED' AFTER `chunk_version`,
    ADD COLUMN `is_current` TINYINT NOT NULL DEFAULT 1 AFTER `chunk_status`,
    ADD COLUMN `parent_chunk_uid` VARCHAR(128) DEFAULT NULL AFTER `parent_chunk_id`,
    ADD COLUMN `source` VARCHAR(64) DEFAULT NULL AFTER `parent_chunk_uid`,
    ADD COLUMN `file_name` VARCHAR(255) DEFAULT NULL AFTER `source`,
    ADD COLUMN `content_type` VARCHAR(32) NOT NULL DEFAULT 'text' AFTER `file_name`,
    ADD COLUMN `image_url` VARCHAR(1000) DEFAULT NULL AFTER `section_path`,
    ADD COLUMN `image_caption` VARCHAR(1000) DEFAULT NULL AFTER `image_url`,
    ADD COLUMN `image_number` VARCHAR(64) DEFAULT NULL AFTER `image_caption`,
    ADD COLUMN `permission_tags` VARCHAR(1000) DEFAULT NULL AFTER `image_number`,
    ADD COLUMN `tenant_external_id` VARCHAR(128) DEFAULT NULL AFTER `permission_tags`,
    ADD COLUMN `milvus_alias` VARCHAR(128) DEFAULT NULL AFTER `vector_collection`,
    ADD COLUMN `text_vector_ids` JSON DEFAULT NULL AFTER `vector_id`,
    ADD COLUMN `image_vector_ids` JSON DEFAULT NULL AFTER `text_vector_ids`;

ALTER TABLE `rag_document_chunk`
    DROP INDEX `uk_chunk_uid`,
    DROP INDEX `uk_document_chunk_index`,
    ADD UNIQUE KEY `uk_chunk_uid_version` (`chunk_uid`, `chunk_version`),
    ADD KEY `idx_document_chunk_index` (`document_id`, `document_version_id`, `chunk_index`),
    ADD KEY `idx_chunk_current` (`chunk_uid`, `is_current`, `chunk_status`),
    ADD KEY `idx_source_document` (`source_document_id`, `is_current`, `is_deleted`),
    ADD KEY `idx_store_current` (`milvus_alias`, `vector_collection`, `is_current`, `is_deleted`);

UPDATE `rag_document_chunk`
SET `source_document_id` = COALESCE(`source_document_id`, CAST(`document_id` AS CHAR)),
    `chunk_version` = COALESCE(`chunk_version`, 1),
    `chunk_status` = COALESCE(`chunk_status`, 'ACTIVE'),
    `is_current` = COALESCE(`is_current`, 1),
    `content_type` = COALESCE(`content_type`, 'text'),
    `text_vector_ids` = IF(`text_vector_ids` IS NULL AND `vector_id` IS NOT NULL, JSON_ARRAY(`vector_id`), `text_vector_ids`);


-- ============================================================================
-- Source: rag_document_lifecycle_upgrade.sql
-- ============================================================================

-- Document lifecycle enhancement fields.
-- Run once on databases that already have rag_document_version before this upgrade.

ALTER TABLE `rag_document_version`
    ADD COLUMN `version_note` VARCHAR(2000) DEFAULT NULL COMMENT 'Version note' AFTER `is_current`,
    ADD COLUMN `approval_status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, PENDING_REVIEW, APPROVED, REJECTED, PUBLISHED' AFTER `version_note`,
    ADD COLUMN `approval_comment` VARCHAR(2000) DEFAULT NULL COMMENT 'Approval comment' AFTER `approval_status`,
    ADD COLUMN `approved_by` VARCHAR(128) DEFAULT NULL COMMENT 'Reviewer or operator' AFTER `approval_comment`,
    ADD COLUMN `approved_at` DATETIME(3) DEFAULT NULL COMMENT 'Review timestamp' AFTER `approved_by`,
    ADD COLUMN `published_at` DATETIME(3) DEFAULT NULL COMMENT 'Publish timestamp' AFTER `approved_at`;

CREATE INDEX `idx_version_approval`
    ON `rag_document_version` (`approval_status`, `updated_at`);


-- ============================================================================
-- Source: rag_document_version_upgrade.sql
-- ============================================================================

-- Add document version table and backfill one version per existing document.
-- Review before running on an existing database and back up data first.

CREATE TABLE IF NOT EXISTS `rag_document_version`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Document version ID',
    `tenant_id`           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id`   BIGINT UNSIGNED NOT NULL,
    `document_id`         BIGINT UNSIGNED NOT NULL,
    `version_no`          INT UNSIGNED    NOT NULL,
    `version_uid`         VARCHAR(64)     NOT NULL COMMENT 'Stable document version UID',
    `document_name`       VARCHAR(255)    NOT NULL,
    `source_type`         TINYINT         NOT NULL DEFAULT 1,
    `source_uri`          VARCHAR(1000)            DEFAULT NULL,
    `object_key`          VARCHAR(512)             DEFAULT NULL,
    `original_filename`   VARCHAR(255)             DEFAULT NULL,
    `file_extension`      VARCHAR(32)              DEFAULT NULL,
    `mime_type`           VARCHAR(128)             DEFAULT NULL,
    `file_size`           BIGINT UNSIGNED          DEFAULT 0,
    `file_hash`           CHAR(64)                 DEFAULT NULL,
    `page_count`          INT UNSIGNED             DEFAULT NULL,
    `chunk_count`         INT UNSIGNED    NOT NULL DEFAULT 0,
    `character_count`     BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `token_count`         BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `parse_status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
    `chunk_status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
    `embedding_status`    TINYINT         NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
    `version_status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '0 processing, 1 available, 2 failed, 3 disabled, 4 deleted',
    `is_current`          TINYINT         NOT NULL DEFAULT 0,
    `version_note`        VARCHAR(2000)            DEFAULT NULL COMMENT 'Version note',
    `approval_status`     VARCHAR(32)     NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, PENDING_REVIEW, APPROVED, REJECTED, PUBLISHED',
    `approval_comment`    VARCHAR(2000)            DEFAULT NULL COMMENT 'Approval comment',
    `approved_by`         VARCHAR(128)             DEFAULT NULL COMMENT 'Reviewer or operator',
    `approved_at`         DATETIME(3)              DEFAULT NULL COMMENT 'Review timestamp',
    `published_at`        DATETIME(3)              DEFAULT NULL COMMENT 'Publish timestamp',
    `error_code`          VARCHAR(64)              DEFAULT NULL,
    `error_message`       VARCHAR(2000)            DEFAULT NULL,
    `metadata_json`       JSON                     DEFAULT NULL,
    `created_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`          TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_document_version_no` (`document_id`, `version_no`),
    UNIQUE KEY `uk_document_version_uid` (`version_uid`),
    KEY `idx_document_current` (`document_id`, `is_current`, `is_deleted`),
    KEY `idx_kb_version_status` (`tenant_id`, `knowledge_base_id`, `version_status`, `is_deleted`),
    KEY `idx_version_approval` (`approval_status`, `updated_at`),
    KEY `idx_version_hash` (`knowledge_base_id`, `file_hash`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG document version';

INSERT INTO `rag_document_version`
(`tenant_id`, `knowledge_base_id`, `document_id`, `version_no`, `version_uid`,
 `document_name`, `source_type`, `source_uri`, `object_key`, `original_filename`,
 `file_extension`, `mime_type`, `file_size`, `file_hash`, `page_count`,
 `chunk_count`, `character_count`, `token_count`, `parse_status`, `chunk_status`,
 `embedding_status`, `version_status`, `is_current`, `error_code`, `error_message`,
 `metadata_json`, `created_at`, `updated_at`, `is_deleted`)
SELECT d.`tenant_id`,
       d.`knowledge_base_id`,
       d.`id`,
       COALESCE(d.`current_version_no`, 1),
       CONCAT('ver_', REPLACE(UUID(), '-', '')),
       d.`document_name`,
       d.`source_type`,
       d.`source_uri`,
       d.`object_key`,
       d.`original_filename`,
       d.`file_extension`,
       d.`mime_type`,
       d.`file_size`,
       d.`file_hash`,
       d.`page_count`,
       d.`chunk_count`,
       d.`character_count`,
       d.`token_count`,
       d.`parse_status`,
       d.`chunk_status`,
       d.`embedding_status`,
       CASE WHEN d.`document_status` = 1 THEN 1
            WHEN d.`document_status` = 2 THEN 2
            WHEN d.`document_status` = 3 THEN 3
            ELSE 0 END,
       1,
       d.`error_code`,
       d.`error_message`,
       d.`metadata_json`,
       d.`created_at`,
       d.`updated_at`,
       d.`is_deleted`
FROM `rag_document` d
WHERE NOT EXISTS (
    SELECT 1
    FROM `rag_document_version` v
    WHERE v.`document_id` = d.`id`
      AND v.`version_no` = COALESCE(d.`current_version_no`, 1)
);

UPDATE `rag_document` d
JOIN `rag_document_version` v
  ON v.`document_id` = d.`id`
 AND v.`version_no` = COALESCE(d.`current_version_no`, 1)
SET d.`current_version_id` = v.`id`;


-- ============================================================================
-- Source: rag_image_asset_upgrade.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS `rag_image_asset`
(
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Image asset ID',
    `tenant_id`             BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `document_id`           BIGINT UNSIGNED          DEFAULT NULL,
    `document_version_id`   BIGINT UNSIGNED          DEFAULT NULL,
    `source_document_id`    VARCHAR(128)             DEFAULT NULL COMMENT 'Logical document UID',
    `image_id`              VARCHAR(128)    NOT NULL COMMENT 'Stable extracted image ID',
    `chunk_uid`             VARCHAR(128)             DEFAULT NULL COMMENT 'Chunk UID generated from this image',
    `content_type`          VARCHAR(32)     NOT NULL DEFAULT 'image',
    `asset_path`            VARCHAR(1000)            DEFAULT NULL,
    `image_url`             VARCHAR(1000)            DEFAULT NULL,
    `page_no`               INT UNSIGNED             DEFAULT NULL,
    `coordinate_json`       JSON                     DEFAULT NULL,
    `section_title`         VARCHAR(500)             DEFAULT NULL,
    `image_caption`         VARCHAR(1000)            DEFAULT NULL,
    `image_number`          VARCHAR(64)              DEFAULT NULL,
    `ocr_text`              MEDIUMTEXT               DEFAULT NULL,
    `visual_status`         VARCHAR(32)     NOT NULL DEFAULT 'EMPTY' COMMENT 'SUCCESS, INVALID, FAILED, EMPTY',
    `visual_schema_valid`   TINYINT         NOT NULL DEFAULT 0,
    `visual_confidence`     DECIMAL(6, 5)            DEFAULT NULL,
    `visual_json`           JSON                     DEFAULT NULL,
    `text_vector_ids`       JSON                     DEFAULT NULL,
    `image_vector_ids`      JSON                     DEFAULT NULL,
    `created_at`            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `is_deleted`            TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_image_asset_image_id` (`image_id`),
    KEY `idx_image_asset_kb_doc` (`tenant_id`, `knowledge_base_id`, `source_document_id`, `is_deleted`),
    KEY `idx_image_asset_chunk` (`chunk_uid`),
    KEY `idx_image_asset_status` (`visual_status`, `visual_confidence`),
    KEY `idx_image_asset_updated` (`updated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG image asset and multimodal governance metadata';


-- ============================================================================
-- Source: rag_ingestion_progress_upgrade.sql
-- ============================================================================

-- RAG ingestion task progress upgrade.
-- Apply this script to existing databases before deploying the task-process implementation.

ALTER TABLE `rag_ingestion_task`
    ADD COLUMN `current_stage` VARCHAR(64) DEFAULT NULL AFTER `progress`,
    ADD COLUMN `stage_progress` INT UNSIGNED NOT NULL DEFAULT 0 AFTER `current_stage`,
    ADD COLUMN `cancel_requested` TINYINT NOT NULL DEFAULT 0 AFTER `next_retry_at`,
    ADD COLUMN `cancel_requested_at` DATETIME(3) DEFAULT NULL AFTER `cancel_requested`,
    ADD COLUMN `cancel_requested_by` VARCHAR(128) DEFAULT NULL AFTER `cancel_requested_at`,
    ADD COLUMN `partial_success` TINYINT NOT NULL DEFAULT 0 AFTER `cancel_requested_by`,
    ADD COLUMN `last_event_id` BIGINT UNSIGNED DEFAULT NULL AFTER `partial_success`,
    ADD COLUMN `heartbeat_at` DATETIME(3) DEFAULT NULL AFTER `last_event_id`,
    ADD KEY `idx_task_stage` (`current_stage`, `heartbeat_at`);

CREATE TABLE IF NOT EXISTS `rag_ingestion_task_stage`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`       BIGINT UNSIGNED NOT NULL,
    `stage_code`    VARCHAR(64)     NOT NULL,
    `stage_name`    VARCHAR(128)    NOT NULL,
    `stage_order`   INT UNSIGNED    NOT NULL DEFAULT 0,
    `stage_weight`  INT UNSIGNED    NOT NULL DEFAULT 0,
    `stage_status`  VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    `progress`      INT UNSIGNED    NOT NULL DEFAULT 0,
    `total_count`   INT UNSIGNED    NOT NULL DEFAULT 0,
    `success_count` INT UNSIGNED    NOT NULL DEFAULT 0,
    `failed_count`  INT UNSIGNED    NOT NULL DEFAULT 0,
    `started_at`    DATETIME(3)              DEFAULT NULL,
    `finished_at`   DATETIME(3)              DEFAULT NULL,
    `error_code`    VARCHAR(128)             DEFAULT NULL,
    `error_message` VARCHAR(2000)            DEFAULT NULL,
    `metadata_json` JSON                     DEFAULT NULL,
    `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_stage` (`task_id`, `stage_code`),
    KEY `idx_stage_status` (`stage_status`, `updated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG ingestion task stage progress';

CREATE TABLE IF NOT EXISTS `rag_ingestion_task_shard`
(
    `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`             BIGINT UNSIGNED NOT NULL,
    `stage_code`          VARCHAR(64)              DEFAULT NULL,
    `document_id`         BIGINT UNSIGNED          DEFAULT NULL,
    `document_version_id` BIGINT UNSIGNED          DEFAULT NULL,
    `shard_key`           VARCHAR(128)    NOT NULL,
    `shard_type`          VARCHAR(32)     NOT NULL,
    `shard_index`         INT UNSIGNED             DEFAULT NULL,
    `shard_status`        VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    `retry_count`         INT UNSIGNED    NOT NULL DEFAULT 0,
    `max_retry_count`     INT UNSIGNED    NOT NULL DEFAULT 3,
    `next_retry_at`       DATETIME(3)              DEFAULT NULL,
    `error_code`          VARCHAR(128)             DEFAULT NULL,
    `error_message`       VARCHAR(2000)            DEFAULT NULL,
    `input_hash`          CHAR(64)                 DEFAULT NULL,
    `output_ref`          VARCHAR(512)             DEFAULT NULL,
    `metadata_json`       JSON                     DEFAULT NULL,
    `created_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_shard` (`task_id`, `shard_key`, `shard_type`),
    KEY `idx_shard_retry` (`shard_status`, `next_retry_at`),
    KEY `idx_shard_document` (`document_id`, `document_version_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG ingestion task shard progress';

CREATE TABLE IF NOT EXISTS `rag_ingestion_task_event`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`        BIGINT UNSIGNED NOT NULL,
    `event_type`     VARCHAR(64)     NOT NULL,
    `stage_code`     VARCHAR(64)              DEFAULT NULL,
    `shard_key`      VARCHAR(128)             DEFAULT NULL,
    `progress`       INT UNSIGNED             DEFAULT NULL,
    `stage_progress` INT UNSIGNED             DEFAULT NULL,
    `message`        VARCHAR(1000)            DEFAULT NULL,
    `payload_json`   JSON                     DEFAULT NULL,
    `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_event_task` (`task_id`, `id`),
    KEY `idx_event_created` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG ingestion task event stream';


-- ============================================================================
-- Source: rag_multimodal_native_upgrade.sql
-- ============================================================================

-- Native multimodal RAG upgrade.
-- MySQL stores auditable metadata and vector IDs only. The native Milvus
-- collection with text_vector and image_vector fields is created by:
--   POST /api/rag/multimodal/collections/ensure

ALTER TABLE `rag_query_log`
    ADD COLUMN `multimodal_trace_json` JSON DEFAULT NULL AFTER `permission_tags_json`;

ALTER TABLE `rag_query_hit`
    ADD COLUMN `modality` VARCHAR(32) DEFAULT NULL AFTER `content_type`,
    ADD COLUMN `retrieval_source` VARCHAR(64) DEFAULT NULL AFTER `modality`,
    ADD COLUMN `image_asset_id` BIGINT UNSIGNED DEFAULT NULL AFTER `retrieval_source`,
    ADD COLUMN `fusion_score` DECIMAL(12, 10) DEFAULT NULL AFTER `image_asset_id`,
    ADD INDEX `idx_query_hit_multimodal` (`modality`, `retrieval_source`, `image_asset_id`);

ALTER TABLE `rag_image_asset`
    ADD COLUMN `ocr_status` VARCHAR(32) DEFAULT NULL AFTER `ocr_text`,
    ADD COLUMN `ocr_confidence` DECIMAL(6, 5) DEFAULT NULL AFTER `ocr_status`,
    ADD COLUMN `ocr_provider` VARCHAR(64) DEFAULT NULL AFTER `ocr_confidence`,
    ADD COLUMN `ocr_model` VARCHAR(128) DEFAULT NULL AFTER `ocr_provider`,
    ADD COLUMN `ocr_error_message` VARCHAR(2000) DEFAULT NULL AFTER `ocr_model`,
    ADD COLUMN `visual_schema_errors` JSON DEFAULT NULL AFTER `visual_json`,
    ADD COLUMN `image_embedding_status` VARCHAR(32) DEFAULT NULL AFTER `image_vector_ids`,
    ADD COLUMN `image_embedding_model` VARCHAR(128) DEFAULT NULL AFTER `image_embedding_status`,
    ADD COLUMN `image_embedding_dimension` INT UNSIGNED DEFAULT NULL AFTER `image_embedding_model`,
    ADD COLUMN `image_embedding_error_message` VARCHAR(2000) DEFAULT NULL AFTER `image_embedding_dimension`,
    ADD COLUMN `image_embedding_updated_at` DATETIME(3) DEFAULT NULL AFTER `image_embedding_error_message`,
    ADD COLUMN `review_status` VARCHAR(32) NOT NULL DEFAULT 'EMPTY' AFTER `image_embedding_updated_at`,
    ADD COLUMN `review_comment` VARCHAR(1000) DEFAULT NULL AFTER `review_status`,
    ADD COLUMN `reviewed_by` VARCHAR(128) DEFAULT NULL AFTER `review_comment`,
    ADD COLUMN `reviewed_at` DATETIME(3) DEFAULT NULL AFTER `reviewed_by`,
    ADD COLUMN `review_updated_visual_json` JSON DEFAULT NULL AFTER `reviewed_at`,
    ADD COLUMN `review_updated_ocr_text` MEDIUMTEXT DEFAULT NULL AFTER `review_updated_visual_json`,
    ADD INDEX `idx_image_asset_review` (`review_status`, `updated_at`),
    ADD INDEX `idx_image_asset_ocr` (`ocr_status`, `ocr_confidence`),
    ADD INDEX `idx_image_asset_embedding` (`image_embedding_status`, `image_embedding_updated_at`);


-- ============================================================================
-- Source: rag_query_audit_upgrade.sql
-- ============================================================================

CREATE TABLE IF NOT EXISTS `rag_query_log`
(
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Query log ID',
    `tenant_id`               BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `trace_id`                VARCHAR(64)              DEFAULT NULL,
    `conversation_id`         VARCHAR(128)             DEFAULT NULL,
    `query_type`              VARCHAR(16)     NOT NULL COMMENT 'QUERY or SEARCH',
    `query_text`              TEXT            NOT NULL,
    `retrieval_mode`          VARCHAR(32)              DEFAULT NULL,
    `knowledge_base_ids_json` JSON                     DEFAULT NULL,
    `top_k`                   INT UNSIGNED             DEFAULT NULL,
    `min_score`               DECIMAL(8, 6)            DEFAULT NULL,
    `content_types_json`      JSON                     DEFAULT NULL,
    `permission_tags_json`    JSON                     DEFAULT NULL,
    `prompt_text`             MEDIUMTEXT               DEFAULT NULL,
    `answer_text`             MEDIUMTEXT               DEFAULT NULL,
    `knowledge_hit`           TINYINT         NOT NULL DEFAULT 0,
    `hit_count`               INT UNSIGNED    NOT NULL DEFAULT 0,
    `prompt_tokens`           INT UNSIGNED             DEFAULT NULL,
    `completion_tokens`       INT UNSIGNED             DEFAULT NULL,
    `total_tokens`            INT UNSIGNED             DEFAULT NULL,
    `latency_ms`              BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `status`                  VARCHAR(16)     NOT NULL DEFAULT 'SUCCESS',
    `error_code`              VARCHAR(64)              DEFAULT NULL,
    `error_message`           VARCHAR(2000)            DEFAULT NULL,
    `created_at`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`              DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_query_tenant_created` (`tenant_id`, `created_at`),
    KEY `idx_query_trace` (`trace_id`),
    KEY `idx_query_conversation` (`conversation_id`, `created_at`),
    KEY `idx_query_type_status` (`query_type`, `status`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query audit log';

CREATE TABLE IF NOT EXISTS `rag_query_hit`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Query hit ID',
    `query_log_id`       BIGINT UNSIGNED NOT NULL,
    `rank_no`            INT UNSIGNED    NOT NULL,
    `score`              DECIMAL(12, 10)          DEFAULT NULL,
    `knowledge_base_id`  BIGINT UNSIGNED          DEFAULT NULL,
    `document_id`        VARCHAR(128)             DEFAULT NULL,
    `document_name`      VARCHAR(255)             DEFAULT NULL,
    `chunk_id`           VARCHAR(128)             DEFAULT NULL,
    `chunk_version`      INT UNSIGNED             DEFAULT NULL,
    `content_type`       VARCHAR(32)              DEFAULT NULL,
    `page_no`            INT UNSIGNED             DEFAULT NULL,
    `section_title`      VARCHAR(500)             DEFAULT NULL,
    `image_url`          VARCHAR(1000)            DEFAULT NULL,
    `content_snippet`    TEXT                     DEFAULT NULL,
    `metadata_json`      JSON                     DEFAULT NULL,
    `created_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_query_hit_log` (`query_log_id`, `rank_no`),
    KEY `idx_query_hit_chunk` (`chunk_id`),
    KEY `idx_query_hit_document` (`document_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query retrieval hit';

CREATE TABLE IF NOT EXISTS `rag_query_feedback`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Query feedback ID',
    `query_log_id`     BIGINT UNSIGNED NOT NULL,
    `rating`           VARCHAR(32)     NOT NULL COMMENT 'HELPFUL, NOT_HELPFUL, CORRECTION',
    `created_by`       VARCHAR(128)             DEFAULT NULL,
    `comment`          VARCHAR(2000)            DEFAULT NULL,
    `corrected_answer` MEDIUMTEXT               DEFAULT NULL,
    `created_at`       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_query_feedback_log` (`query_log_id`, `created_at`),
    KEY `idx_query_feedback_rating` (`rating`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query feedback';


-- ============================================================================
-- Source: rag_query_feedback_advanced_upgrade.sql
-- ============================================================================

-- Query audit governance, cost analytics, feedback workflow, and revision task upgrade.
-- Review before running on an existing database and back up data first.

ALTER TABLE `rag_query_log`
    ADD COLUMN `archive_status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, ARCHIVED, DELETE_PENDING, DELETED' AFTER `status`,
    ADD COLUMN `retention_until` DATETIME(3) DEFAULT NULL AFTER `archive_status`,
    ADD COLUMN `deleted_at` DATETIME(3) DEFAULT NULL AFTER `retention_until`,
    ADD COLUMN `deleted_by` VARCHAR(128) DEFAULT NULL AFTER `deleted_at`,
    ADD COLUMN `delete_reason` VARCHAR(1000) DEFAULT NULL AFTER `deleted_by`,
    ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 AFTER `delete_reason`,
    ADD COLUMN `llm_provider` VARCHAR(64) DEFAULT NULL AFTER `total_tokens`,
    ADD COLUMN `llm_model` VARCHAR(128) DEFAULT NULL AFTER `llm_provider`,
    ADD COLUMN `embedding_provider` VARCHAR(64) DEFAULT NULL AFTER `llm_model`,
    ADD COLUMN `embedding_model` VARCHAR(128) DEFAULT NULL AFTER `embedding_provider`,
    ADD COLUMN `estimated_input_cost` DECIMAL(18, 8) DEFAULT NULL AFTER `embedding_model`,
    ADD COLUMN `estimated_output_cost` DECIMAL(18, 8) DEFAULT NULL AFTER `estimated_input_cost`,
    ADD COLUMN `estimated_embedding_cost` DECIMAL(18, 8) DEFAULT NULL AFTER `estimated_output_cost`,
    ADD COLUMN `estimated_total_cost` DECIMAL(18, 8) DEFAULT NULL AFTER `estimated_embedding_cost`,
    ADD COLUMN `cost_currency` VARCHAR(16) DEFAULT NULL AFTER `estimated_total_cost`,
    ADD KEY `idx_query_log_archive_status` (`archive_status`, `retention_until`, `created_at`),
    ADD KEY `idx_query_log_deleted` (`is_deleted`, `deleted_at`),
    ADD KEY `idx_query_cost_time` (`tenant_id`, `llm_model`, `created_at`);

ALTER TABLE `rag_query_feedback`
    ADD COLUMN `feedback_status` VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, TRIAGED, IN_REVIEW, REVISION_PLANNED, RESOLVED, REJECTED, CLOSED' AFTER `corrected_answer`,
    ADD COLUMN `priority` VARCHAR(32) NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW, MEDIUM, HIGH, URGENT' AFTER `feedback_status`,
    ADD COLUMN `assignee` VARCHAR(128) DEFAULT NULL AFTER `priority`,
    ADD COLUMN `review_result` VARCHAR(32) DEFAULT NULL COMMENT 'VALID, INVALID, DUPLICATE, NEEDS_MORE_INFO' AFTER `assignee`,
    ADD COLUMN `review_comment` VARCHAR(2000) DEFAULT NULL AFTER `review_result`,
    ADD COLUMN `resolved_at` DATETIME(3) DEFAULT NULL AFTER `review_comment`,
    ADD COLUMN `closed_at` DATETIME(3) DEFAULT NULL AFTER `resolved_at`,
    ADD COLUMN `reopened_count` INT NOT NULL DEFAULT 0 AFTER `closed_at`,
    ADD KEY `idx_feedback_status` (`feedback_status`, `priority`, `updated_at`),
    ADD KEY `idx_feedback_assignee` (`assignee`, `feedback_status`, `updated_at`);

CREATE TABLE IF NOT EXISTS `rag_query_log_delete_audit`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Delete audit ID',
    `delete_no`          VARCHAR(64)     NOT NULL COMMENT 'Operation batch number',
    `operator`           VARCHAR(128)             DEFAULT NULL,
    `delete_mode`        VARCHAR(32)     NOT NULL COMMENT 'SOFT_DELETE, ARCHIVE, PURGE, RESTORE',
    `reason`             VARCHAR(1000)            DEFAULT NULL,
    `query_log_ids_json` JSON                     DEFAULT NULL,
    `matched_count`      INT             NOT NULL DEFAULT 0,
    `success_count`      INT             NOT NULL DEFAULT 0,
    `failed_count`       INT             NOT NULL DEFAULT 0,
    `filter_json`        JSON                     DEFAULT NULL,
    `result_json`        JSON                     DEFAULT NULL,
    `created_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_query_log_delete_no` (`delete_no`),
    KEY `idx_query_log_delete_mode` (`delete_mode`, `created_at`),
    KEY `idx_query_log_delete_operator` (`operator`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query log delete audit';

CREATE TABLE IF NOT EXISTS `rag_query_log_archive`
(
    `id`                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Archive row ID',
    `source_query_log_id`     BIGINT UNSIGNED NOT NULL,
    `delete_no`               VARCHAR(64)              DEFAULT NULL,
    `tenant_id`               BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `trace_id`                VARCHAR(64)              DEFAULT NULL,
    `conversation_id`         VARCHAR(128)             DEFAULT NULL,
    `query_type`              VARCHAR(16)     NOT NULL,
    `query_text`              TEXT            NOT NULL,
    `retrieval_mode`          VARCHAR(32)              DEFAULT NULL,
    `knowledge_base_ids_json` JSON                     DEFAULT NULL,
    `top_k`                   INT UNSIGNED             DEFAULT NULL,
    `min_score`               DECIMAL(8, 6)            DEFAULT NULL,
    `content_types_json`      JSON                     DEFAULT NULL,
    `permission_tags_json`    JSON                     DEFAULT NULL,
    `prompt_text`             MEDIUMTEXT               DEFAULT NULL,
    `answer_text`             MEDIUMTEXT               DEFAULT NULL,
    `knowledge_hit`           TINYINT         NOT NULL DEFAULT 0,
    `hit_count`               INT UNSIGNED    NOT NULL DEFAULT 0,
    `prompt_tokens`           INT UNSIGNED             DEFAULT NULL,
    `completion_tokens`       INT UNSIGNED             DEFAULT NULL,
    `total_tokens`            INT UNSIGNED             DEFAULT NULL,
    `llm_provider`            VARCHAR(64)              DEFAULT NULL,
    `llm_model`               VARCHAR(128)             DEFAULT NULL,
    `embedding_provider`      VARCHAR(64)              DEFAULT NULL,
    `embedding_model`         VARCHAR(128)             DEFAULT NULL,
    `estimated_input_cost`    DECIMAL(18, 8)           DEFAULT NULL,
    `estimated_output_cost`   DECIMAL(18, 8)           DEFAULT NULL,
    `estimated_embedding_cost` DECIMAL(18, 8)          DEFAULT NULL,
    `estimated_total_cost`    DECIMAL(18, 8)           DEFAULT NULL,
    `cost_currency`           VARCHAR(16)              DEFAULT NULL,
    `latency_ms`              BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `status`                  VARCHAR(16)     NOT NULL DEFAULT 'SUCCESS',
    `archive_status`          VARCHAR(32)     NOT NULL DEFAULT 'ARCHIVED',
    `retention_until`         DATETIME(3)              DEFAULT NULL,
    `error_code`              VARCHAR(64)              DEFAULT NULL,
    `error_message`           VARCHAR(2000)            DEFAULT NULL,
    `query_created_at`        DATETIME(3)              DEFAULT NULL,
    `query_updated_at`        DATETIME(3)              DEFAULT NULL,
    `archived_at`             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_query_archive_source` (`source_query_log_id`, `archived_at`),
    KEY `idx_query_archive_tenant_time` (`tenant_id`, `archived_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query log archive';

CREATE TABLE IF NOT EXISTS `rag_query_retention_policy`
(
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Retention policy ID',
    `tenant_id`             BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `policy_name`           VARCHAR(128)    NOT NULL,
    `query_type`            VARCHAR(32)     NOT NULL DEFAULT 'ALL',
    `status_filter`         VARCHAR(32)     NOT NULL DEFAULT 'ALL',
    `retention_days`        INT             NOT NULL DEFAULT 180,
    `archive_before_delete` TINYINT         NOT NULL DEFAULT 1,
    `enabled`               TINYINT         NOT NULL DEFAULT 1,
    `created_at`            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`            DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_retention_policy_match` (`tenant_id`, `query_type`, `status_filter`, `enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query retention policy';

CREATE TABLE IF NOT EXISTS `rag_model_pricing`
(
    `id`                         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Model pricing ID',
    `provider`                   VARCHAR(64)     NOT NULL,
    `model`                      VARCHAR(128)    NOT NULL,
    `input_cost_per_1k_tokens`   DECIMAL(18, 8)  NOT NULL DEFAULT 0,
    `output_cost_per_1k_tokens`  DECIMAL(18, 8)  NOT NULL DEFAULT 0,
    `currency`                   VARCHAR(16)     NOT NULL DEFAULT 'USD',
    `effective_from`             DATETIME(3)              DEFAULT NULL,
    `effective_to`               DATETIME(3)              DEFAULT NULL,
    `enabled`                    TINYINT         NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    KEY `idx_model_pricing_effective` (`provider`, `model`, `enabled`, `effective_from`, `effective_to`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG model token pricing';

CREATE TABLE IF NOT EXISTS `rag_query_cost_anomaly`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Cost anomaly ID',
    `tenant_id`      BIGINT UNSIGNED          DEFAULT NULL,
    `anomaly_type`   VARCHAR(64)     NOT NULL,
    `severity`       VARCHAR(32)     NOT NULL DEFAULT 'LOW',
    `metric_name`    VARCHAR(64)     NOT NULL,
    `metric_value`   DECIMAL(18, 6)           DEFAULT NULL,
    `baseline_value` DECIMAL(18, 6)           DEFAULT NULL,
    `window_start`   DATETIME(3)              DEFAULT NULL,
    `window_end`     DATETIME(3)              DEFAULT NULL,
    `status`         VARCHAR(32)     NOT NULL DEFAULT 'OPEN',
    `metadata_json`  JSON                     DEFAULT NULL,
    `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_query_cost_anomaly` (`tenant_id`, `status`, `severity`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query cost anomaly';

CREATE TABLE IF NOT EXISTS `rag_query_feedback_event`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Feedback event ID',
    `feedback_id`  BIGINT UNSIGNED NOT NULL,
    `event_type`   VARCHAR(64)     NOT NULL,
    `from_status`  VARCHAR(32)              DEFAULT NULL,
    `to_status`    VARCHAR(32)              DEFAULT NULL,
    `operator`     VARCHAR(128)             DEFAULT NULL,
    `comment`      VARCHAR(2000)            DEFAULT NULL,
    `payload_json` JSON                     DEFAULT NULL,
    `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_feedback_event` (`feedback_id`, `created_at`),
    KEY `idx_feedback_event_type` (`event_type`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG query feedback event';

CREATE TABLE IF NOT EXISTS `rag_feedback_revision_task`
(
    `id`                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Feedback revision task ID',
    `revision_no`              VARCHAR(64)     NOT NULL,
    `feedback_id`              BIGINT UNSIGNED NOT NULL,
    `query_log_id`             BIGINT UNSIGNED          DEFAULT NULL,
    `tenant_id`                BIGINT UNSIGNED          DEFAULT NULL,
    `knowledge_base_id`        BIGINT UNSIGNED          DEFAULT NULL,
    `document_id`              BIGINT UNSIGNED          DEFAULT NULL,
    `chunk_uid`                VARCHAR(128)             DEFAULT NULL,
    `revision_type`            VARCHAR(32)     NOT NULL DEFAULT 'OTHER',
    `revision_status`          VARCHAR(32)     NOT NULL DEFAULT 'PLANNED',
    `before_snapshot_json`     JSON                     DEFAULT NULL,
    `after_snapshot_json`      JSON                     DEFAULT NULL,
    `expected_fix`             VARCHAR(2000)            DEFAULT NULL,
    `verification_query`       TEXT                     DEFAULT NULL,
    `verification_result_json` JSON                     DEFAULT NULL,
    `created_by`               VARCHAR(128)             DEFAULT NULL,
    `assignee`                 VARCHAR(128)             DEFAULT NULL,
    `created_at`               DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`               DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_feedback_revision_no` (`revision_no`),
    KEY `idx_feedback_revision_feedback` (`feedback_id`, `created_at`),
    KEY `idx_feedback_revision_status` (`revision_status`, `updated_at`),
    KEY `idx_feedback_revision_kb` (`tenant_id`, `knowledge_base_id`, `revision_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG feedback revision task';


-- ============================================================================
-- Source: rag_rerank_eval_advanced_upgrade.sql
-- ============================================================================

ALTER TABLE `rag_retrieval_eval_case`
    ADD COLUMN `query_category` VARCHAR(64) NOT NULL DEFAULT 'definition' AFTER `retrieval_mode`,
    ADD COLUMN `difficulty_level` VARCHAR(32) NOT NULL DEFAULT 'easy' AFTER `query_category`,
    ADD COLUMN `language` VARCHAR(32) NOT NULL DEFAULT 'mixed' AFTER `difficulty_level`,
    ADD COLUMN `expected_answer_type` VARCHAR(64) NOT NULL DEFAULT 'fact' AFTER `language`,
    ADD KEY `idx_eval_case_category` (`tenant_id`, `knowledge_base_id`, `query_category`, `enabled`, `is_deleted`);

ALTER TABLE `rag_retrieval_eval_case_result`
    ADD COLUMN `failure_type` VARCHAR(64) DEFAULT NULL AFTER `recall_value`,
    ADD COLUMN `failure_reason` VARCHAR(1000) DEFAULT NULL AFTER `failure_type`,
    ADD COLUMN `retrieval_trace_json` JSON DEFAULT NULL AFTER `failure_reason`,
    ADD COLUMN `cluster_key` VARCHAR(128) DEFAULT NULL AFTER `retrieval_trace_json`,
    ADD KEY `idx_eval_result_failure` (`failure_type`, `cluster_key`, `created_at`);

CREATE TABLE IF NOT EXISTS `rag_retrieval_eval_cluster`
(
    `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Evaluation failure cluster ID',
    `run_id`               BIGINT UNSIGNED NOT NULL,
    `cluster_key`          VARCHAR(128)    NOT NULL,
    `cluster_label`        VARCHAR(255)    NOT NULL,
    `failure_type`         VARCHAR(64)     NOT NULL,
    `case_count`           INT UNSIGNED    NOT NULL DEFAULT 0,
    `sample_case_ids_json` JSON                     DEFAULT NULL,
    `suggestion`           VARCHAR(2000)            DEFAULT NULL,
    `created_at`           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_eval_cluster_run_key` (`run_id`, `cluster_key`),
    KEY `idx_eval_cluster_run` (`run_id`, `case_count`),
    KEY `idx_eval_cluster_type` (`failure_type`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='RAG retrieval evaluation failure cluster';

ALTER TABLE `rag_rerank_call_log`
    ADD COLUMN `api_key_hash` CHAR(16) DEFAULT NULL AFTER `query_hash`,
    ADD COLUMN `tenant_external_id` VARCHAR(128) DEFAULT NULL AFTER `api_key_hash`,
    ADD COLUMN `request_window` DATETIME(3) DEFAULT NULL AFTER `tenant_external_id`,
    ADD COLUMN `http_status` INT UNSIGNED DEFAULT NULL AFTER `error_code`,
    ADD COLUMN `error_code_normalized` VARCHAR(64) DEFAULT NULL AFTER `http_status`,
    ADD COLUMN `degraded_reason` VARCHAR(64) DEFAULT NULL AFTER `error_code_normalized`,
    ADD COLUMN `retry_count` INT UNSIGNED NOT NULL DEFAULT 0 AFTER `degraded_reason`,
    ADD COLUMN `cache_hit` TINYINT NOT NULL DEFAULT 0 AFTER `retry_count`,
    ADD KEY `idx_rerank_api_key_time` (`api_key_hash`, `created_at`),
    ADD KEY `idx_rerank_tenant_time` (`tenant_id`, `created_at`),
    ADD KEY `idx_rerank_error_time` (`error_code_normalized`, `created_at`),
    ADD KEY `idx_rerank_window` (`request_window`, `provider`, `model`);


-- ============================================================================
-- Source: rag_production_upgrade.sql
-- ============================================================================

-- Production hardening upgrade: identity, deletion worker, reindex orchestration and materialized metrics.

ALTER TABLE `tenant_data_deletion_task`
    ADD COLUMN `execution_mode` VARCHAR(32) NOT NULL DEFAULT 'DRY_RUN' AFTER `reason`,
    ADD COLUMN `lock_owner` VARCHAR(128) DEFAULT NULL AFTER `task_status`,
    ADD COLUMN `lock_until` DATETIME(3) DEFAULT NULL AFTER `lock_owner`,
    ADD COLUMN `verify_result_json` JSON DEFAULT NULL AFTER `lock_until`;

ALTER TABLE `tenant_data_deletion_stage`
    ADD COLUMN `error_code` VARCHAR(128) DEFAULT NULL AFTER `deleted_count`,
    ADD COLUMN `dry_run_result_json` JSON DEFAULT NULL AFTER `error_message`,
    ADD COLUMN `verify_status` VARCHAR(32) DEFAULT NULL AFTER `dry_run_result_json`,
    ADD COLUMN `verify_result_json` JSON DEFAULT NULL AFTER `verify_status`,
    ADD COLUMN `started_at` DATETIME(3) DEFAULT NULL AFTER `verify_result_json`,
    ADD COLUMN `finished_at` DATETIME(3) DEFAULT NULL AFTER `started_at`;

CREATE TABLE IF NOT EXISTS `rag_keyword_reindex_job`
(
    `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`              BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `job_no`                 VARCHAR(64)     NOT NULL,
    `source_index`           VARCHAR(255)    NOT NULL,
    `target_index`           VARCHAR(255)    NOT NULL,
    `alias_name`             VARCHAR(255)    NOT NULL,
    `template_version`       VARCHAR(64)              DEFAULT NULL,
    `job_status`             VARCHAR(32)     NOT NULL DEFAULT 'PLANNED',
    `progress`               INT UNSIGNED    NOT NULL DEFAULT 0,
    `total_count`            BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `success_count`          BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `failed_count`           BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `sample_validation_json` JSON                     DEFAULT NULL,
    `rollback_target`        VARCHAR(255)             DEFAULT NULL,
    `error_message`          VARCHAR(2000)            DEFAULT NULL,
    `started_at`             DATETIME(3)              DEFAULT NULL,
    `finished_at`            DATETIME(3)              DEFAULT NULL,
    `created_at`             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`             DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keyword_reindex_job_no` (`job_no`),
    KEY `idx_keyword_reindex_tenant_status` (`tenant_id`, `job_status`, `created_at`),
    KEY `idx_keyword_reindex_alias` (`alias_name`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Keyword index reindex orchestration job';

CREATE TABLE IF NOT EXISTS `rag_query_cost_metric_hourly`
(
    `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`            BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `knowledge_base_id`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `bucket_start`         DATETIME(3)     NOT NULL,
    `window_type`          VARCHAR(16)     NOT NULL DEFAULT 'HOUR',
    `query_type`           VARCHAR(32)              DEFAULT NULL,
    `retrieval_mode`       VARCHAR(32)              DEFAULT NULL,
    `llm_model`            VARCHAR(128)             DEFAULT NULL,
    `embedding_model`      VARCHAR(128)             DEFAULT NULL,
    `status`               VARCHAR(32)              DEFAULT NULL,
    `query_count`          BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `success_count`        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `failed_count`         BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `prompt_tokens`        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `completion_tokens`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `total_tokens`         BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `estimated_total_cost` DECIMAL(18, 8)  NOT NULL DEFAULT 0,
    `p50_latency_ms`       DECIMAL(18, 4)  NOT NULL DEFAULT 0,
    `p90_latency_ms`       DECIMAL(18, 4)  NOT NULL DEFAULT 0,
    `p95_latency_ms`       DECIMAL(18, 4)  NOT NULL DEFAULT 0,
    `p99_latency_ms`       DECIMAL(18, 4)  NOT NULL DEFAULT 0,
    `created_at`           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_query_cost_hourly` (`tenant_id`, `knowledge_base_id`, `bucket_start`, `query_type`, `retrieval_mode`, `llm_model`, `embedding_model`, `status`),
    KEY `idx_query_cost_hourly_bucket` (`bucket_start`, `tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Hourly materialized query cost metrics';

CREATE TABLE IF NOT EXISTS `rag_query_cost_metric_daily` LIKE `rag_query_cost_metric_hourly`;

ALTER TABLE `rag_query_cost_metric_hourly`
    ADD COLUMN `knowledge_base_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER `tenant_id`;

ALTER TABLE `rag_query_cost_metric_daily`
    ADD COLUMN `knowledge_base_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER `tenant_id`;

SET @query_cost_hourly_has_kb_unique := (
    SELECT COUNT(*)
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'rag_query_cost_metric_hourly'
       AND index_name = 'uk_query_cost_hourly'
       AND column_name = 'knowledge_base_id'
);
SET @query_cost_hourly_unique_sql := IF(
        @query_cost_hourly_has_kb_unique = 0,
        'ALTER TABLE `rag_query_cost_metric_hourly` DROP INDEX `uk_query_cost_hourly`, ADD UNIQUE KEY `uk_query_cost_hourly` (`tenant_id`, `knowledge_base_id`, `bucket_start`, `query_type`, `retrieval_mode`, `llm_model`, `embedding_model`, `status`)',
        'SELECT 1'
);
PREPARE query_cost_hourly_unique_stmt FROM @query_cost_hourly_unique_sql;
EXECUTE query_cost_hourly_unique_stmt;
DEALLOCATE PREPARE query_cost_hourly_unique_stmt;

SET @query_cost_daily_unique_name := (
    SELECT index_name
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'rag_query_cost_metric_daily'
       AND non_unique = 0
       AND index_name <> 'PRIMARY'
       AND column_name = 'tenant_id'
     LIMIT 1
);
SET @query_cost_daily_has_kb_unique := (
    SELECT COUNT(*)
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'rag_query_cost_metric_daily'
       AND non_unique = 0
       AND index_name = @query_cost_daily_unique_name
       AND column_name = 'knowledge_base_id'
);
SET @query_cost_daily_unique_sql := IF(
        @query_cost_daily_unique_name IS NOT NULL AND @query_cost_daily_has_kb_unique = 0,
        CONCAT('ALTER TABLE `rag_query_cost_metric_daily` DROP INDEX `', @query_cost_daily_unique_name, '`, ADD UNIQUE KEY `', @query_cost_daily_unique_name, '` (`tenant_id`, `knowledge_base_id`, `bucket_start`, `query_type`, `retrieval_mode`, `llm_model`, `embedding_model`, `status`)'),
        'SELECT 1'
);
PREPARE query_cost_daily_unique_stmt FROM @query_cost_daily_unique_sql;
EXECUTE query_cost_daily_unique_stmt;
DEALLOCATE PREPARE query_cost_daily_unique_stmt;

CREATE TABLE IF NOT EXISTS `rag_feedback_quality_metric_hourly`
(
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`          BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `bucket_start`       DATETIME(3)     NOT NULL,
    `window_type`        VARCHAR(16)     NOT NULL DEFAULT 'HOUR',
    `knowledge_base_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `retrieval_mode`     VARCHAR(32)              DEFAULT NULL,
    `query_type`         VARCHAR(32)              DEFAULT NULL,
    `feedback_rating`    VARCHAR(32)              DEFAULT NULL,
    `feedback_status`    VARCHAR(32)              DEFAULT NULL,
    `assignee`           VARCHAR(128)             DEFAULT NULL,
    `query_count`        BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `feedback_count`     BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `helpful_count`      BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `not_helpful_count`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `correction_count`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `created_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_feedback_quality_hourly` (`tenant_id`, `bucket_start`, `knowledge_base_id`, `retrieval_mode`, `query_type`, `feedback_rating`, `feedback_status`, `assignee`),
    KEY `idx_feedback_quality_hourly_bucket` (`bucket_start`, `tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Hourly materialized feedback quality metrics';

CREATE TABLE IF NOT EXISTS `rag_feedback_quality_metric_daily` LIKE `rag_feedback_quality_metric_hourly`;

CREATE TABLE IF NOT EXISTS `rag_rerank_observation_metric_hourly`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `bucket_start`    DATETIME(3)     NOT NULL,
    `window_type`     VARCHAR(16)     NOT NULL DEFAULT 'HOUR',
    `provider`        VARCHAR(64)              DEFAULT NULL,
    `model`           VARCHAR(128)             DEFAULT NULL,
    `api_key_hash`    VARCHAR(64)              DEFAULT NULL,
    `error_code`      VARCHAR(128)             DEFAULT NULL,
    `degraded_reason` VARCHAR(128)             DEFAULT NULL,
    `request_count`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `success_count`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `failure_count`   BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `fallback_count`  BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `retry_count`     BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `cache_hit_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `total_tokens`    BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `estimated_cost`  DECIMAL(18, 8)  NOT NULL DEFAULT 0,
    `p50_latency_ms`  DECIMAL(18, 4)  NOT NULL DEFAULT 0,
    `p90_latency_ms`  DECIMAL(18, 4)  NOT NULL DEFAULT 0,
    `p95_latency_ms`  DECIMAL(18, 4)  NOT NULL DEFAULT 0,
    `p99_latency_ms`  DECIMAL(18, 4)  NOT NULL DEFAULT 0,
    `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rerank_observation_hourly` (`tenant_id`, `bucket_start`, `provider`, `model`, `api_key_hash`, `error_code`, `degraded_reason`),
    KEY `idx_rerank_observation_hourly_bucket` (`bucket_start`, `tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Hourly materialized rerank observation metrics';

CREATE TABLE IF NOT EXISTS `rag_rerank_observation_metric_daily` LIKE `rag_rerank_observation_metric_hourly`;

CREATE TABLE IF NOT EXISTS `rag_metric_aggregation_watermark`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `metric_name`  VARCHAR(64)     NOT NULL,
    `window_type`  VARCHAR(16)     NOT NULL,
    `watermark_at` DATETIME(3)     NOT NULL,
    `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_metric_watermark` (`metric_name`, `window_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='Materialized metric aggregation watermark';


-- ============================================================================
-- Tenant Isolation: Add tenant_id column to tables missing it
-- These tables were previously in the mybatis-ignore-tables list.
-- After adding tenant_id, the MyBatis-Plus TenantLineInnerInterceptor
-- automatically filters all queries by tenant_id.
--
-- NOTE: MySQL does not support "ADD COLUMN" (that is MariaDB syntax).
-- These statements use standard MySQL ALTER TABLE syntax. For idempotent re-runs
-- on existing databases, use the JDBC migration runner at target/migration/RunMigration.java
-- which checks INFORMATION_SCHEMA before each ALTER.
-- ============================================================================

-- work_order: legacy business table
ALTER TABLE `work_order`
    ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- rag_ingestion_task_shard: child of rag_ingestion_task
ALTER TABLE `rag_ingestion_task_shard`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- rag_ingestion_task_event: child of rag_ingestion_task
ALTER TABLE `rag_ingestion_task_event`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- rag_ingestion_task_stage: child of rag_ingestion_task
ALTER TABLE `rag_ingestion_task_stage`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- rag_query_hit: child of rag_query_log
ALTER TABLE `rag_query_hit`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- rag_query_feedback: child of rag_query_log
ALTER TABLE `rag_query_feedback`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- rag_query_log_delete_audit: audit trail for query log deletion
ALTER TABLE `rag_query_log_delete_audit`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- rag_query_feedback_event: child of rag_query_feedback
ALTER TABLE `rag_query_feedback_event`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- rag_retrieval_eval_case_result: child of rag_retrieval_eval_run
ALTER TABLE `rag_retrieval_eval_case_result`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- rag_retrieval_eval_cluster: child of rag_retrieval_eval_run
ALTER TABLE `rag_retrieval_eval_cluster`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- sys_admin_impersonation_session: add standard tenant_id (= operator_tenant_id)
ALTER TABLE `sys_admin_impersonation_session`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'Tenant ID (mirrors operator_tenant_id)' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- Backfill tenant_id from operator_tenant_id for existing rows
UPDATE `sys_admin_impersonation_session` SET `tenant_id` = `operator_tenant_id` WHERE `tenant_id` IS NULL AND `operator_tenant_id` IS NOT NULL;

-- sys_operation_audit_log: add standard tenant_id (= operator_tenant_id)
ALTER TABLE `sys_operation_audit_log`
    ADD COLUMN `tenant_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'Tenant ID (mirrors operator_tenant_id)' AFTER `id`,
    ADD KEY `idx_tenant_id` (`tenant_id`);

-- Backfill tenant_id from operator_tenant_id for existing rows
UPDATE `sys_operation_audit_log` SET `tenant_id` = `operator_tenant_id` WHERE `tenant_id` IS NULL AND `operator_tenant_id` IS NOT NULL;

-- ============================================================================
-- Agent System Prompt (decoupled from code)
-- ============================================================================

CREATE TABLE IF NOT EXISTS `rag_agent_prompt`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `tenant_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID (0 = global default)',
    `prompt_name`     VARCHAR(64)     NOT NULL DEFAULT 'default' COMMENT 'Prompt name/identifier',
    `prompt_content`  TEXT            NOT NULL COMMENT 'System prompt content',
    `version`         INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT 'Version number',
    `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 active',
    `created_by`      VARCHAR(64)              DEFAULT NULL COMMENT 'Created by user',
    `updated_by`      VARCHAR(64)              DEFAULT NULL COMMENT 'Updated by user',
    `created_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_prompt_version` (`tenant_id`, `prompt_name`, `version`),
    KEY `idx_tenant_active` (`tenant_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='Agent system prompt templates';

-- Seed default prompt for tenant 0 (global)
INSERT INTO `rag_agent_prompt` (`tenant_id`, `prompt_name`, `prompt_content`, `version`, `status`, `created_by`)
SELECT 0, 'default', '你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。
你必须遵守以下规则：

1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，
   优先调用 ticketAnalysis 工具。
2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，
   不要直接调用 ticketAnalysis 工具，先追问用户：
   "请告诉我需要查询的处理人用户ID。"
3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。
4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。
5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。
6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。
6.1 当用户要求"生成图片/画图/出图/文生图/海报/插画"等视觉内容时，调用 textToImage 工具。
7. 回答时优先基于工具返回内容，不要编造不存在的事实。
8. 如果工单工具返回了 totalCount 和状态分布，回答时要用自然中文总结：
   - 总工单数
   - 各状态数量
   - 简要处理情况判断
9. 如果调用了互联网搜索，请在正文中说明结论，并保留"来源"语义，方便外层系统追加标准来源块。
10. 对话要简洁、专业、中文输出；必要时可用要点列表。
11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。', 1, 1, 'system'
WHERE NOT EXISTS (SELECT 1 FROM `rag_agent_prompt` WHERE `tenant_id` = 0 AND `prompt_name` = 'default');
