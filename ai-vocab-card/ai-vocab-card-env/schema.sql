CREATE DATABASE IF NOT EXISTS ai_vocab DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_vocab;

CREATE TABLE IF NOT EXISTS word_card (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  word VARCHAR(128) NOT NULL COMMENT '英文单词',
  phonetic VARCHAR(128) DEFAULT NULL COMMENT '音标',
  part_of_speech VARCHAR(64) DEFAULT NULL COMMENT '词性',
  english_definition TEXT NOT NULL COMMENT '英文解释',
  chinese_meaning TEXT COMMENT '中文含义',
  usage_note TEXT COMMENT '用法说明',
  tags VARCHAR(512) COMMENT '标签，逗号分隔',
  status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_word (word),
  KEY idx_updated_at (updated_at),
  FULLTEXT KEY ft_word_content (word, english_definition, chinese_meaning)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS word_slang (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  word_card_id BIGINT NOT NULL,
  phrase VARCHAR(255) NOT NULL COMMENT '俚语/口语表达',
  meaning TEXT COMMENT '英文解释',
  example TEXT COMMENT '例句',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_word_card_id (word_card_id),
  CONSTRAINT fk_slang_word FOREIGN KEY (word_card_id) REFERENCES word_card(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS word_example (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  word_card_id BIGINT NOT NULL,
  sentence TEXT NOT NULL COMMENT '英文句子',
  translation TEXT COMMENT '中文翻译',
  scene VARCHAR(128) COMMENT '使用场景',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_word_card_id (word_card_id),
  CONSTRAINT fk_example_word FOREIGN KEY (word_card_id) REFERENCES word_card(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_word_book (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID，演示版由前端传入',
  word_card_id BIGINT NOT NULL COMMENT '词卡ID',
  mastery_level TINYINT DEFAULT 0 COMMENT '掌握等级 0-5',
  review_count INT DEFAULT 0 COMMENT '复习次数',
  ease_factor DECIMAL(4,2) DEFAULT 2.50 COMMENT '记忆难度因子',
  last_review_time DATETIME NULL COMMENT '上次复习时间',
  next_review_time DATETIME NOT NULL COMMENT '下次复习时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_word (user_id, word_card_id),
  KEY idx_user_next_review (user_id, next_review_time),
  CONSTRAINT fk_user_word_card FOREIGN KEY (word_card_id) REFERENCES word_card(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS word_review_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  word_card_id BIGINT NOT NULL,
  result TINYINT NOT NULL COMMENT '0忘记 1模糊 2记住',
  interval_days INT DEFAULT 0 COMMENT '本次计算出的间隔天数',
  ease_factor DECIMAL(4,2) DEFAULT 2.50 COMMENT '本次复习后的难度因子',
  review_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  next_review_time DATETIME NOT NULL,
  KEY idx_user_review_time (user_id, review_time),
  KEY idx_word_card_id (word_card_id),
  CONSTRAINT fk_review_word_card FOREIGN KEY (word_card_id) REFERENCES word_card(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  user_code VARCHAR(64) NOT NULL COMMENT '用户编号',
  password_hash VARCHAR(128) NOT NULL,
  nickname VARCHAR(64),
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_user_code (user_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_usage_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  model VARCHAR(128) NOT NULL,
  request_type VARCHAR(64) NOT NULL,
  input_text VARCHAR(512),
  prompt_tokens INT DEFAULT 0,
  completion_tokens INT DEFAULT 0,
  total_tokens INT DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  error_message VARCHAR(1024),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_created (user_id, created_at),
  KEY idx_model_created (model, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS export_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  export_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
  file_name VARCHAR(255),
  file_url TEXT,
  error_message TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_user_created (user_id, created_at),
  KEY idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS word_embedding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  word_card_id BIGINT NOT NULL,
  content TEXT NOT NULL COMMENT '用于语义检索的拼接文本',
  keywords TEXT COMMENT '本地语义降级关键词',
  vector_provider VARCHAR(64) DEFAULT 'local-keyword-jaccard',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_word_card_id (word_card_id),
  FULLTEXT KEY ft_embedding_content (content, keywords),
  CONSTRAINT fk_embedding_word_card FOREIGN KEY (word_card_id) REFERENCES word_card(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prompt_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL COMMENT 'Prompt 编码',
  version VARCHAR(32) NOT NULL COMMENT '版本号',
  title VARCHAR(128),
  content TEXT NOT NULL COMMENT '模板内容，变量如 {{word}}',
  enabled TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_code_version (code, version),
  KEY idx_code_enabled (code, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO prompt_template(code, version, title, content, enabled)
VALUES (
  'WORD_CARD_GENERATE',
  'v1',
  '默认英文词卡生成 Prompt',
  'You are an English vocabulary assistant for Chinese learners. Given one English word, return valid JSON only. Quality requirements: Give an accurate English definition for the most common learner meaning of the word. The English definition must be specific to this word, not a generic phrase. Give a natural Simplified Chinese meaning that matches the English definition. If the word has multiple common meanings, mention the main alternatives briefly in usageNote. Do not invent slang, idioms, or examples. Use an empty slangs array when there is no common informal expression. Examples must be natural, short, and translated accurately into Simplified Chinese. Return one JSON object only. Do not wrap it in markdown. The exact JSON schema is: {"word":"lowercase word","phonetic":"IPA pronunciation if known","partOfSpeech":"noun/verb/adjective/...","englishDefinition":"accurate English definition","chineseMeaning":"natural Simplified Chinese meaning","usageNote":"short usage note","slangs":[{"phrase":"...","meaning":"...","example":"..."}],"examples":[{"sentence":"...","translation":"...","scene":"..."}],"tags":["learning tag"]}. Word: {{word}}',
  1
) ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), enabled = VALUES(enabled), updated_at = CURRENT_TIMESTAMP;


CREATE TABLE IF NOT EXISTS outbox_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  aggregate_type VARCHAR(64) NOT NULL,
  aggregate_id BIGINT NOT NULL,
  event_type VARCHAR(128) NOT NULL,
  payload JSON NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  error_message TEXT,
  next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_status_retry (status, next_retry_time),
  KEY idx_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS idempotency_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  request_key VARCHAR(128) NOT NULL,
  request_uri VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  expired_at DATETIME NOT NULL,
  UNIQUE KEY uk_user_request (user_id, request_key, request_uri),
  KEY idx_expired_at (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE app_user ADD COLUMN user_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户编号' AFTER username, ADD UNIQUE KEY uk_user_code (user_code);