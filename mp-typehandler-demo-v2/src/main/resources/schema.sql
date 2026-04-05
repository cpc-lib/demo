CREATE DATABASE IF NOT EXISTS test DEFAULT CHARACTER SET utf8mb4;
USE test;

DROP TABLE IF EXISTS export_task;
DROP TABLE IF EXISTS user_profile;

CREATE TABLE user_profile (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(64) NOT NULL COMMENT '姓名',
    hobbies JSON DEFAULT NULL COMMENT '爱好，List<String>',
    tags JSON DEFAULT NULL COMMENT '标签，List<TagItem>',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
