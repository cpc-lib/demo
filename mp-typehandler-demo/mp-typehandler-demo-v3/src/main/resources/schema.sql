CREATE DATABASE IF NOT EXISTS test DEFAULT CHARACTER SET utf8mb4;
USE test;

DROP TABLE IF EXISTS change_log_detail;
DROP TABLE IF EXISTS change_log;
DROP TABLE IF EXISTS export_task;
DROP TABLE IF EXISTS user_profile;

CREATE TABLE user_profile (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(64) NOT NULL COMMENT '姓名',
    hobbies JSON DEFAULT NULL COMMENT '爱好，List<String>',
    tags JSON DEFAULT NULL COMMENT '标签，List<TagItem>',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';

CREATE TABLE export_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    business_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    status VARCHAR(32) NOT NULL COMMENT '任务状态',
    message VARCHAR(255) DEFAULT NULL COMMENT '任务消息',
    query_name VARCHAR(64) DEFAULT NULL COMMENT '姓名筛选条件',
    page_no INT DEFAULT NULL COMMENT '导出页码',
    page_size INT DEFAULT NULL COMMENT '导出页大小',
    fields_json TEXT DEFAULT NULL COMMENT '导出字段JSON',
    file_name VARCHAR(255) DEFAULT NULL COMMENT '导出文件名',
    file_path VARCHAR(512) DEFAULT NULL COMMENT '导出文件路径',
    total_count BIGINT NOT NULL DEFAULT 0 COMMENT '总数据量',
    exported_count BIGINT NOT NULL DEFAULT 0 COMMENT '已导出数量',
    fail_reason VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    finished_at DATETIME DEFAULT NULL COMMENT '结束时间',
    expire_at DATETIME DEFAULT NULL COMMENT '过期时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_business_status_created (business_type, status, created_at),
    KEY idx_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出任务表';

CREATE TABLE change_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型，如user_profile',
    biz_id VARCHAR(64) NOT NULL COMMENT '业务主键ID',
    operation_type VARCHAR(32) NOT NULL COMMENT '操作类型，如UPDATE',
    log_mode VARCHAR(32) NOT NULL COMMENT '日志记录模式：ANNOTATION/HARDCODED',
    operator VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    method_name VARCHAR(128) DEFAULT NULL COMMENT '应用层方法名',
    request_uri VARCHAR(255) DEFAULT NULL COMMENT '请求URI',
    changed_field_count INT NOT NULL DEFAULT 0 COMMENT '变更字段数量',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_biz_type_biz_id_created (biz_type, biz_id, created_at),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务变更日志主表';

CREATE TABLE change_log_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    log_id BIGINT NOT NULL COMMENT '日志主表ID',
    field_name VARCHAR(64) NOT NULL COMMENT '字段名',
    field_label VARCHAR(64) DEFAULT NULL COMMENT '字段展示名',
    old_value TEXT DEFAULT NULL COMMENT '旧值',
    new_value TEXT DEFAULT NULL COMMENT '新值',
    value_type VARCHAR(64) DEFAULT NULL COMMENT '值类型',
    changed TINYINT NOT NULL DEFAULT 0 COMMENT '是否发生变更，1是0否',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_log_id (log_id),
    KEY idx_field_name (field_name),
    CONSTRAINT fk_change_log_detail_log_id FOREIGN KEY (log_id) REFERENCES change_log(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务变更日志明细表';
