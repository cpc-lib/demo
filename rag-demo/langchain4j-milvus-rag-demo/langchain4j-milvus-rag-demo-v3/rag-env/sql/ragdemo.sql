/*
 Navicat Premium Dump SQL

 Source Server         : 192.168.220.200_3306
 Source Server Type    : MySQL
 Source Server Version : 80410 (8.4.10)
 Source Host           : 192.168.220.200:3306
 Source Schema         : ragdemo

 Target Server Type    : MySQL
 Target Server Version : 80410 (8.4.10)
 File Encoding         : 65001

 Date: 01/08/2026 09:30:34
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for rag_agent_prompt
-- ----------------------------
DROP TABLE IF EXISTS `rag_agent_prompt`;
CREATE TABLE `rag_agent_prompt`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID (0 = global default)',
  `prompt_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'default' COMMENT 'Prompt name/identifier',
  `prompt_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'System prompt content',
  `version` int UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Version number',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 active',
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Created by user',
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Updated by user',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_prompt_version`(`tenant_id` ASC, `prompt_name` ASC, `version` ASC) USING BTREE,
  INDEX `idx_tenant_active`(`tenant_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Agent system prompt templates' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_agent_prompt
-- ----------------------------
INSERT INTO `rag_agent_prompt` VALUES (1, 0, 'default', '你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。\n你必须遵守以下规则：\n\n1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，\n   优先调用 ticketAnalysis 工具。\n2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，\n   不要直接调用 ticketAnalysis 工具，先追问用户：\n   \"请告诉我需要查询的处理人用户ID。\"\n3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。\n4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。\n5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。\n6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。\n7. 只有在工具调用所需参数充足时才调用工具；缺少关键参数时先追问。\n8. 工单分析类回答必须包含：\n   - 查询范围\n   - 总工单数\n   - 各状态数量\n   - 简要处理情况判断\n9. 如果调用了互联网搜索，请在正文中说明结论，并保留\"来源\"语义，方便外层系统追加标准来源块。\n10. 对话要简洁、专业、中文输出；必要时可用要点列表。\n11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。', 1, 0, 'system', 'demo-user', '2026-07-30 15:36:40.690', '2026-07-30 15:44:01.492');
INSERT INTO `rag_agent_prompt` VALUES (2, 0, 'default', '你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。\n你必须遵守以下规则：\n\n1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，\n   优先调用 ticketAnalysis 工具。\n2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，\n   不要直接调用 ticketAnalysis 工具，先追问用户：\n   \"请告诉我需要查询的处理人用户ID。\"\n3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。\n4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。\n5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。\n6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。\n7. 只有在工具调用所需参数充足时才调用工具；缺少关键参数时先追问。\n8. 工单分析类回答必须包含：\n   - 查询范围\n   - 总工单数\n   - 各状态数量\n   - 简要处理情况判断\n9. 如果调用了互联网搜索，请在正文中说明结论，并保留\"来源\"语义，方便外层系统追加标准来源块。\n10. 对话要简洁、专业、中文输出；必要时可用要点列表。\n11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。', 2, 0, 'demo-user', 'demo-user', '2026-07-30 15:43:41.387', '2026-07-30 19:41:45.390');
INSERT INTO `rag_agent_prompt` VALUES (3, 0, 'default', '你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。\n你必须遵守以下规则：\n\n1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，\n   优先调用 ticketAnalysis 工具。\n2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，\n   不要直接调用 ticketAnalysis 工具，先追问用户：\n   \"请告诉我需要查询的处理人用户ID。\"\n3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。\n4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。\n5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。\n6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。\n7. 只有在工具调用所需参数充足时才调用工具；缺少关键参数时先追问。\n8. 工单分析类回答必须包含：\n   - 查询范围\n   - 总工单数\n   - 各状态数量\n   - 简要处理情况判断\n9. 如果调用了互联网搜索，请在正文中说明结论，并保留\"来源\"语义，方便外层系统追加标准来源块。\n10. 对话要简洁、专业、中文输出；必要时可用要点列表。\n11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。', 3, 0, 'demo-user', 'demo-user', '2026-07-30 19:41:32.619', '2026-07-30 19:41:32.619');
INSERT INTO `rag_agent_prompt` VALUES (4, 0, 'default', '你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。\n你必须遵守以下规则：\n\n1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，\n   优先调用 ticketAnalysis 工具。\n2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，\n   不要直接调用 ticketAnalysis 工具，先追问用户：\n   \"请告诉我需要查询的处理人用户ID。\"\n3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。\n4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。\n5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。\n6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。\n7. 只有在工具调用所需参数充足时才调用工具；缺少关键参数时先追问。\n8. 工单分析类回答必须包含：\n   - 查询范围\n   - 总工单数\n   - 各状态数量\n   - 简要处理情况判断\n9. 如果调用了互联网搜索，请在正文中说明结论，并保留\"来源\"语义，方便外层系统追加标准来源块。\n10. 对话要简洁、专业、中文输出；必要时可用要点列表。\n11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。', 4, 0, 'demo-user', 'demo-user', '2026-07-30 19:41:45.426', '2026-07-30 19:42:07.660');
INSERT INTO `rag_agent_prompt` VALUES (5, 0, 'default', '你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。\n你必须遵守以下规则：\n\n1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，\n   优先调用 ticketAnalysis 工具。\n2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，\n   不要直接调用 ticketAnalysis 工具，先追问用户：\n   \"请告诉我需要查询的处理人用户ID。\"\n3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。\n4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。\n5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。\n6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。\n7. 只有在工具调用所需参数充足时才调用工具；缺少关键参数时先追问。\n8. 工单分析类回答必须包含：\n   - 查询范围\n   - 总工单数\n   - 各状态数量\n   - 简要处理情况判断\n9. 如果调用了互联网搜索，请在正文中说明结论，并保留\"来源\"语义，方便外层系统追加标准来源块。\n10. 对话要简洁、专业、中文输出；必要时可用要点列表。\n11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。', 5, 0, 'demo-user', 'demo-user', '2026-07-30 19:42:07.700', '2026-07-30 19:54:07.369');
INSERT INTO `rag_agent_prompt` VALUES (6, 0, 'default', '你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。\n你必须遵守以下规则：\n\n1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，\n   优先调用 ticketAnalysis 工具。\n2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，\n   不要直接调用 ticketAnalysis 工具，先追问用户：\n   \"请告诉我需要查询的处理人用户ID。\"\n3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。\n4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。\n5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。\n6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。\n7. 只有在工具调用所需参数充足时才调用工具；缺少关键参数时先追问。\n8. 工单分析类回答必须包含：\n   - 查询范围\n   - 总工单数\n   - 各状态数量\n   - 简要处理情况判断\n9. 如果调用了互联网搜索，请在正文中说明结论，并保留\"来源\"语义，方便外层系统追加标准来源块。\n10. 对话要简洁、专业、中文输出；必要时可用要点列表。\n11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。', 6, 0, 'demo-user', 'demo-user', '2026-07-30 19:54:00.656', '2026-07-30 19:54:00.656');
INSERT INTO `rag_agent_prompt` VALUES (7, 0, 'default', '你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。\n你必须遵守以下规则：\n\n1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，\n   优先调用 ticketAnalysis 工具。\n2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，\n   不要直接调用 ticketAnalysis 工具，先追问用户：\n   \"请告诉我需要查询的处理人用户ID。\"\n3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。\n4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。\n5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。\n6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。\n7. 只有在工具调用所需参数充足时才调用工具；缺少关键参数时先追问。\n8. 工单分析类回答必须包含：\n   - 查询范围\n   - 总工单数\n   - 各状态数量\n   - 简要处理情况判断\n9. 如果调用了互联网搜索，请在正文中说明结论，并保留\"来源\"语义，方便外层系统追加标准来源块。\n10. 对话要简洁、专业、中文输出；必要时可用要点列表。\n11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。', 7, 1, 'demo-user', 'demo-user', '2026-07-30 19:54:07.416', '2026-07-30 19:54:07.416');
INSERT INTO `rag_agent_prompt` VALUES (8, 1, 'default', '你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。\n你必须遵守以下规则：\n\n1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，\n   优先调用 ticketAnalysis 工具。\n2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，\n   不要直接调用 ticketAnalysis 工具，先追问用户：\n   \"请告诉我需要查询的处理人用户ID。\"\n3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。\n4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。\n5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。\n6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。\n7. 只有在工具调用所需参数充足时才调用工具；缺少关键参数时先追问。\n8. 工单分析类回答必须包含：\n   - 查询范围\n   - 总工单数\n   - 各状态数量\n   - 简要处理情况判断\n9. 如果调用了互联网搜索，请在正文中说明结论，并保留\"来源\"语义，方便外层系统追加标准来源块。\n10. 对话要简洁、专业、中文输出；必要时可用要点列表。\n11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。', 1, 0, 'demo-user', 'demo-user', '2026-08-01 09:18:31.695', '2026-08-01 09:18:31.695');
INSERT INTO `rag_agent_prompt` VALUES (9, 1, 'default', '你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。\n你必须遵守以下规则：\n\n1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，\n   优先调用 ticketAnalysis 工具。\n2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，\n   不要直接调用 ticketAnalysis 工具，先追问用户：\n   \"请告诉我需要查询的处理人用户ID。\"\n3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。\n4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。\n5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。\n6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。\n7. 只有在工具调用所需参数充足时才调用工具；缺少关键参数时先追问。\n8. 工单分析类回答必须包含：\n   - 查询范围\n   - 总工单数\n   - 各状态数量\n   - 简要处理情况判断\n9. 如果调用了互联网搜索，请在正文中说明结论，并保留\"来源\"语义，方便外层系统追加标准来源块。\n10. 对话要简洁、专业、中文输出；必要时可用要点列表。\n11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。', 2, 1, 'demo-user', 'demo-user', '2026-08-01 09:18:38.017', '2026-08-01 09:18:38.017');

-- ----------------------------
-- Table structure for rag_document
-- ----------------------------
DROP TABLE IF EXISTS `rag_document`;
CREATE TABLE `rag_document`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Document ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NOT NULL,
  `document_uid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Stable document UID',
  `document_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` tinyint NOT NULL DEFAULT 1 COMMENT '1 upload, 2 web, 3 api, 4 db, 5 object store',
  `source_uri` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `object_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `original_filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `file_extension` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `mime_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `file_size` bigint UNSIGNED NULL DEFAULT 0,
  `file_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `current_version_id` bigint UNSIGNED NULL DEFAULT NULL,
  `current_version_no` int UNSIGNED NOT NULL DEFAULT 1,
  `page_count` int UNSIGNED NULL DEFAULT NULL,
  `chunk_count` int UNSIGNED NOT NULL DEFAULT 0,
  `character_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `token_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `parse_status` tinyint NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
  `chunk_status` tinyint NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
  `embedding_status` tinyint NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
  `document_status` tinyint NOT NULL DEFAULT 0 COMMENT '0 processing, 1 available, 2 failed, 3 disabled',
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `metadata_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_document_uid`(`tenant_id` ASC, `document_uid` ASC) USING BTREE,
  INDEX `idx_kb_status`(`tenant_id` ASC, `knowledge_base_id` ASC, `document_status` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_kb_hash`(`knowledge_base_id` ASC, `file_hash` ASC) USING BTREE,
  INDEX `idx_embedding_status`(`embedding_status` ASC, `updated_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG document' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_document
-- ----------------------------
INSERT INTO `rag_document` VALUES (1, 0, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 1, 'file:///D:/code/langchain4j-milvus-rag-demo-v2/data/rag-objects/dev/0/1/doc_34823d93651b4511a251eb40608dd200/original/e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c/浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'dev/0/1/doc_34823d93651b4511a251eb40608dd200/original/e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c/浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'pdf', 'application/pdf', 331035, 'e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c', 1, 1, NULL, 5, 3817, 1907, 2, 2, 2, 1, NULL, NULL, '{}', '2026-07-27 22:58:55.723', '2026-07-27 22:59:04.644', 0);
INSERT INTO `rag_document` VALUES (2, 1, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 1, 'file:///D:/code/langchain4j-milvus-rag-demo-v2/data/rag-objects/dev/1/2/doc_d3ab4b2bd5cf408f9afa9c94b7e537c9/original/e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c/浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'dev/1/2/doc_d3ab4b2bd5cf408f9afa9c94b7e537c9/original/e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c/浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'pdf', 'application/pdf', 331035, 'e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c', 2, 1, NULL, 5, 3817, 1907, 2, 2, 2, 1, NULL, NULL, '{}', '2026-08-01 08:56:44.885', '2026-08-01 08:56:53.326', 0);

-- ----------------------------
-- Table structure for rag_document_chunk
-- ----------------------------
DROP TABLE IF EXISTS `rag_document_chunk`;
CREATE TABLE `rag_document_chunk`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Chunk ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NOT NULL,
  `document_id` bigint UNSIGNED NOT NULL,
  `source_document_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Logical/external document ID used by chunk APIs',
  `document_version_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `chunk_uid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `chunk_version` int UNSIGNED NOT NULL DEFAULT 1,
  `chunk_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, SUPERSEDED, DISABLED, DELETED',
  `is_current` tinyint NOT NULL DEFAULT 1,
  `chunk_index` int UNSIGNED NOT NULL,
  `parent_chunk_id` bigint UNSIGNED NULL DEFAULT NULL,
  `parent_chunk_uid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `source` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `content_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'text',
  `page_start` int UNSIGNED NULL DEFAULT NULL,
  `page_end` int UNSIGNED NULL DEFAULT NULL,
  `char_start` int UNSIGNED NULL DEFAULT NULL,
  `char_end` int UNSIGNED NULL DEFAULT NULL,
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `section_path` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_caption` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `permission_tags` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `tenant_external_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `content_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `character_count` int UNSIGNED NOT NULL DEFAULT 0,
  `token_count` int UNSIGNED NOT NULL DEFAULT 0,
  `vector_store_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'milvus',
  `vector_collection` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `milvus_alias` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `vector_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `text_vector_ids` json NULL,
  `image_vector_ids` json NULL,
  `embedding_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `embedding_dimension` int UNSIGNED NULL DEFAULT NULL,
  `embedding_status` tinyint NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
  `metadata_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_chunk_uid_version`(`tenant_id` ASC, `chunk_uid` ASC, `chunk_version` ASC) USING BTREE,
  INDEX `idx_document_chunk_index`(`document_id` ASC, `document_version_id` ASC, `chunk_index` ASC) USING BTREE,
  INDEX `idx_chunk_current`(`chunk_uid` ASC, `is_current` ASC, `chunk_status` ASC) USING BTREE,
  INDEX `idx_source_document`(`source_document_id` ASC, `is_current` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_kb_document`(`tenant_id` ASC, `knowledge_base_id` ASC, `document_id` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_content_hash`(`knowledge_base_id` ASC, `content_hash` ASC) USING BTREE,
  INDEX `idx_embedding_status`(`embedding_status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_vector_id`(`vector_collection` ASC, `vector_id` ASC) USING BTREE,
  INDEX `idx_store_current`(`milvus_alias` ASC, `vector_collection` ASC, `is_current` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG document chunk' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_document_chunk
-- ----------------------------
INSERT INTO `rag_document_chunk` VALUES (1, 0, 1, 1, 'doc_34823d93651b4511a251eb40608dd200', 1, 'doc_34823d93651b4511a251eb40608dd200-text-0', 1, 'ACTIVE', 1, 0, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作', '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、', '6a237106b9829f819a62500aeabc404ff39d5c4403299e6e586e9229cabaa9f0', 896, 448, 'milvus', 'demo_kb', 'default', 'c3f76c37-0d25-40e7-94af-916d1c06b6a1', '[\"c3f76c37-0d25-40e7-94af-916d1c06b6a1\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-07-27 22:59:03.393', '2026-07-27 22:59:03.393', 0);
INSERT INTO `rag_document_chunk` VALUES (2, 0, 1, 1, 'doc_34823d93651b4511a251eb40608dd200', 1, 'doc_34823d93651b4511a251eb40608dd200-text-1', 1, 'ACTIVE', 1, 1, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：', '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 1', 'e7d2a2e7709c589f38703e8bad4608df19bfd6f1fecc36b01d60ccb2ef4a374b', 879, 439, 'milvus', 'demo_kb', 'default', '3af21019-c7e0-42b5-bbef-f65b72917679', '[\"3af21019-c7e0-42b5-bbef-f65b72917679\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-07-27 22:59:03.595', '2026-07-27 22:59:03.595', 0);
INSERT INTO `rag_document_chunk` VALUES (3, 0, 1, 1, 'doc_34823d93651b4511a251eb40608dd200', 1, 'doc_34823d93651b4511a251eb40608dd200-text-2', 1, 'ACTIVE', 1, 2, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章', '0858e86227350c3b75032d7ae75dc4f780e1fe7f2ba6a63b142a29a075d3b7db', 887, 443, 'milvus', 'demo_kb', 'default', 'ceaaaaab-7673-4522-bff7-cdc7dba878db', '[\"ceaaaaab-7673-4522-bff7-cdc7dba878db\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-07-27 22:59:03.794', '2026-07-27 22:59:03.794', 0);
INSERT INTO `rag_document_chunk` VALUES (4, 0, 1, 1, 'doc_34823d93651b4511a251eb40608dd200', 1, 'doc_34823d93651b4511a251eb40608dd200-text-3', 1, 'ACTIVE', 1, 3, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工', 'daf94e278a068d58b912e166df31569be95bc542248e173174e64b16bb9d8117', 891, 445, 'milvus', 'demo_kb', 'default', 'fe43a55f-20f0-4091-bee4-f1369bb5400a', '[\"fe43a55f-20f0-4091-bee4-f1369bb5400a\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-07-27 22:59:04.043', '2026-07-27 22:59:04.043', 0);
INSERT INTO `rag_document_chunk` VALUES (5, 0, 1, 1, 'doc_34823d93651b4511a251eb40608dd200', 1, 'doc_34823d93651b4511a251eb40608dd200-text-4', 1, 'ACTIVE', 1, 4, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '7ef15d3661c63c32f02476830ec5dcb550092955bca551685091088fef2c9464', 264, 132, 'milvus', 'demo_kb', 'default', 'a719b8b9-653f-435a-9337-594db0fd3794', '[\"a719b8b9-653f-435a-9337-594db0fd3794\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-07-27 22:59:04.278', '2026-07-27 22:59:04.278', 0);
INSERT INTO `rag_document_chunk` VALUES (6, 0, 0, 0, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', 0, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0', 1, 'ACTIVE', 1, 0, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作', '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、', '6a237106b9829f819a62500aeabc404ff39d5c4403299e6e586e9229cabaa9f0', 896, 448, 'milvus', 'demo_kb_tenant_1', 'default', 'f0df9a1c-07ec-4b55-a9b6-2376404e7395', '[\"f0df9a1c-07ec-4b55-a9b6-2376404e7395\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-08-01 08:56:48.334', '2026-08-01 08:56:50.210', 0);
INSERT INTO `rag_document_chunk` VALUES (7, 0, 0, 0, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', 0, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-1', 1, 'ACTIVE', 1, 0, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：', '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 1', 'e7d2a2e7709c589f38703e8bad4608df19bfd6f1fecc36b01d60ccb2ef4a374b', 879, 439, 'milvus', 'demo_kb_tenant_1', 'default', 'ae2f3fd9-e2ba-4a73-8f72-210f05d9bd62', '[\"ae2f3fd9-e2ba-4a73-8f72-210f05d9bd62\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-08-01 08:56:48.365', '2026-08-01 08:56:50.675', 0);
INSERT INTO `rag_document_chunk` VALUES (8, 0, 0, 0, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', 0, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2', 1, 'ACTIVE', 1, 0, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章', '0858e86227350c3b75032d7ae75dc4f780e1fe7f2ba6a63b142a29a075d3b7db', 887, 443, 'milvus', 'demo_kb_tenant_1', 'default', '38aca5e6-3896-4bbc-bcbd-dfac68d1a017', '[\"38aca5e6-3896-4bbc-bcbd-dfac68d1a017\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-08-01 08:56:48.392', '2026-08-01 08:56:50.786', 0);
INSERT INTO `rag_document_chunk` VALUES (9, 0, 0, 0, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', 0, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3', 1, 'ACTIVE', 1, 0, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工', 'daf94e278a068d58b912e166df31569be95bc542248e173174e64b16bb9d8117', 891, 445, 'milvus', 'demo_kb_tenant_1', 'default', '6ea6ed8c-fc52-4f92-ab82-33b5a5a669b6', '[\"6ea6ed8c-fc52-4f92-ab82-33b5a5a669b6\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-08-01 08:56:48.419', '2026-08-01 08:56:50.961', 0);
INSERT INTO `rag_document_chunk` VALUES (10, 0, 0, 0, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', 0, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4', 1, 'ACTIVE', 1, 0, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '7ef15d3661c63c32f02476830ec5dcb550092955bca551685091088fef2c9464', 264, 132, 'milvus', 'demo_kb_tenant_1', 'default', 'd852cde3-7e8d-4054-925e-f11ef4c7e2f8', '[\"d852cde3-7e8d-4054-925e-f11ef4c7e2f8\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-08-01 08:56:48.444', '2026-08-01 08:56:51.071', 0);
INSERT INTO `rag_document_chunk` VALUES (11, 1, 2, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0', 1, 'ACTIVE', 1, 0, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作', '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、', '6a237106b9829f819a62500aeabc404ff39d5c4403299e6e586e9229cabaa9f0', 896, 448, 'milvus', 'demo_kb_tenant_1', 'default', 'f0df9a1c-07ec-4b55-a9b6-2376404e7395', '[\"f0df9a1c-07ec-4b55-a9b6-2376404e7395\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-08-01 08:56:51.624', '2026-08-01 08:56:51.624', 0);
INSERT INTO `rag_document_chunk` VALUES (12, 1, 2, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-1', 1, 'ACTIVE', 1, 1, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：', '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 1', 'e7d2a2e7709c589f38703e8bad4608df19bfd6f1fecc36b01d60ccb2ef4a374b', 879, 439, 'milvus', 'demo_kb_tenant_1', 'default', 'ae2f3fd9-e2ba-4a73-8f72-210f05d9bd62', '[\"ae2f3fd9-e2ba-4a73-8f72-210f05d9bd62\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-08-01 08:56:51.907', '2026-08-01 08:56:51.907', 0);
INSERT INTO `rag_document_chunk` VALUES (13, 1, 2, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2', 1, 'ACTIVE', 1, 2, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章', '0858e86227350c3b75032d7ae75dc4f780e1fe7f2ba6a63b142a29a075d3b7db', 887, 443, 'milvus', 'demo_kb_tenant_1', 'default', '38aca5e6-3896-4bbc-bcbd-dfac68d1a017', '[\"38aca5e6-3896-4bbc-bcbd-dfac68d1a017\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-08-01 08:56:52.194', '2026-08-01 08:56:52.194', 0);
INSERT INTO `rag_document_chunk` VALUES (14, 1, 2, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3', 1, 'ACTIVE', 1, 3, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工', 'daf94e278a068d58b912e166df31569be95bc542248e173174e64b16bb9d8117', 891, 445, 'milvus', 'demo_kb_tenant_1', 'default', '6ea6ed8c-fc52-4f92-ab82-33b5a5a669b6', '[\"6ea6ed8c-fc52-4f92-ab82-33b5a5a669b6\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-08-01 08:56:52.494', '2026-08-01 08:56:52.494', 0);
INSERT INTO `rag_document_chunk` VALUES (15, 1, 2, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4', 1, 'ACTIVE', 1, 4, NULL, NULL, 'file', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '', NULL, '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '7ef15d3661c63c32f02476830ec5dcb550092955bca551685091088fef2c9464', 264, 132, 'milvus', 'demo_kb_tenant_1', 'default', 'd852cde3-7e8d-4054-925e-f11ef4c7e2f8', '[\"d852cde3-7e8d-4054-925e-f11ef4c7e2f8\"]', '[]', 'text-embedding-v4', 1024, 2, '{}', '2026-08-01 08:56:52.828', '2026-08-01 08:56:52.828', 0);

-- ----------------------------
-- Table structure for rag_document_version
-- ----------------------------
DROP TABLE IF EXISTS `rag_document_version`;
CREATE TABLE `rag_document_version`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Document version ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NOT NULL,
  `document_id` bigint UNSIGNED NOT NULL,
  `version_no` int UNSIGNED NOT NULL,
  `version_uid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Stable document version UID',
  `document_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_type` tinyint NOT NULL DEFAULT 1,
  `source_uri` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `object_key` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `original_filename` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `file_extension` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `mime_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `file_size` bigint UNSIGNED NULL DEFAULT 0,
  `file_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `page_count` int UNSIGNED NULL DEFAULT NULL,
  `chunk_count` int UNSIGNED NOT NULL DEFAULT 0,
  `character_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `token_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `parse_status` tinyint NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
  `chunk_status` tinyint NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
  `embedding_status` tinyint NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed',
  `version_status` tinyint NOT NULL DEFAULT 0 COMMENT '0 processing, 1 available, 2 failed, 3 disabled, 4 deleted',
  `is_current` tinyint NOT NULL DEFAULT 0,
  `version_note` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Version note',
  `approval_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, PENDING_REVIEW, APPROVED, REJECTED, PUBLISHED',
  `approval_comment` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Approval comment',
  `approved_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Reviewer or operator',
  `approved_at` datetime(3) NULL DEFAULT NULL COMMENT 'Review timestamp',
  `published_at` datetime(3) NULL DEFAULT NULL COMMENT 'Publish timestamp',
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `metadata_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_document_version_no`(`document_id` ASC, `version_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_tenant_document_version_uid`(`tenant_id` ASC, `version_uid` ASC) USING BTREE,
  INDEX `idx_document_current`(`document_id` ASC, `is_current` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_kb_version_status`(`tenant_id` ASC, `knowledge_base_id` ASC, `version_status` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_version_approval`(`approval_status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_version_hash`(`knowledge_base_id` ASC, `file_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG document version' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_document_version
-- ----------------------------
INSERT INTO `rag_document_version` VALUES (1, 0, 1, 1, 1, 'ver_c0e50634e66446129c6a5882f982485b', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 1, 'file:///D:/code/langchain4j-milvus-rag-demo-v2/data/rag-objects/dev/0/1/doc_34823d93651b4511a251eb40608dd200/original/e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c/浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'dev/0/1/doc_34823d93651b4511a251eb40608dd200/original/e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c/浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'pdf', 'application/pdf', 331035, 'e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c', NULL, 5, 3817, 1907, 2, 2, 2, 1, 1, NULL, 'DRAFT', NULL, NULL, NULL, NULL, NULL, NULL, '{}', '2026-07-27 22:58:55.740', '2026-07-27 22:59:04.661', 0);
INSERT INTO `rag_document_version` VALUES (2, 1, 2, 2, 1, 'ver_98e8c516c3e54f7593927cb7023a6904', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 1, 'file:///D:/code/langchain4j-milvus-rag-demo-v2/data/rag-objects/dev/1/2/doc_d3ab4b2bd5cf408f9afa9c94b7e537c9/original/e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c/浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'dev/1/2/doc_d3ab4b2bd5cf408f9afa9c94b7e537c9/original/e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c/浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'pdf', 'application/pdf', 331035, 'e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c', NULL, 5, 3817, 1907, 2, 2, 2, 1, 1, NULL, 'DRAFT', NULL, NULL, NULL, NULL, NULL, NULL, '{}', '2026-08-01 08:56:44.928', '2026-08-01 08:56:53.349', 0);

-- ----------------------------
-- Table structure for rag_feedback_quality_metric_daily
-- ----------------------------
DROP TABLE IF EXISTS `rag_feedback_quality_metric_daily`;
CREATE TABLE `rag_feedback_quality_metric_daily`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `bucket_start` datetime(3) NOT NULL,
  `window_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HOUR',
  `knowledge_base_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `retrieval_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `feedback_rating` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `feedback_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `assignee` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `feedback_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `helpful_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `not_helpful_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `correction_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_feedback_quality_hourly`(`tenant_id` ASC, `bucket_start` ASC, `knowledge_base_id` ASC, `retrieval_mode` ASC, `query_type` ASC, `feedback_rating` ASC, `feedback_status` ASC, `assignee` ASC) USING BTREE,
  INDEX `idx_feedback_quality_hourly_bucket`(`bucket_start` ASC, `tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Hourly materialized feedback quality metrics' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_feedback_quality_metric_daily
-- ----------------------------

-- ----------------------------
-- Table structure for rag_feedback_quality_metric_hourly
-- ----------------------------
DROP TABLE IF EXISTS `rag_feedback_quality_metric_hourly`;
CREATE TABLE `rag_feedback_quality_metric_hourly`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `bucket_start` datetime(3) NOT NULL,
  `window_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HOUR',
  `knowledge_base_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `retrieval_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `feedback_rating` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `feedback_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `assignee` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `feedback_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `helpful_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `not_helpful_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `correction_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_feedback_quality_hourly`(`tenant_id` ASC, `bucket_start` ASC, `knowledge_base_id` ASC, `retrieval_mode` ASC, `query_type` ASC, `feedback_rating` ASC, `feedback_status` ASC, `assignee` ASC) USING BTREE,
  INDEX `idx_feedback_quality_hourly_bucket`(`bucket_start` ASC, `tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Hourly materialized feedback quality metrics' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_feedback_quality_metric_hourly
-- ----------------------------

-- ----------------------------
-- Table structure for rag_feedback_revision_task
-- ----------------------------
DROP TABLE IF EXISTS `rag_feedback_revision_task`;
CREATE TABLE `rag_feedback_revision_task`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Feedback revision task ID',
  `revision_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `feedback_id` bigint UNSIGNED NOT NULL,
  `query_log_id` bigint UNSIGNED NULL DEFAULT NULL,
  `tenant_id` bigint UNSIGNED NULL DEFAULT NULL,
  `knowledge_base_id` bigint UNSIGNED NULL DEFAULT NULL,
  `document_id` bigint UNSIGNED NULL DEFAULT NULL,
  `chunk_uid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `revision_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER',
  `revision_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PLANNED',
  `before_snapshot_json` json NULL,
  `after_snapshot_json` json NULL,
  `expected_fix` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `verification_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `verification_result_json` json NULL,
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `assignee` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_feedback_revision_no`(`revision_no` ASC) USING BTREE,
  INDEX `idx_feedback_revision_feedback`(`feedback_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_feedback_revision_status`(`revision_status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_feedback_revision_kb`(`tenant_id` ASC, `knowledge_base_id` ASC, `revision_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG feedback revision task' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_feedback_revision_task
-- ----------------------------

-- ----------------------------
-- Table structure for rag_image_asset
-- ----------------------------
DROP TABLE IF EXISTS `rag_image_asset`;
CREATE TABLE `rag_image_asset`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Image asset ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `document_id` bigint UNSIGNED NULL DEFAULT NULL,
  `document_version_id` bigint UNSIGNED NULL DEFAULT NULL,
  `source_document_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Logical document UID',
  `image_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Stable extracted image ID',
  `chunk_uid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Chunk UID generated from this image',
  `content_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'image',
  `asset_path` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `page_no` int UNSIGNED NULL DEFAULT NULL,
  `coordinate_json` json NULL,
  `section_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_caption` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ocr_text` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `ocr_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ocr_confidence` decimal(6, 5) NULL DEFAULT NULL,
  `ocr_provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ocr_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ocr_error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `visual_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EMPTY' COMMENT 'SUCCESS, INVALID, FAILED, EMPTY',
  `visual_schema_valid` tinyint NOT NULL DEFAULT 0,
  `visual_confidence` decimal(6, 5) NULL DEFAULT NULL,
  `visual_json` json NULL,
  `visual_schema_errors` json NULL,
  `text_vector_ids` json NULL,
  `image_vector_ids` json NULL,
  `image_embedding_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_embedding_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_embedding_dimension` int UNSIGNED NULL DEFAULT NULL,
  `image_embedding_error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_embedding_updated_at` datetime(3) NULL DEFAULT NULL,
  `review_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EMPTY',
  `review_comment` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `reviewed_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `reviewed_at` datetime(3) NULL DEFAULT NULL,
  `review_updated_visual_json` json NULL,
  `review_updated_ocr_text` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_image_id`(`tenant_id` ASC, `image_id` ASC) USING BTREE,
  INDEX `idx_image_asset_kb_doc`(`tenant_id` ASC, `knowledge_base_id` ASC, `source_document_id` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_image_asset_chunk`(`chunk_uid` ASC) USING BTREE,
  INDEX `idx_image_asset_status`(`visual_status` ASC, `visual_confidence` ASC) USING BTREE,
  INDEX `idx_image_asset_review`(`review_status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_image_asset_ocr`(`ocr_status` ASC, `ocr_confidence` ASC) USING BTREE,
  INDEX `idx_image_asset_embedding`(`image_embedding_status` ASC, `image_embedding_updated_at` ASC) USING BTREE,
  INDEX `idx_image_asset_updated`(`updated_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG image asset and multimodal governance metadata' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_image_asset
-- ----------------------------

-- ----------------------------
-- Table structure for rag_ingestion_task
-- ----------------------------
DROP TABLE IF EXISTS `rag_ingestion_task`;
CREATE TABLE `rag_ingestion_task`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Task ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NOT NULL,
  `document_id` bigint UNSIGNED NOT NULL,
  `document_version_id` bigint UNSIGNED NULL DEFAULT NULL,
  `task_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `task_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `task_status` tinyint NOT NULL DEFAULT 0 COMMENT '0 pending, 1 running, 2 success, 3 failed, 4 retry_wait, 5 cancelled, 6 partial_success',
  `progress` int UNSIGNED NOT NULL DEFAULT 0,
  `current_stage` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `stage_progress` int UNSIGNED NOT NULL DEFAULT 0,
  `total_count` int UNSIGNED NOT NULL DEFAULT 0,
  `success_count` int UNSIGNED NOT NULL DEFAULT 0,
  `failed_count` int UNSIGNED NOT NULL DEFAULT 0,
  `retry_count` int UNSIGNED NOT NULL DEFAULT 0,
  `max_retry_count` int UNSIGNED NOT NULL DEFAULT 3,
  `next_retry_at` datetime(3) NULL DEFAULT NULL,
  `cancel_requested` tinyint NOT NULL DEFAULT 0,
  `cancel_requested_at` datetime(3) NULL DEFAULT NULL,
  `cancel_requested_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `partial_success` tinyint NOT NULL DEFAULT 0,
  `last_event_id` bigint UNSIGNED NULL DEFAULT NULL,
  `heartbeat_at` datetime(3) NULL DEFAULT NULL,
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `idempotency_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `lock_version` bigint UNSIGNED NOT NULL DEFAULT 0,
  `started_at` datetime(3) NULL DEFAULT NULL,
  `finished_at` datetime(3) NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_task_no`(`tenant_id` ASC, `task_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_tenant_idempotency_key`(`tenant_id` ASC, `idempotency_key` ASC) USING BTREE,
  INDEX `idx_task_schedule`(`task_status` ASC, `next_retry_at` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_document_task`(`document_id` ASC, `task_type` ASC, `task_status` ASC) USING BTREE,
  INDEX `idx_task_stage`(`current_stage` ASC, `heartbeat_at` ASC) USING BTREE,
  INDEX `idx_trace_id`(`trace_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG ingestion task' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_ingestion_task
-- ----------------------------
INSERT INTO `rag_ingestion_task` VALUES (1, 0, 1, 1, 1, 'ing_a7437f76416f410d8a409c918e42f12a', 'PARSE', 2, 100, 'SYNC_KEYWORD_INDEX', 100, 5, 5, 0, 0, 3, NULL, 0, NULL, NULL, 0, 26, '2026-07-27 22:59:04.711', NULL, NULL, 'c302f82e0f4b45358f9b56755300dbc7', 'tenant:0:kb:1:doc:浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf:hash:e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c', 0, '2026-07-27 22:58:56.156', '2026-07-27 22:59:04.672', '2026-07-27 22:58:55.792', '2026-07-27 22:59:04.711');
INSERT INTO `rag_ingestion_task` VALUES (2, 1, 2, 2, 2, 'ing_4c6a5ce5e706468b806e31c5d800c596', 'PARSE', 2, 100, 'SYNC_KEYWORD_INDEX', 100, 5, 5, 0, 0, 3, NULL, 0, NULL, NULL, 0, 52, '2026-08-01 08:56:53.418', NULL, NULL, '2465c117c6d044c2a4eb6eb59d6444dc', 'tenant:1:kb:2:doc:浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf:hash:e5067a42170c3a70c0fb368aec31b5ecff7d0d11715ffac22790b7690f6adf2c', 0, '2026-08-01 08:56:45.379', '2026-08-01 08:56:53.374', '2026-08-01 08:56:44.987', '2026-08-01 08:56:53.418');

-- ----------------------------
-- Table structure for rag_ingestion_task_event
-- ----------------------------
DROP TABLE IF EXISTS `rag_ingestion_task_event`;
CREATE TABLE `rag_ingestion_task_event`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `task_id` bigint UNSIGNED NOT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `shard_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `progress` int UNSIGNED NULL DEFAULT NULL,
  `stage_progress` int UNSIGNED NULL DEFAULT NULL,
  `message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `payload_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_task`(`task_id` ASC, `id` ASC) USING BTREE,
  INDEX `idx_event_created`(`created_at` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 53 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG ingestion task event stream' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_ingestion_task_event
-- ----------------------------
INSERT INTO `rag_ingestion_task_event` VALUES (1, 0, 1, 'TASK_STARTED', NULL, NULL, 10, 0, 'Ingestion task started', NULL, '2026-07-27 22:58:56.189');
INSERT INTO `rag_ingestion_task_event` VALUES (2, 0, 1, 'STAGE_STARTED', 'OBJECT_READ', NULL, 0, 0, 'Read object file', NULL, '2026-07-27 22:58:56.387');
INSERT INTO `rag_ingestion_task_event` VALUES (3, 0, 1, 'STAGE_COMPLETED', 'OBJECT_READ', NULL, 5, 100, 'Read object file', NULL, '2026-07-27 22:58:56.506');
INSERT INTO `rag_ingestion_task_event` VALUES (4, 0, 1, 'STAGE_STARTED', 'PARSE_DOCUMENT', NULL, 5, 0, 'Parse document', NULL, '2026-07-27 22:58:56.679');
INSERT INTO `rag_ingestion_task_event` VALUES (5, 0, 1, 'STAGE_COMPLETED', 'PARSE_DOCUMENT', NULL, 20, 100, 'Parse document', NULL, '2026-07-27 22:58:59.326');
INSERT INTO `rag_ingestion_task_event` VALUES (6, 0, 1, 'STAGE_SKIPPED', 'EXTRACT_ASSETS', NULL, 30, 100, 'No image or table assets extracted', NULL, '2026-07-27 22:58:59.434');
INSERT INTO `rag_ingestion_task_event` VALUES (7, 0, 1, 'STAGE_SKIPPED', 'OCR_IMAGES', NULL, 40, 100, 'No image assets', NULL, '2026-07-27 22:58:59.524');
INSERT INTO `rag_ingestion_task_event` VALUES (8, 0, 1, 'STAGE_SKIPPED', 'VISION_ANALYSIS', NULL, 50, 100, 'No image assets', NULL, '2026-07-27 22:58:59.620');
INSERT INTO `rag_ingestion_task_event` VALUES (9, 0, 1, 'STAGE_SKIPPED', 'EMBED_IMAGE', NULL, 60, 100, 'No image assets', NULL, '2026-07-27 22:58:59.721');
INSERT INTO `rag_ingestion_task_event` VALUES (10, 0, 1, 'STAGE_STARTED', 'SPLIT_CHUNKS', NULL, 60, 0, 'Split chunks', NULL, '2026-07-27 22:58:59.836');
INSERT INTO `rag_ingestion_task_event` VALUES (11, 0, 1, 'STAGE_COMPLETED', 'SPLIT_CHUNKS', NULL, 70, 100, 'Split chunks', NULL, '2026-07-27 22:58:59.971');
INSERT INTO `rag_ingestion_task_event` VALUES (12, 0, 1, 'STAGE_STARTED', 'EMBED_TEXT', NULL, 70, 0, 'Embed text', NULL, '2026-07-27 22:59:00.193');
INSERT INTO `rag_ingestion_task_event` VALUES (13, 0, 1, 'STAGE_STARTED', 'WRITE_VECTOR', NULL, 70, 0, 'Write vectors', NULL, '2026-07-27 22:59:00.280');
INSERT INTO `rag_ingestion_task_event` VALUES (14, 0, 1, 'STAGE_PROGRESS', 'EMBED_TEXT', NULL, 85, 100, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 5, \"taskProgress\": 85}', '2026-07-27 22:59:00.790');
INSERT INTO `rag_ingestion_task_event` VALUES (15, 0, 1, 'STAGE_PROGRESS', 'WRITE_VECTOR', NULL, 95, 100, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 5, \"taskProgress\": 95}', '2026-07-27 22:59:00.989');
INSERT INTO `rag_ingestion_task_event` VALUES (16, 0, 1, 'STAGE_COMPLETED', 'EMBED_TEXT', NULL, 95, 100, 'Embed text', NULL, '2026-07-27 22:59:03.111');
INSERT INTO `rag_ingestion_task_event` VALUES (17, 0, 1, 'STAGE_COMPLETED', 'WRITE_VECTOR', NULL, 95, 100, 'Write vectors', NULL, '2026-07-27 22:59:03.227');
INSERT INTO `rag_ingestion_task_event` VALUES (18, 0, 1, 'STAGE_STARTED', 'PERSIST_METADATA', NULL, 95, 0, 'Persist metadata', NULL, '2026-07-27 22:59:03.328');
INSERT INTO `rag_ingestion_task_event` VALUES (19, 0, 1, 'STAGE_PROGRESS', 'PERSIST_METADATA', NULL, 96, 20, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 1, \"taskProgress\": 96}', '2026-07-27 22:59:03.529');
INSERT INTO `rag_ingestion_task_event` VALUES (20, 0, 1, 'STAGE_PROGRESS', 'PERSIST_METADATA', NULL, 96, 40, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 2, \"taskProgress\": 96}', '2026-07-27 22:59:03.695');
INSERT INTO `rag_ingestion_task_event` VALUES (21, 0, 1, 'STAGE_PROGRESS', 'PERSIST_METADATA', NULL, 97, 60, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 3, \"taskProgress\": 97}', '2026-07-27 22:59:03.961');
INSERT INTO `rag_ingestion_task_event` VALUES (22, 0, 1, 'STAGE_PROGRESS', 'PERSIST_METADATA', NULL, 97, 80, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 4, \"taskProgress\": 97}', '2026-07-27 22:59:04.216');
INSERT INTO `rag_ingestion_task_event` VALUES (23, 0, 1, 'STAGE_PROGRESS', 'PERSIST_METADATA', NULL, 98, 100, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 5, \"taskProgress\": 98}', '2026-07-27 22:59:04.426');
INSERT INTO `rag_ingestion_task_event` VALUES (24, 0, 1, 'STAGE_COMPLETED', 'PERSIST_METADATA', NULL, 98, 100, 'Persist metadata', NULL, '2026-07-27 22:59:04.511');
INSERT INTO `rag_ingestion_task_event` VALUES (25, 0, 1, 'STAGE_COMPLETED', 'SYNC_KEYWORD_INDEX', NULL, 100, 100, 'Sync keyword index', NULL, '2026-07-27 22:59:04.600');
INSERT INTO `rag_ingestion_task_event` VALUES (26, 0, 1, 'TASK_SUCCEEDED', NULL, NULL, 100, 100, 'Ingestion task succeeded', NULL, '2026-07-27 22:59:04.704');
INSERT INTO `rag_ingestion_task_event` VALUES (27, 1, 2, 'TASK_STARTED', NULL, NULL, 10, 0, 'Ingestion task started', NULL, '2026-08-01 08:56:45.428');
INSERT INTO `rag_ingestion_task_event` VALUES (28, 1, 2, 'STAGE_STARTED', 'OBJECT_READ', NULL, 0, 0, 'Read object file', NULL, '2026-08-01 08:56:45.656');
INSERT INTO `rag_ingestion_task_event` VALUES (29, 1, 2, 'STAGE_COMPLETED', 'OBJECT_READ', NULL, 5, 100, 'Read object file', NULL, '2026-08-01 08:56:45.781');
INSERT INTO `rag_ingestion_task_event` VALUES (30, 1, 2, 'STAGE_STARTED', 'PARSE_DOCUMENT', NULL, 5, 0, 'Parse document', NULL, '2026-08-01 08:56:45.954');
INSERT INTO `rag_ingestion_task_event` VALUES (31, 1, 2, 'STAGE_COMPLETED', 'PARSE_DOCUMENT', NULL, 20, 100, 'Parse document', NULL, '2026-08-01 08:56:47.367');
INSERT INTO `rag_ingestion_task_event` VALUES (32, 1, 2, 'STAGE_SKIPPED', 'EXTRACT_ASSETS', NULL, 30, 100, 'No image or table assets extracted', NULL, '2026-08-01 08:56:47.498');
INSERT INTO `rag_ingestion_task_event` VALUES (33, 1, 2, 'STAGE_SKIPPED', 'OCR_IMAGES', NULL, 40, 100, 'No image assets', NULL, '2026-08-01 08:56:47.667');
INSERT INTO `rag_ingestion_task_event` VALUES (34, 1, 2, 'STAGE_SKIPPED', 'VISION_ANALYSIS', NULL, 50, 100, 'No image assets', NULL, '2026-08-01 08:56:47.802');
INSERT INTO `rag_ingestion_task_event` VALUES (35, 1, 2, 'STAGE_SKIPPED', 'EMBED_IMAGE', NULL, 60, 100, 'No image assets', NULL, '2026-08-01 08:56:47.944');
INSERT INTO `rag_ingestion_task_event` VALUES (36, 1, 2, 'STAGE_STARTED', 'SPLIT_CHUNKS', NULL, 60, 0, 'Split chunks', NULL, '2026-08-01 08:56:48.095');
INSERT INTO `rag_ingestion_task_event` VALUES (37, 1, 2, 'STAGE_COMPLETED', 'SPLIT_CHUNKS', NULL, 70, 100, 'Split chunks', NULL, '2026-08-01 08:56:48.252');
INSERT INTO `rag_ingestion_task_event` VALUES (38, 1, 2, 'STAGE_STARTED', 'EMBED_TEXT', NULL, 70, 0, 'Embed text', NULL, '2026-08-01 08:56:48.534');
INSERT INTO `rag_ingestion_task_event` VALUES (39, 1, 2, 'STAGE_STARTED', 'WRITE_VECTOR', NULL, 70, 0, 'Write vectors', NULL, '2026-08-01 08:56:48.676');
INSERT INTO `rag_ingestion_task_event` VALUES (40, 1, 2, 'STAGE_PROGRESS', 'EMBED_TEXT', NULL, 85, 100, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 5, \"taskProgress\": 85}', '2026-08-01 08:56:49.854');
INSERT INTO `rag_ingestion_task_event` VALUES (41, 1, 2, 'STAGE_PROGRESS', 'WRITE_VECTOR', NULL, 95, 100, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 5, \"taskProgress\": 95}', '2026-08-01 08:56:50.135');
INSERT INTO `rag_ingestion_task_event` VALUES (42, 1, 2, 'STAGE_COMPLETED', 'EMBED_TEXT', NULL, 95, 100, 'Embed text', NULL, '2026-08-01 08:56:51.249');
INSERT INTO `rag_ingestion_task_event` VALUES (43, 1, 2, 'STAGE_COMPLETED', 'WRITE_VECTOR', NULL, 95, 100, 'Write vectors', NULL, '2026-08-01 08:56:51.375');
INSERT INTO `rag_ingestion_task_event` VALUES (44, 1, 2, 'STAGE_STARTED', 'PERSIST_METADATA', NULL, 95, 0, 'Persist metadata', NULL, '2026-08-01 08:56:51.523');
INSERT INTO `rag_ingestion_task_event` VALUES (45, 1, 2, 'STAGE_PROGRESS', 'PERSIST_METADATA', NULL, 96, 20, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 1, \"taskProgress\": 96}', '2026-08-01 08:56:51.818');
INSERT INTO `rag_ingestion_task_event` VALUES (46, 1, 2, 'STAGE_PROGRESS', 'PERSIST_METADATA', NULL, 96, 40, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 2, \"taskProgress\": 96}', '2026-08-01 08:56:52.099');
INSERT INTO `rag_ingestion_task_event` VALUES (47, 1, 2, 'STAGE_PROGRESS', 'PERSIST_METADATA', NULL, 97, 60, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 3, \"taskProgress\": 97}', '2026-08-01 08:56:52.389');
INSERT INTO `rag_ingestion_task_event` VALUES (48, 1, 2, 'STAGE_PROGRESS', 'PERSIST_METADATA', NULL, 97, 80, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 4, \"taskProgress\": 97}', '2026-08-01 08:56:52.685');
INSERT INTO `rag_ingestion_task_event` VALUES (49, 1, 2, 'STAGE_PROGRESS', 'PERSIST_METADATA', NULL, 98, 100, 'Stage progress updated', '{\"totalCount\": 5, \"failedCount\": 0, \"successCount\": 5, \"taskProgress\": 98}', '2026-08-01 08:56:52.990');
INSERT INTO `rag_ingestion_task_event` VALUES (50, 1, 2, 'STAGE_COMPLETED', 'PERSIST_METADATA', NULL, 98, 100, 'Persist metadata', NULL, '2026-08-01 08:56:53.121');
INSERT INTO `rag_ingestion_task_event` VALUES (51, 1, 2, 'STAGE_COMPLETED', 'SYNC_KEYWORD_INDEX', NULL, 100, 100, 'Sync keyword index', NULL, '2026-08-01 08:56:53.259');
INSERT INTO `rag_ingestion_task_event` VALUES (52, 1, 2, 'TASK_SUCCEEDED', NULL, NULL, 100, 100, 'Ingestion task succeeded', NULL, '2026-08-01 08:56:53.405');

-- ----------------------------
-- Table structure for rag_ingestion_task_shard
-- ----------------------------
DROP TABLE IF EXISTS `rag_ingestion_task_shard`;
CREATE TABLE `rag_ingestion_task_shard`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `task_id` bigint UNSIGNED NOT NULL,
  `stage_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `document_id` bigint UNSIGNED NULL DEFAULT NULL,
  `document_version_id` bigint UNSIGNED NULL DEFAULT NULL,
  `shard_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `shard_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `shard_index` int UNSIGNED NULL DEFAULT NULL,
  `shard_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `retry_count` int UNSIGNED NOT NULL DEFAULT 0,
  `max_retry_count` int UNSIGNED NOT NULL DEFAULT 3,
  `next_retry_at` datetime(3) NULL DEFAULT NULL,
  `error_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `input_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `output_ref` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `metadata_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_shard`(`task_id` ASC, `shard_key` ASC, `shard_type` ASC) USING BTREE,
  INDEX `idx_shard_retry`(`shard_status` ASC, `next_retry_at` ASC) USING BTREE,
  INDEX `idx_shard_document`(`document_id` ASC, `document_version_id` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG ingestion task shard progress' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_ingestion_task_shard
-- ----------------------------
INSERT INTO `rag_ingestion_task_shard` VALUES (1, 0, 1, 'PERSIST_METADATA', 1, 1, 'doc_34823d93651b4511a251eb40608dd200-text-0', 'TEXT_CHUNK', 0, 'SUCCESS', 0, 3, NULL, NULL, NULL, '6a237106b9829f819a62500aeabc404ff39d5c4403299e6e586e9229cabaa9f0', 'c3f76c37-0d25-40e7-94af-916d1c06b6a1', NULL, '2026-07-27 22:59:03.385', '2026-07-27 22:59:03.467');
INSERT INTO `rag_ingestion_task_shard` VALUES (2, 0, 1, 'PERSIST_METADATA', 1, 1, 'doc_34823d93651b4511a251eb40608dd200-text-1', 'TEXT_CHUNK', 1, 'SUCCESS', 0, 3, NULL, NULL, NULL, 'e7d2a2e7709c589f38703e8bad4608df19bfd6f1fecc36b01d60ccb2ef4a374b', '3af21019-c7e0-42b5-bbef-f65b72917679', NULL, '2026-07-27 22:59:03.578', '2026-07-27 22:59:03.646');
INSERT INTO `rag_ingestion_task_shard` VALUES (3, 0, 1, 'PERSIST_METADATA', 1, 1, 'doc_34823d93651b4511a251eb40608dd200-text-2', 'TEXT_CHUNK', 2, 'SUCCESS', 0, 3, NULL, NULL, NULL, '0858e86227350c3b75032d7ae75dc4f780e1fe7f2ba6a63b142a29a075d3b7db', 'ceaaaaab-7673-4522-bff7-cdc7dba878db', NULL, '2026-07-27 22:59:03.769', '2026-07-27 22:59:03.869');
INSERT INTO `rag_ingestion_task_shard` VALUES (4, 0, 1, 'PERSIST_METADATA', 1, 1, 'doc_34823d93651b4511a251eb40608dd200-text-3', 'TEXT_CHUNK', 3, 'SUCCESS', 0, 3, NULL, NULL, NULL, 'daf94e278a068d58b912e166df31569be95bc542248e173174e64b16bb9d8117', 'fe43a55f-20f0-4091-bee4-f1369bb5400a', NULL, '2026-07-27 22:59:04.019', '2026-07-27 22:59:04.125');
INSERT INTO `rag_ingestion_task_shard` VALUES (5, 0, 1, 'PERSIST_METADATA', 1, 1, 'doc_34823d93651b4511a251eb40608dd200-text-4', 'TEXT_CHUNK', 4, 'SUCCESS', 0, 3, NULL, NULL, NULL, '7ef15d3661c63c32f02476830ec5dcb550092955bca551685091088fef2c9464', 'a719b8b9-653f-435a-9337-594db0fd3794', NULL, '2026-07-27 22:59:04.262', '2026-07-27 22:59:04.361');
INSERT INTO `rag_ingestion_task_shard` VALUES (6, 1, 2, 'PERSIST_METADATA', 2, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0', 'TEXT_CHUNK', 0, 'SUCCESS', 0, 3, NULL, NULL, NULL, '6a237106b9829f819a62500aeabc404ff39d5c4403299e6e586e9229cabaa9f0', 'f0df9a1c-07ec-4b55-a9b6-2376404e7395', NULL, '2026-08-01 08:56:51.592', '2026-08-01 08:56:51.710');
INSERT INTO `rag_ingestion_task_shard` VALUES (7, 1, 2, 'PERSIST_METADATA', 2, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-1', 'TEXT_CHUNK', 1, 'SUCCESS', 0, 3, NULL, NULL, NULL, 'e7d2a2e7709c589f38703e8bad4608df19bfd6f1fecc36b01d60ccb2ef4a374b', 'ae2f3fd9-e2ba-4a73-8f72-210f05d9bd62', NULL, '2026-08-01 08:56:51.880', '2026-08-01 08:56:51.986');
INSERT INTO `rag_ingestion_task_shard` VALUES (8, 1, 2, 'PERSIST_METADATA', 2, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2', 'TEXT_CHUNK', 2, 'SUCCESS', 0, 3, NULL, NULL, NULL, '0858e86227350c3b75032d7ae75dc4f780e1fe7f2ba6a63b142a29a075d3b7db', '38aca5e6-3896-4bbc-bcbd-dfac68d1a017', NULL, '2026-08-01 08:56:52.170', '2026-08-01 08:56:52.278');
INSERT INTO `rag_ingestion_task_shard` VALUES (9, 1, 2, 'PERSIST_METADATA', 2, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3', 'TEXT_CHUNK', 3, 'SUCCESS', 0, 3, NULL, NULL, NULL, 'daf94e278a068d58b912e166df31569be95bc542248e173174e64b16bb9d8117', '6ea6ed8c-fc52-4f92-ab82-33b5a5a669b6', NULL, '2026-08-01 08:56:52.465', '2026-08-01 08:56:52.572');
INSERT INTO `rag_ingestion_task_shard` VALUES (10, 1, 2, 'PERSIST_METADATA', 2, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4', 'TEXT_CHUNK', 4, 'SUCCESS', 0, 3, NULL, NULL, NULL, '7ef15d3661c63c32f02476830ec5dcb550092955bca551685091088fef2c9464', 'd852cde3-7e8d-4054-925e-f11ef4c7e2f8', NULL, '2026-08-01 08:56:52.792', '2026-08-01 08:56:52.897');

-- ----------------------------
-- Table structure for rag_ingestion_task_stage
-- ----------------------------
DROP TABLE IF EXISTS `rag_ingestion_task_stage`;
CREATE TABLE `rag_ingestion_task_stage`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `task_id` bigint UNSIGNED NOT NULL,
  `stage_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage_order` int UNSIGNED NOT NULL DEFAULT 0,
  `stage_weight` int UNSIGNED NOT NULL DEFAULT 0,
  `stage_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `progress` int UNSIGNED NOT NULL DEFAULT 0,
  `total_count` int UNSIGNED NOT NULL DEFAULT 0,
  `success_count` int UNSIGNED NOT NULL DEFAULT 0,
  `failed_count` int UNSIGNED NOT NULL DEFAULT 0,
  `started_at` datetime(3) NULL DEFAULT NULL,
  `finished_at` datetime(3) NULL DEFAULT NULL,
  `error_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `metadata_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_stage`(`task_id` ASC, `stage_code` ASC) USING BTREE,
  INDEX `idx_stage_status`(`stage_status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG ingestion task stage progress' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_ingestion_task_stage
-- ----------------------------
INSERT INTO `rag_ingestion_task_stage` VALUES (1, 0, 1, 'OBJECT_READ', 'Read object file', 10, 5, 'SUCCESS', 100, 1, 1, 0, '2026-07-27 22:58:56.312', '2026-07-27 22:58:56.452', NULL, NULL, NULL, '2026-07-27 22:58:55.873', '2026-07-27 22:58:56.452');
INSERT INTO `rag_ingestion_task_stage` VALUES (2, 0, 1, 'PARSE_DOCUMENT', 'Parse document', 20, 15, 'SUCCESS', 100, 1, 1, 0, '2026-07-27 22:58:56.606', '2026-07-27 22:58:59.269', NULL, NULL, NULL, '2026-07-27 22:58:55.873', '2026-07-27 22:58:59.269');
INSERT INTO `rag_ingestion_task_stage` VALUES (3, 0, 1, 'EXTRACT_ASSETS', 'Extract assets', 30, 10, 'SKIPPED', 100, 0, 0, 0, '2026-07-27 22:58:59.381', '2026-07-27 22:58:59.381', NULL, NULL, '{\"reason\": \"No image or table assets extracted\"}', '2026-07-27 22:58:55.873', '2026-07-27 22:58:59.381');
INSERT INTO `rag_ingestion_task_stage` VALUES (4, 0, 1, 'SPLIT_CHUNKS', 'Split chunks', 40, 10, 'SUCCESS', 100, 5, 5, 0, '2026-07-27 22:58:59.783', '2026-07-27 22:58:59.904', NULL, NULL, NULL, '2026-07-27 22:58:55.873', '2026-07-27 22:58:59.904');
INSERT INTO `rag_ingestion_task_stage` VALUES (5, 0, 1, 'OCR_IMAGES', 'OCR images', 50, 10, 'SKIPPED', 100, 0, 0, 0, '2026-07-27 22:58:59.470', '2026-07-27 22:58:59.470', NULL, NULL, '{\"reason\": \"No image assets\"}', '2026-07-27 22:58:55.873', '2026-07-27 22:58:59.470');
INSERT INTO `rag_ingestion_task_stage` VALUES (6, 0, 1, 'VISION_ANALYSIS', 'Vision analysis', 60, 10, 'SKIPPED', 100, 0, 0, 0, '2026-07-27 22:58:59.561', '2026-07-27 22:58:59.561', NULL, NULL, '{\"reason\": \"No image assets\"}', '2026-07-27 22:58:55.873', '2026-07-27 22:58:59.561');
INSERT INTO `rag_ingestion_task_stage` VALUES (7, 0, 1, 'EMBED_TEXT', 'Embed text', 70, 15, 'SUCCESS', 100, 5, 5, 0, '2026-07-27 22:59:00.111', '2026-07-27 22:59:03.061', NULL, NULL, NULL, '2026-07-27 22:58:55.873', '2026-07-27 22:59:03.061');
INSERT INTO `rag_ingestion_task_stage` VALUES (8, 0, 1, 'EMBED_IMAGE', 'Embed images', 80, 10, 'SKIPPED', 100, 0, 0, 0, '2026-07-27 22:58:59.659', '2026-07-27 22:58:59.659', NULL, NULL, '{\"reason\": \"No image assets\"}', '2026-07-27 22:58:55.873', '2026-07-27 22:58:59.659');
INSERT INTO `rag_ingestion_task_stage` VALUES (9, 0, 1, 'WRITE_VECTOR', 'Write vectors', 90, 10, 'SUCCESS', 100, 5, 5, 0, '2026-07-27 22:59:00.232', '2026-07-27 22:59:03.184', NULL, NULL, NULL, '2026-07-27 22:58:55.873', '2026-07-27 22:59:03.184');
INSERT INTO `rag_ingestion_task_stage` VALUES (10, 0, 1, 'PERSIST_METADATA', 'Persist metadata', 100, 3, 'SUCCESS', 100, 5, 5, 0, '2026-07-27 22:59:03.307', '2026-07-27 22:59:04.461', NULL, NULL, NULL, '2026-07-27 22:58:55.873', '2026-07-27 22:59:04.461');
INSERT INTO `rag_ingestion_task_stage` VALUES (11, 0, 1, 'SYNC_KEYWORD_INDEX', 'Sync keyword index', 110, 2, 'SUCCESS', 100, 5, 5, 0, '2026-07-27 22:59:04.551', '2026-07-27 22:59:04.551', NULL, NULL, NULL, '2026-07-27 22:58:55.873', '2026-07-27 22:59:04.551');
INSERT INTO `rag_ingestion_task_stage` VALUES (12, 1, 2, 'OBJECT_READ', 'Read object file', 10, 5, 'SUCCESS', 100, 1, 1, 0, '2026-08-01 08:56:45.572', '2026-08-01 08:56:45.702', NULL, NULL, NULL, '2026-08-01 08:56:45.070', '2026-08-01 08:56:45.702');
INSERT INTO `rag_ingestion_task_stage` VALUES (13, 1, 2, 'PARSE_DOCUMENT', 'Parse document', 20, 15, 'SUCCESS', 100, 1, 1, 0, '2026-08-01 08:56:45.881', '2026-08-01 08:56:47.299', NULL, NULL, NULL, '2026-08-01 08:56:45.070', '2026-08-01 08:56:47.299');
INSERT INTO `rag_ingestion_task_stage` VALUES (14, 1, 2, 'EXTRACT_ASSETS', 'Extract assets', 30, 10, 'SKIPPED', 100, 0, 0, 0, '2026-08-01 08:56:47.425', '2026-08-01 08:56:47.425', NULL, NULL, '{\"reason\": \"No image or table assets extracted\"}', '2026-08-01 08:56:45.070', '2026-08-01 08:56:47.425');
INSERT INTO `rag_ingestion_task_stage` VALUES (15, 1, 2, 'SPLIT_CHUNKS', 'Split chunks', 40, 10, 'SUCCESS', 100, 5, 5, 0, '2026-08-01 08:56:48.025', '2026-08-01 08:56:48.174', NULL, NULL, NULL, '2026-08-01 08:56:45.070', '2026-08-01 08:56:48.174');
INSERT INTO `rag_ingestion_task_stage` VALUES (16, 1, 2, 'OCR_IMAGES', 'OCR images', 50, 10, 'SKIPPED', 100, 0, 0, 0, '2026-08-01 08:56:47.589', '2026-08-01 08:56:47.589', NULL, NULL, '{\"reason\": \"No image assets\"}', '2026-08-01 08:56:45.070', '2026-08-01 08:56:47.589');
INSERT INTO `rag_ingestion_task_stage` VALUES (17, 1, 2, 'VISION_ANALYSIS', 'Vision analysis', 60, 10, 'SKIPPED', 100, 0, 0, 0, '2026-08-01 08:56:47.723', '2026-08-01 08:56:47.723', NULL, NULL, '{\"reason\": \"No image assets\"}', '2026-08-01 08:56:45.070', '2026-08-01 08:56:47.723');
INSERT INTO `rag_ingestion_task_stage` VALUES (18, 1, 2, 'EMBED_TEXT', 'Embed text', 70, 15, 'SUCCESS', 100, 5, 5, 0, '2026-08-01 08:56:48.456', '2026-08-01 08:56:51.186', NULL, NULL, NULL, '2026-08-01 08:56:45.070', '2026-08-01 08:56:51.186');
INSERT INTO `rag_ingestion_task_stage` VALUES (19, 1, 2, 'EMBED_IMAGE', 'Embed images', 80, 10, 'SKIPPED', 100, 0, 0, 0, '2026-08-01 08:56:47.857', '2026-08-01 08:56:47.857', NULL, NULL, '{\"reason\": \"No image assets\"}', '2026-08-01 08:56:45.070', '2026-08-01 08:56:47.857');
INSERT INTO `rag_ingestion_task_stage` VALUES (20, 1, 2, 'WRITE_VECTOR', 'Write vectors', 90, 10, 'SUCCESS', 100, 5, 5, 0, '2026-08-01 08:56:48.599', '2026-08-01 08:56:51.302', NULL, NULL, NULL, '2026-08-01 08:56:45.070', '2026-08-01 08:56:51.302');
INSERT INTO `rag_ingestion_task_stage` VALUES (21, 1, 2, 'PERSIST_METADATA', 'Persist metadata', 100, 3, 'SUCCESS', 100, 5, 5, 0, '2026-08-01 08:56:51.446', '2026-08-01 08:56:53.051', NULL, NULL, NULL, '2026-08-01 08:56:45.070', '2026-08-01 08:56:53.051');
INSERT INTO `rag_ingestion_task_stage` VALUES (22, 1, 2, 'SYNC_KEYWORD_INDEX', 'Sync keyword index', 110, 2, 'SUCCESS', 100, 5, 5, 0, '2026-08-01 08:56:53.174', '2026-08-01 08:56:53.174', NULL, NULL, NULL, '2026-08-01 08:56:45.070', '2026-08-01 08:56:53.174');

-- ----------------------------
-- Table structure for rag_keyword_reindex_job
-- ----------------------------
DROP TABLE IF EXISTS `rag_keyword_reindex_job`;
CREATE TABLE `rag_keyword_reindex_job`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `job_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_index` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_index` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `alias_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `job_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PLANNED',
  `progress` int UNSIGNED NOT NULL DEFAULT 0,
  `total_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `success_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `failed_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `sample_validation_json` json NULL,
  `rollback_target` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `started_at` datetime(3) NULL DEFAULT NULL,
  `finished_at` datetime(3) NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_keyword_reindex_job_no`(`job_no` ASC) USING BTREE,
  INDEX `idx_keyword_reindex_tenant_status`(`tenant_id` ASC, `job_status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_keyword_reindex_alias`(`alias_name` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Keyword index reindex orchestration job' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_keyword_reindex_job
-- ----------------------------

-- ----------------------------
-- Table structure for rag_knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `rag_knowledge_base`;
CREATE TABLE `rag_knowledge_base`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Knowledge base ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `kb_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Knowledge base code',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Knowledge base name',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Description',
  `vector_store_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'milvus' COMMENT 'Vector store type',
  `vector_collection` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Vector collection',
  `embedding_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Embedding model',
  `embedding_dimension` int UNSIGNED NOT NULL COMMENT 'Embedding dimension',
  `chunk_strategy` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'recursive' COMMENT 'Chunk strategy',
  `chunk_size` int UNSIGNED NOT NULL DEFAULT 900 COMMENT 'Chunk size',
  `chunk_overlap` int UNSIGNED NOT NULL DEFAULT 120 COMMENT 'Chunk overlap',
  `retrieval_top_k` int UNSIGNED NOT NULL DEFAULT 6 COMMENT 'Default top k',
  `min_score` decimal(8, 6) NULL DEFAULT 0.550000 COMMENT 'Minimum score',
  `config_json` json NULL COMMENT 'Extended config',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0 disabled, 1 enabled',
  `lock_version` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_kb_code`(`tenant_id` ASC, `kb_code` ASC) USING BTREE,
  UNIQUE INDEX `uk_vector_collection`(`vector_collection` ASC) USING BTREE,
  INDEX `idx_tenant_status`(`tenant_id` ASC, `status` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG knowledge base' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_knowledge_base
-- ----------------------------
INSERT INTO `rag_knowledge_base` VALUES (1, 0, 'default', 'default', NULL, 'milvus', 'default', 'text-embedding-v4', 1024, 'recursive', 900, 120, 6, 0.550000, '{}', 1, 0, '2026-07-27 22:56:11.929', '2026-07-27 22:56:11.929', 0);
INSERT INTO `rag_knowledge_base` VALUES (2, 1, 'default', 'Default Knowledge Base', 'Default RAG knowledge base', 'milvus', 'demo_kb_tenant_1', 'text-embedding-v4', 1024, 'recursive', 900, 120, 6, 0.550000, '{}', 1, 0, '2026-07-31 21:35:55.244', '2026-07-31 21:35:55.244', 0);

-- ----------------------------
-- Table structure for rag_knowledge_base_member
-- ----------------------------
DROP TABLE IF EXISTS `rag_knowledge_base_member`;
CREATE TABLE `rag_knowledge_base_member`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NOT NULL,
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `member_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'READER',
  `permission_tags` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_kb_member`(`tenant_id` ASC, `knowledge_base_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_kb_member_user`(`tenant_id` ASC, `user_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Knowledge base member' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_knowledge_base_member
-- ----------------------------

-- ----------------------------
-- Table structure for rag_metric_aggregation_watermark
-- ----------------------------
DROP TABLE IF EXISTS `rag_metric_aggregation_watermark`;
CREATE TABLE `rag_metric_aggregation_watermark`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `metric_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `window_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `watermark_at` datetime(3) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_metric_watermark`(`metric_name` ASC, `window_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Materialized metric aggregation watermark' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_metric_aggregation_watermark
-- ----------------------------

-- ----------------------------
-- Table structure for rag_model_pricing
-- ----------------------------
DROP TABLE IF EXISTS `rag_model_pricing`;
CREATE TABLE `rag_model_pricing`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Model pricing ID',
  `provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `input_cost_per_1k_tokens` decimal(18, 8) NOT NULL DEFAULT 0.00000000,
  `output_cost_per_1k_tokens` decimal(18, 8) NOT NULL DEFAULT 0.00000000,
  `currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USD',
  `effective_from` datetime(3) NULL DEFAULT NULL,
  `effective_to` datetime(3) NULL DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_model_pricing_effective`(`provider` ASC, `model` ASC, `enabled` ASC, `effective_from` ASC, `effective_to` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG model token pricing' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_model_pricing
-- ----------------------------

-- ----------------------------
-- Table structure for rag_query_cost_anomaly
-- ----------------------------
DROP TABLE IF EXISTS `rag_query_cost_anomaly`;
CREATE TABLE `rag_query_cost_anomaly`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Cost anomaly ID',
  `tenant_id` bigint UNSIGNED NULL DEFAULT NULL,
  `anomaly_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `severity` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LOW',
  `metric_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `metric_value` decimal(18, 6) NULL DEFAULT NULL,
  `baseline_value` decimal(18, 6) NULL DEFAULT NULL,
  `window_start` datetime(3) NULL DEFAULT NULL,
  `window_end` datetime(3) NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN',
  `metadata_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_query_cost_anomaly`(`tenant_id` ASC, `status` ASC, `severity` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG query cost anomaly' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_query_cost_anomaly
-- ----------------------------

-- ----------------------------
-- Table structure for rag_query_cost_metric_daily
-- ----------------------------
DROP TABLE IF EXISTS `rag_query_cost_metric_daily`;
CREATE TABLE `rag_query_cost_metric_daily`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `bucket_start` datetime(3) NOT NULL,
  `window_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HOUR',
  `query_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `retrieval_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `llm_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `embedding_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `success_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `failed_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `prompt_tokens` bigint UNSIGNED NOT NULL DEFAULT 0,
  `completion_tokens` bigint UNSIGNED NOT NULL DEFAULT 0,
  `total_tokens` bigint UNSIGNED NOT NULL DEFAULT 0,
  `estimated_total_cost` decimal(18, 8) NOT NULL DEFAULT 0.00000000,
  `p50_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p90_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p95_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p99_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_query_cost_hourly`(`tenant_id` ASC, `knowledge_base_id` ASC, `bucket_start` ASC, `query_type` ASC, `retrieval_mode` ASC, `llm_model` ASC, `embedding_model` ASC, `status` ASC) USING BTREE,
  INDEX `idx_query_cost_hourly_bucket`(`bucket_start` ASC, `tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Hourly materialized query cost metrics' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_query_cost_metric_daily
-- ----------------------------

-- ----------------------------
-- Table structure for rag_query_cost_metric_hourly
-- ----------------------------
DROP TABLE IF EXISTS `rag_query_cost_metric_hourly`;
CREATE TABLE `rag_query_cost_metric_hourly`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `bucket_start` datetime(3) NOT NULL,
  `window_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HOUR',
  `query_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `retrieval_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `llm_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `embedding_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `success_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `failed_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `prompt_tokens` bigint UNSIGNED NOT NULL DEFAULT 0,
  `completion_tokens` bigint UNSIGNED NOT NULL DEFAULT 0,
  `total_tokens` bigint UNSIGNED NOT NULL DEFAULT 0,
  `estimated_total_cost` decimal(18, 8) NOT NULL DEFAULT 0.00000000,
  `p50_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p90_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p95_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p99_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_query_cost_hourly`(`tenant_id` ASC, `knowledge_base_id` ASC, `bucket_start` ASC, `query_type` ASC, `retrieval_mode` ASC, `llm_model` ASC, `embedding_model` ASC, `status` ASC) USING BTREE,
  INDEX `idx_query_cost_hourly_bucket`(`bucket_start` ASC, `tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Hourly materialized query cost metrics' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_query_cost_metric_hourly
-- ----------------------------

-- ----------------------------
-- Table structure for rag_query_feedback
-- ----------------------------
DROP TABLE IF EXISTS `rag_query_feedback`;
CREATE TABLE `rag_query_feedback`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Query feedback ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `query_log_id` bigint UNSIGNED NOT NULL,
  `rating` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HELPFUL, NOT_HELPFUL, CORRECTION',
  `created_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `comment` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `corrected_answer` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `feedback_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN',
  `priority` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MEDIUM',
  `assignee` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `review_result` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `review_comment` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `resolved_at` datetime(3) NULL DEFAULT NULL,
  `closed_at` datetime(3) NULL DEFAULT NULL,
  `reopened_count` int NOT NULL DEFAULT 0,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_query_feedback_log`(`query_log_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_query_feedback_rating`(`rating` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_feedback_status`(`feedback_status` ASC, `priority` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_feedback_assignee`(`assignee` ASC, `feedback_status` ASC, `updated_at` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG query feedback' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_query_feedback
-- ----------------------------

-- ----------------------------
-- Table structure for rag_query_feedback_event
-- ----------------------------
DROP TABLE IF EXISTS `rag_query_feedback_event`;
CREATE TABLE `rag_query_feedback_event`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Feedback event ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `feedback_id` bigint UNSIGNED NOT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `from_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `to_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `operator` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `comment` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `payload_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_feedback_event`(`feedback_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_feedback_event_type`(`event_type` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG query feedback event' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_query_feedback_event
-- ----------------------------

-- ----------------------------
-- Table structure for rag_query_hit
-- ----------------------------
DROP TABLE IF EXISTS `rag_query_hit`;
CREATE TABLE `rag_query_hit`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Query hit ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `query_log_id` bigint UNSIGNED NOT NULL,
  `rank_no` int UNSIGNED NOT NULL,
  `score` decimal(12, 10) NULL DEFAULT NULL,
  `knowledge_base_id` bigint UNSIGNED NULL DEFAULT NULL,
  `document_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `document_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `chunk_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `chunk_version` int UNSIGNED NULL DEFAULT NULL,
  `content_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `modality` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `retrieval_source` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_asset_id` bigint UNSIGNED NULL DEFAULT NULL,
  `fusion_score` decimal(12, 10) NULL DEFAULT NULL,
  `page_no` int UNSIGNED NULL DEFAULT NULL,
  `section_title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `image_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `content_snippet` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `metadata_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_query_hit_log`(`query_log_id` ASC, `rank_no` ASC) USING BTREE,
  INDEX `idx_query_hit_chunk`(`chunk_id` ASC) USING BTREE,
  INDEX `idx_query_hit_document`(`document_id` ASC) USING BTREE,
  INDEX `idx_query_hit_multimodal`(`modality` ASC, `retrieval_source` ASC, `image_asset_id` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG query retrieval hit' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_query_hit
-- ----------------------------
INSERT INTO `rag_query_hit` VALUES (1, 0, 5, 1, 0.8743108809, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-1', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：', '{\"index\": \"1\", \"source\": \"file\", \"chunkId\": \"1\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-1\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 1.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-27 23:00:51.720');
INSERT INTO `rag_query_hit` VALUES (2, 0, 5, 2, 0.8656617701, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-0', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作', '{\"index\": \"0\", \"source\": \"file\", \"chunkId\": \"0\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-0\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 0.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-27 23:00:51.733');
INSERT INTO `rag_query_hit` VALUES (3, 0, 5, 3, 0.8359001577, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-3', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '{\"index\": \"3\", \"source\": \"file\", \"chunkId\": \"3\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-3\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 3.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-27 23:00:51.755');
INSERT INTO `rag_query_hit` VALUES (4, 0, 5, 4, 0.8332695365, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 2.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-27 23:00:51.768');
INSERT INTO `rag_query_hit` VALUES (5, 0, 5, 5, 0.8255413175, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-4', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '{\"index\": \"4\", \"source\": \"file\", \"chunkId\": \"4\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-4\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 4.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-27 23:00:51.785');
INSERT INTO `rag_query_hit` VALUES (6, 0, 7, 1, 0.6107255667, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 2.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 20:25:02.625');
INSERT INTO `rag_query_hit` VALUES (7, 0, 8, 1, 0.6113253683, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 2.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 20:25:20.951');
INSERT INTO `rag_query_hit` VALUES (8, 0, 9, 1, 0.6216341183, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 2.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 20:26:01.295');
INSERT INTO `rag_query_hit` VALUES (9, 0, 9, 2, 0.6051971242, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-1', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：', '{\"index\": \"1\", \"source\": \"file\", \"chunkId\": \"1\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-1\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 1.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 20:26:01.309');
INSERT INTO `rag_query_hit` VALUES (10, 0, 9, 3, 0.6027211994, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-3', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '{\"index\": \"3\", \"source\": \"file\", \"chunkId\": \"3\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-3\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 3.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 20:26:01.325');
INSERT INTO `rag_query_hit` VALUES (11, 0, 10, 1, 0.6056539938, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 2.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 21:22:48.864');
INSERT INTO `rag_query_hit` VALUES (12, 0, 10, 2, 0.5881295800, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-1', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：', '{\"index\": \"1\", \"source\": \"file\", \"chunkId\": \"1\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-1\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 1.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 21:22:48.877');
INSERT INTO `rag_query_hit` VALUES (13, 0, 10, 3, 0.5842205510, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-3', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '{\"index\": \"3\", \"source\": \"file\", \"chunkId\": \"3\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-3\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 3.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 21:22:48.890');
INSERT INTO `rag_query_hit` VALUES (14, 0, 11, 1, 0.6188406423, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 2.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 21:23:24.693');
INSERT INTO `rag_query_hit` VALUES (15, 0, 14, 1, 0.5952754095, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 2.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 21:25:37.065');
INSERT INTO `rag_query_hit` VALUES (16, 0, 15, 1, 0.8773986101, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-1', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：', '{\"index\": \"1\", \"source\": \"file\", \"chunkId\": \"1\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-1\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 1.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 21:57:38.415');
INSERT INTO `rag_query_hit` VALUES (17, 0, 15, 2, 0.8660921752, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-0', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作', '{\"index\": \"0\", \"source\": \"file\", \"chunkId\": \"0\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-0\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 0.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 21:57:38.438');
INSERT INTO `rag_query_hit` VALUES (18, 0, 15, 3, 0.8388222158, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-3', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '{\"index\": \"3\", \"source\": \"file\", \"chunkId\": \"3\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-3\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 3.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 21:57:38.450');
INSERT INTO `rag_query_hit` VALUES (19, 0, 15, 4, 0.8380517364, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 2.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 21:57:38.473');
INSERT INTO `rag_query_hit` VALUES (20, 0, 15, 5, 0.8280361891, 1, 'doc_34823d93651b4511a251eb40608dd200', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_34823d93651b4511a251eb40608dd200-text-4', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '{\"index\": \"4\", \"source\": \"file\", \"chunkId\": \"4\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_34823d93651b4511a251eb40608dd200-text-4\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"0\", \"chunk_index\": 4.0, \"document_id\": \"doc_34823d93651b4511a251eb40608dd200\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"1\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb\", \"knowledge_base_id\": \"1\", \"document_version_id\": \"1\"}', '2026-07-28 21:57:38.488');
INSERT INTO `rag_query_hit` VALUES (21, 1, 16, 1, 0.8773986101, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-1', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：', '{\"index\": \"1\", \"source\": \"file\", \"chunkId\": \"1\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-1\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 1.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:57:59.297');
INSERT INTO `rag_query_hit` VALUES (22, 1, 16, 2, 0.8660921752, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作', '{\"index\": \"0\", \"source\": \"file\", \"chunkId\": \"0\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 0.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:57:59.319');
INSERT INTO `rag_query_hit` VALUES (23, 1, 16, 3, 0.8388222158, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '{\"index\": \"3\", \"source\": \"file\", \"chunkId\": \"3\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 3.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:57:59.335');
INSERT INTO `rag_query_hit` VALUES (24, 1, 16, 4, 0.8380517364, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 2.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:57:59.347');
INSERT INTO `rag_query_hit` VALUES (25, 1, 16, 5, 0.8280361891, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '{\"index\": \"4\", \"source\": \"file\", \"chunkId\": \"4\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 4.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:57:59.360');
INSERT INTO `rag_query_hit` VALUES (26, 1, 17, 1, 0.5762419105, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '{\"index\": \"4\", \"source\": \"file\", \"chunkId\": \"4\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 4.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:58:33.730');
INSERT INTO `rag_query_hit` VALUES (27, 1, 17, 2, 0.5589769445, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作', '{\"index\": \"0\", \"source\": \"file\", \"chunkId\": \"0\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 0.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:58:33.746');
INSERT INTO `rag_query_hit` VALUES (28, 1, 17, 3, 0.5562115274, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '{\"index\": \"3\", \"source\": \"file\", \"chunkId\": \"3\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 3.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:58:33.762');
INSERT INTO `rag_query_hit` VALUES (29, 1, 17, 4, 0.5551728234, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 2.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:58:33.778');
INSERT INTO `rag_query_hit` VALUES (30, 1, 18, 1, 0.5762419105, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '{\"index\": \"4\", \"source\": \"file\", \"chunkId\": \"4\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 4.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:58:45.548');
INSERT INTO `rag_query_hit` VALUES (31, 1, 18, 2, 0.5589769445, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作', '{\"index\": \"0\", \"source\": \"file\", \"chunkId\": \"0\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 0.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:58:45.573');
INSERT INTO `rag_query_hit` VALUES (32, 1, 18, 3, 0.5562115274, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '{\"index\": \"3\", \"source\": \"file\", \"chunkId\": \"3\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 3.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:58:45.592');
INSERT INTO `rag_query_hit` VALUES (33, 1, 18, 4, 0.5551728234, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 2.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:58:45.616');
INSERT INTO `rag_query_hit` VALUES (34, 1, 19, 1, 0.5762419105, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。', '{\"index\": \"4\", \"source\": \"file\", \"chunkId\": \"4\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 4.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:59:00.011');
INSERT INTO `rag_query_hit` VALUES (35, 1, 19, 2, 0.5589769445, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作', '{\"index\": \"0\", \"source\": \"file\", \"chunkId\": \"0\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 0.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:59:00.030');
INSERT INTO `rag_query_hit` VALUES (36, 1, 19, 3, 0.5562115274, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资', '{\"index\": \"3\", \"source\": \"file\", \"chunkId\": \"3\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 3.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:59:00.049');
INSERT INTO `rag_query_hit` VALUES (37, 1, 19, 4, 0.5551728234, 2, 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9', '浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf', 'doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2', 1, 'text', 'text', 'text_vector', NULL, NULL, NULL, NULL, NULL, '1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所', '{\"index\": \"2\", \"source\": \"file\", \"chunkId\": \"2\", \"current\": \"true\", \"version\": 1.0, \"chunk_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2\", \"fileName\": \"浦发上海浦东发展银行西安分行个金客户经理考核办法.pdf\", \"tenant_id\": \"1\", \"chunk_index\": 2.0, \"document_id\": \"doc_d3ab4b2bd5cf408f9afa9c94b7e537c9\", \"milvusAlias\": \"default\", \"chunk_status\": \"ACTIVE\", \"content_type\": \"text\", \"metadata_json\": \"{}\", \"document_db_id\": \"2\", \"permission_tags\": \"\", \"milvusCollection\": \"demo_kb_tenant_1\", \"knowledge_base_id\": \"2\", \"document_version_id\": \"2\"}', '2026-08-01 08:59:00.068');

-- ----------------------------
-- Table structure for rag_query_log
-- ----------------------------
DROP TABLE IF EXISTS `rag_query_log`;
CREATE TABLE `rag_query_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Query log ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `conversation_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'QUERY or SEARCH',
  `query_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `retrieval_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `knowledge_base_ids_json` json NULL,
  `top_k` int UNSIGNED NULL DEFAULT NULL,
  `min_score` decimal(8, 6) NULL DEFAULT NULL,
  `content_types_json` json NULL,
  `permission_tags_json` json NULL,
  `multimodal_trace_json` json NULL,
  `prompt_text` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `answer_text` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `knowledge_hit` tinyint NOT NULL DEFAULT 0,
  `hit_count` int UNSIGNED NOT NULL DEFAULT 0,
  `prompt_tokens` int UNSIGNED NULL DEFAULT NULL,
  `completion_tokens` int UNSIGNED NULL DEFAULT NULL,
  `total_tokens` int UNSIGNED NULL DEFAULT NULL,
  `llm_provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `llm_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `embedding_provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `embedding_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `estimated_input_cost` decimal(18, 8) NULL DEFAULT NULL,
  `estimated_output_cost` decimal(18, 8) NULL DEFAULT NULL,
  `estimated_embedding_cost` decimal(18, 8) NULL DEFAULT NULL,
  `estimated_total_cost` decimal(18, 8) NULL DEFAULT NULL,
  `cost_currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `latency_ms` bigint UNSIGNED NOT NULL DEFAULT 0,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS',
  `archive_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `retention_until` datetime(3) NULL DEFAULT NULL,
  `deleted_at` datetime(3) NULL DEFAULT NULL,
  `deleted_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `delete_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_query_tenant_created`(`tenant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_query_trace`(`trace_id` ASC) USING BTREE,
  INDEX `idx_query_conversation`(`conversation_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_query_type_status`(`query_type` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_query_log_archive_status`(`archive_status` ASC, `retention_until` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_query_log_deleted`(`is_deleted` ASC, `deleted_at` ASC) USING BTREE,
  INDEX `idx_query_cost_time`(`tenant_id` ASC, `llm_model` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG query audit log' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_query_log
-- ----------------------------
INSERT INTO `rag_query_log` VALUES (1, 0, '893f5b28709c4aa99ac34eeebaea26f3', 'demo-001', 'QUERY', '什么是AI', 'vector', '[1]', 8, 0.550000, '[\"text\", \"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"text\", \"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.2, \"imageUrlProvided\": false, \"textVectorWeight\": 0.4, \"imageVectorWeight\": 0.4, \"imageBase64Provided\": false, \"includeReviewPending\": false}', NULL, '我不知道', 0, 0, 0, 0, 0, 'openai-compatible', 'qwen-plus', 'openai-compatible', 'text-embedding-v4', 0.00000000, 0.00000000, 0.00000000, 0.00000000, 'unknown', 1093, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-27 22:56:24.553', '2026-07-27 22:56:24.553');
INSERT INTO `rag_query_log` VALUES (2, 0, 'c4bd1540fff54e11b6d0a3d31296c352', 'demo-001', 'QUERY', '什么是AI', 'vector', '[1]', 8, 0.550000, '[\"text\", \"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"text\", \"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.2, \"imageUrlProvided\": false, \"textVectorWeight\": 0.4, \"imageVectorWeight\": 0.4, \"imageBase64Provided\": false, \"includeReviewPending\": false}', NULL, '我不知道', 0, 0, 0, 0, 0, 'openai-compatible', 'qwen-plus', 'openai-compatible', 'text-embedding-v4', 0.00000000, 0.00000000, 0.00000000, 0.00000000, 'unknown', 258, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-27 22:56:46.397', '2026-07-27 22:56:46.397');
INSERT INTO `rag_query_log` VALUES (3, 0, '78f65ed0d971407e8c5d9484e5fe3cd4', 'demo-001', 'QUERY', '什么是AI', 'vector', '[1]', 8, 0.550000, '[\"text\", \"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"text\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.2, \"imageUrlProvided\": false, \"textVectorWeight\": 0.4, \"imageVectorWeight\": 0.4, \"imageBase64Provided\": false, \"includeReviewPending\": false}', NULL, '我不知道', 0, 0, 0, 0, 0, 'openai-compatible', 'qwen-plus', 'openai-compatible', 'text-embedding-v4', 0.00000000, 0.00000000, 0.00000000, 0.00000000, 'unknown', 296, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-27 22:57:58.539', '2026-07-27 22:57:58.539');
INSERT INTO `rag_query_log` VALUES (4, 0, '2c7a45cc940e4f30ab88e81d5198eff3', 'demo-001', 'QUERY', '什么是AI', 'vector', '[1]', 8, 0.550000, '[\"text\", \"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"text\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.2, \"imageUrlProvided\": false, \"textVectorWeight\": 0.4, \"imageVectorWeight\": 0.4, \"imageBase64Provided\": false, \"includeReviewPending\": false}', NULL, '我不知道', 0, 0, 0, 0, 0, 'openai-compatible', 'qwen-plus', 'openai-compatible', 'text-embedding-v4', 0.00000000, 0.00000000, 0.00000000, 0.00000000, 'unknown', 238, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-27 22:58:06.826', '2026-07-27 22:58:06.826');
INSERT INTO `rag_query_log` VALUES (5, 0, '6baf681514764f27a549420769ef7aa5', 'demo-001', 'QUERY', '浦发银行客户经理考核标准是怎样的', 'vector', '[1]', 8, 0.550000, '[\"text\", \"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"text\", \"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.2, \"imageUrlProvided\": false, \"textVectorWeight\": 0.4, \"imageVectorWeight\": 0.4, \"imageBase64Provided\": false, \"includeReviewPending\": false}', '你是企业级知识库助手。请严格基于【上下文】回答【问题】。\n- 如果上下文没有相关信息，请明确回答“我不知道”，不要编造。\n- 回答要尽量简洁、可操作、条理清晰（可用要点列表）。\n\n【上下文】\n[source rank=1 score=0.8743108808994293 kb=1 document=doc_34823d93651b4511a251eb40608dd200 chunk=doc_34823d93651b4511a251eb40608dd200-text-1 type=text modality=text retrieval=text_vector page=null]\n经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：\n\n---\n\n[source rank=2 score=0.8656617701053619 kb=1 document=doc_34823d93651b4511a251eb40608dd200 chunk=doc_34823d93651b4511a251eb40608dd200-text-0 type=text modality=text retrieval=text_vector page=null]\n百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作\n\n---\n\n[source rank=3 score=0.8359001576900482 kb=1 document=doc_34823d93651b4511a251eb40608dd200 chunk=doc_34823d93651b4511a251eb40608dd200-text-3 type=text modality=text retrieval=text_vector page=null]\n评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资\n\n---\n\n[source rank=4 score=0.8332695364952087 kb=1 document=doc_34823d93651b4511a251eb40608dd200 chunk=doc_34823d93651b4511a251eb40608dd200-text-2 type=text modality=text retrieval=text_vector page=null]\n1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所\n\n---\n\n[source rank=5 score=0.8255413174629211 kb=1 document=doc_34823d93651b4511a251eb40608dd200 chunk=doc_34823d93651b4511a251eb40608dd200-text-4 type=text modality=text retrieval=text_vector page=null]\n源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。\n\n【问题】\n浦发银行客户经理考核标准是怎样的\n', '浦发银行西安分行个金客户经理考核标准分为两大类，依据《个金客户经理管理考核暂行办法》：\n\n一、**个人业绩考核（季度考核）**  \n以三项业务为核心指标，按行员级别设定准入标准：  \n- **储蓄业务**：季日均余额（最低准入标准）  \n- **个贷业务**：季净增个贷余额（中级及以上客户经理的最低准入标准）  \n- **卡业务**：季有效净增发卡量（见习/D类/初级客户经理的最低准入标准）  \n  ▶ 有效卡定义：单张卡月均余额 ≥ 100 元  \n\n▶ 折算规则：50万元储蓄 = 50万元个贷 = 50张有效卡 = 5分（以5分为单位折算）  \n\n二、**工作质量考核（扣分制，最多扣50分）**  \n- **服务质量**：含投诉、差错、协作、培训参与、贷前贷后管理、廉洁自律等，单项扣分2–50分不等  \n- **资产质量**：  \n  - 收息率＜97%，每降1个百分点扣2分；  \n  - 不良资产率＞0%，每超1个百分点扣1分；  \n  - 逾期分类扣分（如跨月逾期、超3个月逾期等）  \n\n三、**结果应用**  \n- 考核分达某级标准，下一季度起享受对应行员等级薪资待遇；  \n- 连续不达标者，技术职务下调；  \n- 收入构成：等级基本收入 + 业绩奖励收入 + 日常工作绩效收入。', 1, 5, 3089, 366, 3455, 'openai-compatible', 'qwen-plus', 'openai-compatible', 'text-embedding-v4', 0.00000000, 0.00000000, 0.00000000, 0.00000000, 'unknown', 6550, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-27 23:00:51.699', '2026-07-27 23:00:51.699');
INSERT INTO `rag_query_log` VALUES (6, 0, '2fa7cf75369144c6a32c09ee92018238', NULL, 'SEARCH', '', 'vector', '[0]', 1, 0.000000, '[\"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', NULL, '{\"modalities\": [\"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.0, \"imageUrlProvided\": false, \"textVectorWeight\": 0.0, \"imageVectorWeight\": 1.0, \"imageBase64Provided\": true, \"includeReviewPending\": true}', NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 'openai-compatible', 'text-embedding-v4', NULL, NULL, NULL, NULL, NULL, 33, 'FAILED', 'ACTIVE', NULL, NULL, NULL, NULL, 0, 'IllegalStateException', 'Native multimodal image vector retrieval is disabled', '2026-07-27 23:53:33.675', '2026-07-27 23:53:33.675');
INSERT INTO `rag_query_log` VALUES (7, 0, 'f49986b6da5043aeac3219c2b51d92d7', NULL, 'SEARCH', '', 'vector', '[0]', 1, 0.000000, '[\"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', NULL, '{\"modalities\": [\"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.0, \"imageUrlProvided\": false, \"textVectorWeight\": 0.0, \"imageVectorWeight\": 1.0, \"imageBase64Provided\": true, \"includeReviewPending\": true}', NULL, NULL, 1, 1, NULL, NULL, NULL, NULL, NULL, 'openai-compatible', 'text-embedding-v4', NULL, NULL, NULL, NULL, NULL, 22054, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-28 20:25:02.594', '2026-07-28 20:25:02.594');
INSERT INTO `rag_query_log` VALUES (8, 0, '97af2acb40714588831dee6cc07d9806', NULL, 'SEARCH', 'login service architecture', 'vector', '[0]', 1, 0.000000, '[\"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', NULL, '{\"modalities\": [\"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.0, \"imageUrlProvided\": false, \"textVectorWeight\": 0.0, \"imageVectorWeight\": 1.0, \"imageBase64Provided\": true, \"includeReviewPending\": true}', NULL, NULL, 1, 1, NULL, NULL, NULL, NULL, NULL, 'openai-compatible', 'text-embedding-v4', NULL, NULL, NULL, NULL, NULL, 17964, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-28 20:25:20.933', '2026-07-28 20:25:20.933');
INSERT INTO `rag_query_log` VALUES (9, 0, '5385edf6cafc4070a22d58ba458f9ec0', NULL, 'SEARCH', '', 'vector', '[0]', 3, 0.000000, '[\"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', NULL, '{\"modalities\": [\"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.0, \"imageUrlProvided\": false, \"textVectorWeight\": 0.0, \"imageVectorWeight\": 1.0, \"imageBase64Provided\": true, \"includeReviewPending\": true}', NULL, NULL, 1, 3, NULL, NULL, NULL, NULL, NULL, 'openai-compatible', 'text-embedding-v4', NULL, NULL, NULL, NULL, NULL, 17351, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-28 20:26:01.282', '2026-07-28 20:26:01.282');
INSERT INTO `rag_query_log` VALUES (10, 0, '004c2185d48d490ca077efd10d0ddb02', NULL, 'SEARCH', 'architecture', 'vector', '[1]', 3, 0.000000, '[\"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.0, \"imageUrlProvided\": false, \"textVectorWeight\": 0.0, \"imageVectorWeight\": 1.0, \"imageBase64Provided\": true, \"includeReviewPending\": false}', NULL, NULL, 1, 3, NULL, NULL, NULL, NULL, NULL, 'openai-compatible', 'text-embedding-v4', NULL, NULL, NULL, NULL, NULL, 17739, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-28 21:22:48.843', '2026-07-28 21:22:48.843');
INSERT INTO `rag_query_log` VALUES (11, 0, '8cc405e0633c452ca28d18b4cd741e47', NULL, 'SEARCH', 'architecture', 'vector', '[1]', 1, 0.000000, '[\"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.0, \"imageUrlProvided\": false, \"textVectorWeight\": 0.0, \"imageVectorWeight\": 1.0, \"imageBase64Provided\": true, \"includeReviewPending\": false}', NULL, NULL, 1, 1, NULL, NULL, NULL, NULL, NULL, 'openai-compatible', 'text-embedding-v4', NULL, NULL, NULL, NULL, NULL, 16188, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-28 21:23:24.680', '2026-07-28 21:23:24.680');
INSERT INTO `rag_query_log` VALUES (12, 0, 'be2745909b08423bb0bb9f969bf14e85', NULL, 'SEARCH', 'architecture', 'vector', '[1]', 1, 0.000000, '[\"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"image\"], \"imageAssetId\": 1, \"keywordWeight\": 0.0, \"imageUrlProvided\": false, \"textVectorWeight\": 0.0, \"imageVectorWeight\": 1.0, \"imageBase64Provided\": false, \"includeReviewPending\": false}', NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 'openai-compatible', 'text-embedding-v4', NULL, NULL, NULL, NULL, NULL, 25, 'FAILED', 'ACTIVE', NULL, NULL, NULL, NULL, 0, 'IllegalArgumentException', 'Image asset not found: 1', '2026-07-28 21:24:46.446', '2026-07-28 21:24:46.446');
INSERT INTO `rag_query_log` VALUES (13, 0, 'deddf81bb89c44f9b87d34f376a47a50', NULL, 'SEARCH', 'architecture', 'vector', '[1]', 1, 0.000000, '[\"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"image\"], \"imageAssetId\": 1, \"keywordWeight\": 0.0, \"imageUrlProvided\": false, \"textVectorWeight\": 0.0, \"imageVectorWeight\": 1.0, \"imageBase64Provided\": false, \"includeReviewPending\": false}', NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 'openai-compatible', 'text-embedding-v4', NULL, NULL, NULL, NULL, NULL, 27, 'FAILED', 'ACTIVE', NULL, NULL, NULL, NULL, 0, 'IllegalArgumentException', 'Image asset not found: 1', '2026-07-28 21:25:19.266', '2026-07-28 21:25:19.266');
INSERT INTO `rag_query_log` VALUES (14, 0, '9935ec43a9904bde9eeffbe5c3e95d30', NULL, 'SEARCH', 'architecture', 'vector', '[1]', 1, 0.000000, '[\"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.0, \"imageUrlProvided\": false, \"textVectorWeight\": 0.0, \"imageVectorWeight\": 1.0, \"imageBase64Provided\": true, \"includeReviewPending\": false}', NULL, NULL, 1, 1, NULL, NULL, NULL, NULL, NULL, 'openai-compatible', 'text-embedding-v4', NULL, NULL, NULL, NULL, NULL, 967, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-28 21:25:37.054', '2026-07-28 21:25:37.054');
INSERT INTO `rag_query_log` VALUES (15, 0, '13cb4b4948624f6cb7d441db4fe818d9', 'demo-001', 'QUERY', '浦发银行客户经理考核标准', 'vector', '[1]', 8, 0.550000, '[\"text\", \"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"text\", \"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.2, \"imageUrlProvided\": false, \"textVectorWeight\": 0.4, \"imageVectorWeight\": 0.4, \"imageBase64Provided\": false, \"includeReviewPending\": false}', '你是企业级知识库助手。请严格基于【上下文】回答【问题】。\n- 如果上下文没有相关信息，请明确回答“我不知道”，不要编造。\n- 回答要尽量简洁、可操作、条理清晰（可用要点列表）。\n\n【上下文】\n[source rank=1 score=0.8773986101150513 kb=1 document=doc_34823d93651b4511a251eb40608dd200 chunk=doc_34823d93651b4511a251eb40608dd200-text-1 type=text modality=text retrieval=text_vector page=null]\n经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：\n\n---\n\n[source rank=2 score=0.866092175245285 kb=1 document=doc_34823d93651b4511a251eb40608dd200 chunk=doc_34823d93651b4511a251eb40608dd200-text-0 type=text modality=text retrieval=text_vector page=null]\n百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作\n\n---\n\n[source rank=3 score=0.838822215795517 kb=1 document=doc_34823d93651b4511a251eb40608dd200 chunk=doc_34823d93651b4511a251eb40608dd200-text-3 type=text modality=text retrieval=text_vector page=null]\n评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资\n\n---\n\n[source rank=4 score=0.8380517363548279 kb=1 document=doc_34823d93651b4511a251eb40608dd200 chunk=doc_34823d93651b4511a251eb40608dd200-text-2 type=text modality=text retrieval=text_vector page=null]\n1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所\n\n---\n\n[source rank=5 score=0.8280361890792847 kb=1 document=doc_34823d93651b4511a251eb40608dd200 chunk=doc_34823d93651b4511a251eb40608dd200-text-4 type=text modality=text retrieval=text_vector page=null]\n源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。\n\n【问题】\n浦发银行客户经理考核标准\n', '浦发银行西安分行个金客户经理考核标准如下（依据《个金客户经理管理考核暂行办法》）：\n\n**一、考核分类**  \n- **个人业绩考核**（季度考核）：占主要权重，含三项指标  \n- **工作质量考核**（扣分制，最多扣50分）  \n\n**二、个人业绩考核指标与准入标准**  \n| 行员级别       | 考核分值 | 储蓄季日均（万元） | 季净增个贷余额（万元） | 季有效净增发卡量（张） |  \n|----------------|----------|---------------------|-------------------------|-------------------------|  \n| 客户经理助理   | 90–110   | 300                 | —                       | 500                     |  \n| 客户经理       | 115–135  | 300                 | —                       | 500                     |  \n| 高级客户经理   | 140–160  | 500                 | 800                     | —                       |  \n| 资深客户经理   | 165–185  | 500                 | 800                     | —                       |  \n\n✅ **最低准入要求**：  \n- 所有级别：储蓄季日均 ≥ 最低标准（硬性门槛）；  \n- 见习/D类/初级客户经理：卡业务为最低准入标准；  \n- 中级及以上：个贷业务为最低准入标准。  \n\n✅ **折算规则**：  \n50万元储蓄 = 50万元个贷 = 50张有效卡 = 5分（仅以5分为单位折算）。\n\n**三、工作质量考核（扣分项）**  \n- **服务质量**：如投诉（每起扣2分）、不参加培训（每次扣2分）、贷前/贷后失职（每笔扣5分）、不廉洁（每次扣50分）等；  \n- **资产质量**：  \n  - 收息率＜97%，每降1个百分点扣2分；  \n  - 不良资产率＞0%，每超1个百分点扣1分；  \n  - 逾期：跨月逾期扣1–4分；逾期＞3个月，扣10分。\n\n**四、结果应用**  \n- 达到某级考核分值，下一季度享受对应行员等级薪资待遇；  \n- 连续不达标，技术职务下调；  \n- 基础薪点不低于总收入的40%。', 1, 5, 3084, 567, 3651, 'openai-compatible', 'qwen-plus', 'openai-compatible', 'text-embedding-v4', 0.00000000, 0.00000000, 0.00000000, 0.00000000, 'unknown', 10603, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-07-28 21:57:38.382', '2026-07-28 21:57:38.382');
INSERT INTO `rag_query_log` VALUES (16, 1, '6a38277167fb4a83868b4ac039d2f302', 'demo-001', 'QUERY', '浦发银行客户经理考核标准', 'vector', '[2]', 8, 0.550000, '[\"text\", \"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"text\", \"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.2, \"imageUrlProvided\": false, \"textVectorWeight\": 0.4, \"imageVectorWeight\": 0.4, \"imageBase64Provided\": false, \"includeReviewPending\": false}', '你是企业级知识库助手。请严格基于【上下文】回答【问题】。\n- 如果上下文没有相关信息，请明确回答“我不知道”，不要编造。\n- 回答要尽量简洁、可操作、条理清晰（可用要点列表）。\n\n【上下文】\n[source rank=1 score=0.8773986101150513 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-1 type=text modality=text retrieval=text_vector page=null]\n经验。\n\n（二）工作能力：熟悉我行的各项业务，了解市场情况，熟悉各\n\n类客户的金融需求，熟悉个人理财工具，有一定的业务管理和客户管\n\n理能力。\n\n（三）工作业绩：个金客户经理均应达到相应等级的准入标准。\n\n该标准可根据全行整体情况由考核部门进行调整。\n\n（四）专业培训：个金客户经理应参加有关部门组织的专业培训\n\n并通过业务考试。\n\n（五）符合分行人事管理和专业管理的要求。\n\n第四章  个人业绩考核标准\n\n第八条  个金客户经理个人业绩以储蓄季日均、季有效净增发卡\n\n量、季净增个贷余额三项业务为主要考核指标，实行季度考核。具体\n\n标准如下：\n\n类别 行员级别 考核分值 准入标准\n\n储蓄业务 个贷业务 卡业务\n\n客户经理助理 5 90 300 万  500 张\n\n4 95\n\n百度文库 - 好好学习，天天向上\n\n-4\n\n3 100\n\n2 105\n\n1 110\n\n客户经理 5 115 300 万  500 张\n\n4 120\n\n3 125\n\n2 130\n\n1 135\n\n高级客户经理 5 140 500 万 800 万\n\n4 145\n\n3 150\n\n2 155\n\n1 160\n\n资深客户经理 5 165 500 万 800 万\n\n4 170\n\n3 175\n\n2 180\n\n1 185\n\n说明：1.储蓄业务（季日均余额）为各类个金客户经理考核进入的最低标准。\n\n2.卡业务（季新增发有效卡量）为见习、D 类、初级客户经理进入的最低标准。\n\n3.有效卡的概念：每张卡月均余额为 100 元以上。\n\n4.个贷业务（季新增发放个贷）为中级以上客户经理考核进入的最低标准。\n\n5.超出最低考核标准可相互折算，折算标准：50 万储蓄=50 万个贷=50 张有效卡=5 分（折算以 5 分为单位）\n\n百度文库 - 好好学习，天天向上\n\n-5\n\n第五章  工作质量考核标准\n\n第九条  工作质量考核实行扣分制。工作质量指个金客户经理在\n\n从事所有个人业务时出现投诉、差错及风险。该项考核最多扣 50 分，\n\n如发生重大差错事故，按分行有关制度处理。\n\n（一）服务质量考核：\n\n---\n\n[source rank=2 score=0.866092175245285 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0 type=text modality=text retrieval=text_vector page=null]\n百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作\n\n---\n\n[source rank=3 score=0.838822215795517 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3 type=text modality=text retrieval=text_vector page=null]\n评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资\n\n---\n\n[source rank=4 score=0.8380517363548279 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2 type=text modality=text retrieval=text_vector page=null]\n1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所\n\n---\n\n[source rank=5 score=0.8280361890792847 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4 type=text modality=text retrieval=text_vector page=null]\n源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。\n\n【问题】\n浦发银行客户经理考核标准\n', '浦发银行西安分行个金客户经理考核标准分为两大类，依据《个金客户经理管理考核暂行办法》：\n\n一、**个人业绩考核（季度考核）**  \n三项核心指标（可相互折算）：  \n- **储蓄业务**：季日均余额（最低准入标准）  \n- **个贷业务**：季净增个贷余额（中级及以上客户经理的最低准入标准）  \n- **卡业务**：季有效净增发卡量（见习/D类/初级客户经理的最低准入标准）  \n  ▶ 有效卡定义：单张卡月均余额 ≥ 100 元  \n\n▶ 折算规则：50万元储蓄 = 50万元个贷 = 50张有效卡 = 5分（以5分为单位折算）  \n\n二、**工作质量考核（扣分制，最多扣50分）**  \n- **服务质量**：含责任心、客户投诉（每起扣2分）、培训缺勤（每次扣2分）、贷前/贷后违规（每笔扣5分）、廉洁问题（每次扣50分）等；  \n- **资产质量**：  \n  - 收息率＜97%，每降1个百分点扣2分；  \n  - 不良资产率＞0%，每超1个百分点扣1分；  \n  - 逾期：跨月逾期扣1–4分；逾期＞3个月，扣10分。  \n\n三、**结果应用**  \n- 考核分达某行员级别标准，下一季度起享受对应薪资待遇；  \n- 连续不达标者，技术职务下调；  \n- 收入构成：等级基本收入（40%保底）+业绩奖励+日常工作绩效。', 1, 5, 3044, 382, 3426, 'openai-compatible', 'qwen-plus', 'openai-compatible', 'text-embedding-v4', 0.00000000, 0.00000000, 0.00000000, 0.00000000, 'unknown', 6808, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-08-01 08:57:59.271', '2026-08-01 08:57:59.271');
INSERT INTO `rag_query_log` VALUES (17, 1, '1c32f57b2b764c45855e24f717eb4e4c', 'demo-001', 'QUERY', '明天衡阳天气如何', 'vector', '[2]', 8, 0.550000, '[\"text\", \"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"text\", \"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.2, \"imageUrlProvided\": false, \"textVectorWeight\": 0.4, \"imageVectorWeight\": 0.4, \"imageBase64Provided\": false, \"includeReviewPending\": false}', '你是企业级知识库助手。请严格基于【上下文】回答【问题】。\n- 如果上下文没有相关信息，请明确回答“我不知道”，不要编造。\n- 回答要尽量简洁、可操作、条理清晰（可用要点列表）。\n\n【上下文】\n[source rank=1 score=0.5762419104576111 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4 type=text modality=text retrieval=text_vector page=null]\n源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。\n\n---\n\n[source rank=2 score=0.5589769445359707 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0 type=text modality=text retrieval=text_vector page=null]\n百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作\n\n---\n\n[source rank=3 score=0.5562115274369717 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3 type=text modality=text retrieval=text_vector page=null]\n评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资\n\n---\n\n[source rank=4 score=0.555172823369503 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2 type=text modality=text retrieval=text_vector page=null]\n1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所\n\n【问题】\n明天衡阳天气如何\n', '我不知道。', 1, 4, 2326, 2, 2328, 'openai-compatible', 'qwen-plus', 'openai-compatible', 'text-embedding-v4', 0.00000000, 0.00000000, 0.00000000, 0.00000000, 'unknown', 780, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-08-01 08:58:33.711', '2026-08-01 08:58:33.711');
INSERT INTO `rag_query_log` VALUES (18, 1, '75e6da74ba644c2a9ed6150f8869ee7a', 'demo-001', 'QUERY', '明天衡阳天气如何', 'vector', '[2]', 8, 0.550000, '[\"text\", \"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"text\", \"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.2, \"imageUrlProvided\": false, \"textVectorWeight\": 0.4, \"imageVectorWeight\": 0.4, \"imageBase64Provided\": false, \"includeReviewPending\": false}', '你是企业级知识库助手。请严格基于【上下文】回答【问题】。\n- 如果上下文没有相关信息，请明确回答“我不知道”，不要编造。\n- 回答要尽量简洁、可操作、条理清晰（可用要点列表）。\n\n【上下文】\n[source rank=1 score=0.5762419104576111 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4 type=text modality=text retrieval=text_vector page=null]\n源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。\n\n---\n\n[source rank=2 score=0.5589769445359707 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0 type=text modality=text retrieval=text_vector page=null]\n百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作\n\n---\n\n[source rank=3 score=0.5562115274369717 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3 type=text modality=text retrieval=text_vector page=null]\n评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资\n\n---\n\n[source rank=4 score=0.555172823369503 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2 type=text modality=text retrieval=text_vector page=null]\n1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所\n\n【问题】\n明天衡阳天气如何\n', '我不知道', 1, 4, 2326, 1, 2327, 'openai-compatible', 'qwen-plus', 'openai-compatible', 'text-embedding-v4', 0.00000000, 0.00000000, 0.00000000, 0.00000000, 'unknown', 926, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-08-01 08:58:45.524', '2026-08-01 08:58:45.524');
INSERT INTO `rag_query_log` VALUES (19, 1, 'a6641e433fa44be5a8284c9070e2d3ea', 'demo-001', 'QUERY', '明天衡阳天气如何', 'vector', '[2]', 8, 0.550000, '[\"text\", \"image\", \"chart\", \"table\", \"flowchart\", \"architecture\"]', '[]', '{\"modalities\": [\"text\", \"image\"], \"imageAssetId\": \"\", \"keywordWeight\": 0.2, \"imageUrlProvided\": false, \"textVectorWeight\": 0.4, \"imageVectorWeight\": 0.4, \"imageBase64Provided\": false, \"includeReviewPending\": false}', '你是企业级知识库助手。请严格基于【上下文】回答【问题】。\n- 如果上下文没有相关信息，请明确回答“我不知道”，不要编造。\n- 回答要尽量简洁、可操作、条理清晰（可用要点列表）。\n\n【上下文】\n[source rank=1 score=0.5762419104576111 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-4 type=text modality=text retrieval=text_vector page=null]\n源部、风险管理部负责人。\n\n第十九条  客户经理申报的各种信息必须真实。分行个人业务部\n\n需对其工作业绩数据进行核实，并对其真实性负责；分行人事部门需\n\n对其学历、工作阅历等基本信息进行核实，并对其真实性负责。\n\n第二十条  对因工作不负责任使资产质量产生严重风险或造成损\n\n失的给予降级直至开除处分，构成渎职罪的提请司法部门追究刑事责\n\n任。\n\n百度文库 - 好好学习，天天向上\n\n-9\n\n第九章  附    则\n\n第二十一条  本办法自发布之日起执行。\n\n第二十二条  本办法由上海浦东发展银行西安分行行负责解释和\n\n修改。\n\n---\n\n[source rank=2 score=0.5589769445359707 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-0 type=text modality=text retrieval=text_vector page=null]\n百度文库 - 好好学习，天天向上\n\n-1\n\n上海浦东发展银行西安分行\n\n个金客户经理管理考核暂行办法\n\n第一章  总   则\n\n第一条  为保证我分行个金客户经理制的顺利实施，有效调动个\n\n金客户经理的积极性，促进个金业务快速、稳定地发展，根据总行《上\n\n海浦东发展银行个人金融营销体系建设方案（试行）》要求，特制定\n\n《上海浦东发展银行西安分行个金客户经理管理考核暂行办法（试\n\n行）》（以下简称本办法）。\n\n第二条  个金客户经理系指各支行（营业部）从事个人金融产品\n\n营销与市场开拓，为我行个人客户提供综合银行服务的我行市场人\n\n员。\n\n第三条  考核内容分为二大类，即个人业绩考核、工作质量考核。\n\n个人业绩包括个人资产业务、负债业务、卡业务。工作质量指个人业\n\n务的资产质量。\n\n第四条  为规范激励规则，客户经理的技术职务和薪资实行每年\n\n考核浮动。客户经理的奖金实行每季度考核浮动，即客户经理按其考\n\n核内容得分与行员等级结合，享受对应的行员等级待遇。\n\n百度文库 - 好好学习，天天向上\n\n-2\n\n第二章  职位设置与职责\n\n第五条  个金客户经理职位设置为：客户经理助理、客户经理、\n\n高级客户经理、资深客户经理。\n\n第六条  个金客户经理的基本职责：\n\n（一）  客户开发。研究客户信息、联系与选择客户、与客户建\n\n立相互依存、相互支持的业务往来关系，扩大业务资源，创造良好业\n\n绩；\n\n（二）业务创新与产品营销。把握市场竞争变化方向，开展市场\n\n与客户需求的调研，对业务产品及服务进行创新；设计客户需求的产\n\n品组合、制订和实施市场营销方案；\n\n（三）客户服务。负责我行各类表内外授信业务及中间业务的受\n\n理和运作，进行综合性、整体性的客户服务；\n\n（四）防范风险，提高收益。提升风险防范意识及能力，提高经\n\n营产品质量；\n\n（五）培养人材。在提高自身综合素质的同时，发扬团队精神，\n\n培养后备业务骨干。\n\n百度文库 - 好好学习，天天向上\n\n-3\n\n第三章  基础素质要求\n\n第七条  个金客户经理准入条件：\n\n（一）工作经历：须具备大专以上学历，至少二年以上银行工作\n\n---\n\n[source rank=3 score=0.5562115274369717 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-3 type=text modality=text retrieval=text_vector page=null]\n评聘技术职务较低的市场人员，各级领导要加大培养力度，使其尽快\n\n百度文库 - 好好学习，天天向上\n\n-7\n\n入围，并由所在行制定临时奖励办法。\n\n第七章  考核待遇\n\n第十五条  个人金融业务客户经理的收入基本由三部分组成：客\n\n户经理等级基本收入、业绩奖励收入和日常工作绩效收入。\n\n客户经理等级基本收入是指客户经理的每月基本收入，基本分为\n\n助理客户经理、客户经理、高级客户经理和资深客户经理四大层面，\n\n在每一层面分为若干等级。\n\n客户经理的等级标准由客户经理在上年的业绩为核定标准，如果\n\n客户经理在我行第一次进行客户经理评级，以客户经理自我评价为主\n\n要依据，结合客户经理以往工作经验，由个人金融部、人事部门共同\n\n最终决定客户经理的等级。\n\n助理客户经理待遇按照人事部门对主办科员以下人员的待遇标\n\n准；客户经理待遇按照人事部门对主办科员的待遇标准；高级客户经\n\n理待遇按照人事部门对付科级的待遇标准；资深客户经理待遇按照人\n\n事部门对正科级的待遇标准。\n\n业绩奖励收入是指客户经理每个业绩考核期间的实际业绩所给\n\n与兑现的奖金部分。\n\n日常工作绩效收入是按照个金客户经理所从事的事务性工作进\n\n行定量化考核，经过工作的完成情况进行奖金分配。该项奖金主要由\n\n个人金融部总经理和各支行的行长其从事个人金融业务的人员进行\n\n分配，主要侧重分配于从事个金业务的基础工作和创新工作。\n\n百度文库 - 好好学习，天天向上\n\n-8\n\n第十五条  各项考核分值总计达到某一档行员级别考核分值标\n\n准，个金客户经理即可在下一季度享受该级行员的薪资标准。下一季\n\n度考核时，按照已享受行员级别考核折算比值进行考核，以次类推。\n\n第十六条  对已聘为各级客户经理的人员，当工作业绩考核达不\n\n到相应技术职务要求下限时，下一年技术职务相应下调。\n\n第十七条  为保护个人业务客户经理创业的积极性，暂定其收入\n\n构成中基础薪点不低于 40%。\n\n第八章  管理与奖惩\n\n第十八条  个金客户经理管理机构为分行客户经理管理委员会。\n\n管理委员会组成人员：行长或主管业务副行长，个人业务部、人力资\n\n---\n\n[source rank=4 score=0.555172823369503 kb=2 document=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9 chunk=doc_d3ab4b2bd5cf408f9afa9c94b7e537c9-text-2 type=text modality=text retrieval=text_vector page=null]\n1、工作责任心不强，缺乏配合协作精神；扣 5 分\n\n2、客户服务效率低，态度生硬或不及时为客户提供维护服务，\n\n有客户投诉的,每投诉一次扣 2 分\n\n3、不服从支行工作安排，不认真参加分（支）行宣传活动的，\n\n每次扣 2 分；\n\n4、未能及时参加分行（支行）组织的各种业务培训、考试和专\n\n题活动的每次扣 2 分；\n\n5、未按规定要求进行贷前调查、贷后检查工作的，每笔扣 5 分；\n\n6、未建立信贷台帐资料及档案的每笔扣 5 分；\n\n7、在工作中有不廉洁自律情况的每发现一次扣 50 分。\n\n（二）个人资产质量考核：\n\n当季考核收息率 97%以上为合格，每降 1 个百分点扣 2 分；不\n\n良资产零为合格，每超一个个百分点扣 1 分。\n\nA.发生跨月逾期，单笔不超过 10 万元，当季收回者，扣 1 分。\n\nB.发生跨月逾期，2 笔以上累计金额不超过 20 万元，当季收回\n\n者，扣 2 分；累计超过 20 万元以上的，扣 4 分。\n\n百度文库 - 好好学习，天天向上\n\n-6\n\nC.发生逾期超过 3 个月，无论金额大小和笔数，扣 10 分。\n\n第六章  聘任考核程序\n\n第十条  凡达到本办法第三章规定的该技术职务所要求的行内职\n\n工，都可向分行人力资源部申报个金客户经理评聘。\n\n第十一条  每年一月份为客户经理评聘的申报时间，由分行人力\n\n资源部、个人业务部每年二月份组织统一的资格考试。考试合格者由\n\n分行颁发个金客户经理资格证书，其有效期为一年。\n\n第十二条  客户经理聘任实行开放式、浮动制，即：本人申报 —\n\n— 所在部门推荐 —— 分行考核 —— 行长聘任 —— 每年考评\n\n调整浮动。\n\n第十三条  特别聘任：\n\n（一）经分行同意录用从其他单位调入的个金客户经理，由用人\n\n单位按 D 类人员进行考核，薪资待遇按其业绩享受行内正式行员工同\n\n等待遇。待正式转正后按第十一条规定申报技术职务。\n\n（二）对为我行业务创新、工作业绩等方面做出重大贡献的市场\n\n人员经支行推荐、分行行长批准可越级聘任。\n\n第十四条  对于创利业绩较高，而暂未入围技术职务系列，或所\n\n【问题】\n明天衡阳天气如何\n', '我不知道。', 1, 4, 2326, 2, 2328, 'openai-compatible', 'qwen-plus', 'openai-compatible', 'text-embedding-v4', 0.00000000, 0.00000000, 0.00000000, 0.00000000, 'unknown', 861, 'SUCCESS', 'ACTIVE', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-08-01 08:58:59.975', '2026-08-01 08:58:59.975');

-- ----------------------------
-- Table structure for rag_query_log_archive
-- ----------------------------
DROP TABLE IF EXISTS `rag_query_log_archive`;
CREATE TABLE `rag_query_log_archive`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Archive row ID',
  `source_query_log_id` bigint UNSIGNED NOT NULL,
  `delete_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `conversation_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `query_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `retrieval_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `knowledge_base_ids_json` json NULL,
  `top_k` int UNSIGNED NULL DEFAULT NULL,
  `min_score` decimal(8, 6) NULL DEFAULT NULL,
  `content_types_json` json NULL,
  `permission_tags_json` json NULL,
  `prompt_text` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `answer_text` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `knowledge_hit` tinyint NOT NULL DEFAULT 0,
  `hit_count` int UNSIGNED NOT NULL DEFAULT 0,
  `prompt_tokens` int UNSIGNED NULL DEFAULT NULL,
  `completion_tokens` int UNSIGNED NULL DEFAULT NULL,
  `total_tokens` int UNSIGNED NULL DEFAULT NULL,
  `llm_provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `llm_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `embedding_provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `embedding_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `estimated_input_cost` decimal(18, 8) NULL DEFAULT NULL,
  `estimated_output_cost` decimal(18, 8) NULL DEFAULT NULL,
  `estimated_embedding_cost` decimal(18, 8) NULL DEFAULT NULL,
  `estimated_total_cost` decimal(18, 8) NULL DEFAULT NULL,
  `cost_currency` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `latency_ms` bigint UNSIGNED NOT NULL DEFAULT 0,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS',
  `archive_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ARCHIVED',
  `retention_until` datetime(3) NULL DEFAULT NULL,
  `error_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_created_at` datetime(3) NULL DEFAULT NULL,
  `query_updated_at` datetime(3) NULL DEFAULT NULL,
  `archived_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_query_archive_source`(`source_query_log_id` ASC, `archived_at` ASC) USING BTREE,
  INDEX `idx_query_archive_tenant_time`(`tenant_id` ASC, `archived_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG query log archive' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_query_log_archive
-- ----------------------------

-- ----------------------------
-- Table structure for rag_query_log_delete_audit
-- ----------------------------
DROP TABLE IF EXISTS `rag_query_log_delete_audit`;
CREATE TABLE `rag_query_log_delete_audit`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Delete audit ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `delete_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Operation batch number',
  `operator` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `delete_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SOFT_DELETE, ARCHIVE, PURGE, RESTORE',
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_log_ids_json` json NULL,
  `matched_count` int NOT NULL DEFAULT 0,
  `success_count` int NOT NULL DEFAULT 0,
  `failed_count` int NOT NULL DEFAULT 0,
  `filter_json` json NULL,
  `result_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_query_log_delete_no`(`delete_no` ASC) USING BTREE,
  INDEX `idx_query_log_delete_mode`(`delete_mode` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_query_log_delete_operator`(`operator` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG query log delete audit' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_query_log_delete_audit
-- ----------------------------

-- ----------------------------
-- Table structure for rag_query_retention_policy
-- ----------------------------
DROP TABLE IF EXISTS `rag_query_retention_policy`;
CREATE TABLE `rag_query_retention_policy`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Retention policy ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `policy_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `query_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ALL',
  `status_filter` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ALL',
  `retention_days` int NOT NULL DEFAULT 180,
  `archive_before_delete` tinyint NOT NULL DEFAULT 1,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_retention_policy_match`(`tenant_id` ASC, `query_type` ASC, `status_filter` ASC, `enabled` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG query retention policy' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_query_retention_policy
-- ----------------------------

-- ----------------------------
-- Table structure for rag_rerank_call_log
-- ----------------------------
DROP TABLE IF EXISTS `rag_rerank_call_log`;
CREATE TABLE `rag_rerank_call_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Rerank call log ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `query_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `api_key_hash` char(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `tenant_external_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `request_window` datetime(3) NULL DEFAULT NULL,
  `candidate_count` int UNSIGNED NOT NULL DEFAULT 0,
  `top_k` int UNSIGNED NOT NULL DEFAULT 0,
  `input_tokens` int UNSIGNED NOT NULL DEFAULT 0,
  `output_tokens` int UNSIGNED NOT NULL DEFAULT 0,
  `total_tokens` int UNSIGNED NOT NULL DEFAULT 0,
  `latency_ms` bigint UNSIGNED NOT NULL DEFAULT 0,
  `success` tinyint NOT NULL DEFAULT 0,
  `fallback` tinyint NOT NULL DEFAULT 0,
  `estimated_cost` decimal(18, 8) NOT NULL DEFAULT 0.00000000,
  `error_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `http_status` int UNSIGNED NULL DEFAULT NULL,
  `error_code_normalized` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `degraded_reason` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `retry_count` int UNSIGNED NOT NULL DEFAULT 0,
  `cache_hit` tinyint NOT NULL DEFAULT 0,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rerank_model_created`(`tenant_id` ASC, `provider` ASC, `model` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_rerank_api_key_time`(`api_key_hash` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_rerank_tenant_time`(`tenant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_rerank_error_time`(`error_code_normalized` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_rerank_window`(`request_window` ASC, `provider` ASC, `model` ASC) USING BTREE,
  INDEX `idx_rerank_success`(`success` ASC, `fallback` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_rerank_latency`(`latency_ms` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG external rerank call observability log' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_rerank_call_log
-- ----------------------------

-- ----------------------------
-- Table structure for rag_rerank_observation_metric_daily
-- ----------------------------
DROP TABLE IF EXISTS `rag_rerank_observation_metric_daily`;
CREATE TABLE `rag_rerank_observation_metric_daily`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `bucket_start` datetime(3) NOT NULL,
  `window_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HOUR',
  `provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `api_key_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `degraded_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `request_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `success_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `failure_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `fallback_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `retry_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `cache_hit_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `total_tokens` bigint UNSIGNED NOT NULL DEFAULT 0,
  `estimated_cost` decimal(18, 8) NOT NULL DEFAULT 0.00000000,
  `p50_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p90_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p95_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p99_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rerank_observation_hourly`(`tenant_id` ASC, `bucket_start` ASC, `provider` ASC, `model` ASC, `api_key_hash` ASC, `error_code` ASC, `degraded_reason` ASC) USING BTREE,
  INDEX `idx_rerank_observation_hourly_bucket`(`bucket_start` ASC, `tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Hourly materialized rerank observation metrics' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_rerank_observation_metric_daily
-- ----------------------------

-- ----------------------------
-- Table structure for rag_rerank_observation_metric_hourly
-- ----------------------------
DROP TABLE IF EXISTS `rag_rerank_observation_metric_hourly`;
CREATE TABLE `rag_rerank_observation_metric_hourly`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `bucket_start` datetime(3) NOT NULL,
  `window_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HOUR',
  `provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `api_key_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `error_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `degraded_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `request_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `success_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `failure_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `fallback_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `retry_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `cache_hit_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `total_tokens` bigint UNSIGNED NOT NULL DEFAULT 0,
  `estimated_cost` decimal(18, 8) NOT NULL DEFAULT 0.00000000,
  `p50_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p90_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p95_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `p99_latency_ms` decimal(18, 4) NOT NULL DEFAULT 0.0000,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rerank_observation_hourly`(`tenant_id` ASC, `bucket_start` ASC, `provider` ASC, `model` ASC, `api_key_hash` ASC, `error_code` ASC, `degraded_reason` ASC) USING BTREE,
  INDEX `idx_rerank_observation_hourly_bucket`(`bucket_start` ASC, `tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Hourly materialized rerank observation metrics' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_rerank_observation_metric_hourly
-- ----------------------------

-- ----------------------------
-- Table structure for rag_resource_permission_tag
-- ----------------------------
DROP TABLE IF EXISTS `rag_resource_permission_tag`;
CREATE TABLE `rag_resource_permission_tag`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NULL DEFAULT NULL,
  `resource_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `resource_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `permission_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_resource_permission_tag`(`tenant_id` ASC, `resource_type` ASC, `resource_id` ASC, `permission_tag` ASC) USING BTREE,
  INDEX `idx_resource_permission_kb`(`tenant_id` ASC, `knowledge_base_id` ASC, `permission_tag` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Resource permission tag' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_resource_permission_tag
-- ----------------------------

-- ----------------------------
-- Table structure for rag_retrieval_eval_case
-- ----------------------------
DROP TABLE IF EXISTS `rag_retrieval_eval_case`;
CREATE TABLE `rag_retrieval_eval_case`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Evaluation case ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NULL DEFAULT NULL,
  `version_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'default' COMMENT 'Knowledge base or document version label',
  `case_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `query_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `retrieval_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'hybrid',
  `query_category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'definition',
  `difficulty_level` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'easy',
  `language` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'mixed',
  `expected_answer_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'fact',
  `top_k` int UNSIGNED NULL DEFAULT NULL,
  `min_score` decimal(8, 6) NULL DEFAULT NULL,
  `content_types_json` json NULL,
  `permission_tags_json` json NULL,
  `expected_chunk_ids_json` json NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `metadata_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_eval_case_id`(`tenant_id` ASC, `case_id` ASC) USING BTREE,
  INDEX `idx_eval_case_kb_version`(`tenant_id` ASC, `knowledge_base_id` ASC, `version_tag` ASC, `enabled` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_eval_case_category`(`tenant_id` ASC, `knowledge_base_id` ASC, `query_category` ASC, `enabled` ASC, `is_deleted` ASC) USING BTREE,
  INDEX `idx_eval_case_updated`(`updated_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG retrieval evaluation case' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_retrieval_eval_case
-- ----------------------------

-- ----------------------------
-- Table structure for rag_retrieval_eval_case_result
-- ----------------------------
DROP TABLE IF EXISTS `rag_retrieval_eval_case_result`;
CREATE TABLE `rag_retrieval_eval_case_result`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Evaluation case result ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `run_id` bigint UNSIGNED NOT NULL,
  `case_db_id` bigint UNSIGNED NULL DEFAULT NULL,
  `case_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `query_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `top_k` int UNSIGNED NOT NULL DEFAULT 0,
  `expected_chunk_ids_json` json NOT NULL,
  `retrieved_chunk_ids_json` json NOT NULL,
  `hit` tinyint NOT NULL DEFAULT 0,
  `reciprocal_rank` decimal(10, 6) NOT NULL DEFAULT 0.000000,
  `recall_value` decimal(10, 6) NOT NULL DEFAULT 0.000000,
  `failure_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `failure_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `retrieval_trace_json` json NULL,
  `cluster_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_eval_result_run`(`run_id` ASC, `id` ASC) USING BTREE,
  INDEX `idx_eval_result_case`(`case_db_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_eval_result_hit`(`hit` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_eval_result_failure`(`failure_type` ASC, `cluster_key` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG retrieval evaluation per-case result' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_retrieval_eval_case_result
-- ----------------------------

-- ----------------------------
-- Table structure for rag_retrieval_eval_cluster
-- ----------------------------
DROP TABLE IF EXISTS `rag_retrieval_eval_cluster`;
CREATE TABLE `rag_retrieval_eval_cluster`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Evaluation failure cluster ID',
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `run_id` bigint UNSIGNED NOT NULL,
  `cluster_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `cluster_label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `failure_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `case_count` int UNSIGNED NOT NULL DEFAULT 0,
  `sample_case_ids_json` json NULL,
  `suggestion` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_eval_cluster_run_key`(`run_id` ASC, `cluster_key` ASC) USING BTREE,
  INDEX `idx_eval_cluster_run`(`run_id` ASC, `case_count` ASC) USING BTREE,
  INDEX `idx_eval_cluster_type`(`failure_type` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG retrieval evaluation failure cluster' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_retrieval_eval_cluster
-- ----------------------------

-- ----------------------------
-- Table structure for rag_retrieval_eval_run
-- ----------------------------
DROP TABLE IF EXISTS `rag_retrieval_eval_run`;
CREATE TABLE `rag_retrieval_eval_run`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Evaluation run ID',
  `run_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `knowledge_base_id` bigint UNSIGNED NULL DEFAULT NULL,
  `version_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'default',
  `retrieval_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'hybrid',
  `total_cases` int UNSIGNED NOT NULL DEFAULT 0,
  `hit_rate` decimal(10, 6) NOT NULL DEFAULT 0.000000,
  `mean_reciprocal_rank` decimal(10, 6) NOT NULL DEFAULT 0.000000,
  `mean_recall` decimal(10, 6) NOT NULL DEFAULT 0.000000,
  `source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'mysql',
  `metadata_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_eval_run_no`(`tenant_id` ASC, `run_no` ASC) USING BTREE,
  INDEX `idx_eval_run_kb_version`(`tenant_id` ASC, `knowledge_base_id` ASC, `version_tag` ASC, `retrieval_mode` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_eval_run_created`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG retrieval evaluation run history' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_retrieval_eval_run
-- ----------------------------
INSERT INTO `rag_retrieval_eval_run` VALUES (1, 'eval_50234aa639914243848196a2cd6f68b5', 0, NULL, 'default', 'hybrid', 0, 0.000000, 0.000000, 0.000000, 'file', NULL, '2026-07-29 11:58:49.710');

-- ----------------------------
-- Table structure for rag_tenant_model_config
-- ----------------------------
DROP TABLE IF EXISTS `rag_tenant_model_config`;
CREATE TABLE `rag_tenant_model_config`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL,
  `provider` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `base_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `api_key_secret_ref` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `temperature` decimal(3, 2) NULL DEFAULT 0.20 COMMENT 'LLM temperature (0-2)',
  `dimension` int NULL DEFAULT 1536 COMMENT 'Embedding dimension',
  `image_size` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Text-to-image output size',
  `image_quality` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Text-to-image quality option',
  `poll_interval_millis` int NULL DEFAULT NULL COMMENT 'Text-to-image polling interval milliseconds',
  `rate_limit_qps` int NULL DEFAULT NULL COMMENT 'Rate limit (requests per second)',
  `monthly_budget_cents` bigint NULL DEFAULT NULL COMMENT 'Monthly budget in cents',
  `timeout_seconds` int NULL DEFAULT NULL COMMENT 'Request timeout seconds',
  `max_retries` int NULL DEFAULT NULL COMMENT 'Maximum retry count',
  `max_tokens` int NULL DEFAULT NULL COMMENT 'Maximum output tokens',
  `frequency_penalty` decimal(4, 2) NULL DEFAULT NULL COMMENT 'Frequency penalty',
  `presence_penalty` decimal(4, 2) NULL DEFAULT NULL COMMENT 'Presence penalty',
  `top_p` decimal(4, 2) NULL DEFAULT NULL COMMENT 'Top-p sampling value',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_model_config`(`tenant_id` ASC, `model_type` ASC, `provider` ASC, `model_name` ASC) USING BTREE,
  INDEX `idx_tenant_model_enabled`(`tenant_id` ASC, `model_type` ASC, `enabled` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant model configuration' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_tenant_model_config
-- ----------------------------
INSERT INTO `rag_tenant_model_config` VALUES (1, 0, 'smoke-provider', 'LLM', 'smoke-model-updated', 'http://localhost/smoke-updated', 'ENC:lqIS4lj7We7mxYcx0GlqMRWXlL+znnkMFVMqvttrvu4sGNv0xik4d16SiZDkBQ==', 0.40, 1536, NULL, NULL, NULL, NULL, NULL, 15, 2, 1024, NULL, NULL, 0.70, 1, '2026-07-28 22:33:28.638', '2026-07-28 22:33:28.819', 1);
INSERT INTO `rag_tenant_model_config` VALUES (2, 0, 'smoke-provider', 'LLM', 'smoke-model-updated-8d2f2c0f', 'http://localhost/smoke-updated', 'ENC:x5QnyR6Y/6jT+DBEMKg4xZvDmcvSpSTNhvLwTcVAPdTljbpHVtYjwShN6W6moQ==', 0.40, 1536, NULL, NULL, NULL, NULL, NULL, 15, 2, 1024, NULL, NULL, 0.70, 1, '2026-07-28 22:34:00.365', '2026-07-28 22:34:00.502', 1);
INSERT INTO `rag_tenant_model_config` VALUES (3, 90128, 'smoke-provider', 'LLM', 'smoke-model-a-3f1b0ac7-edited', 'http://localhost/edited', 'ENC:kLdibR+Swmx4KK8phDTEZZw29pUoF85ZLIx/Ufcp5/nStRNT4k8+7I74rg5Zug==', 0.30, 1536, NULL, NULL, NULL, NULL, NULL, 12, 1, 512, NULL, NULL, 0.80, 0, '2026-07-28 23:13:57.274', '2026-07-28 23:13:57.742', 1);
INSERT INTO `rag_tenant_model_config` VALUES (4, 90128, 'smoke-provider', 'LLM', 'smoke-model-b-3f1b0ac7', 'http://localhost/b', 'ENC:3mdrgPlXXyZj64GizqqHfDGbbytcmwgH0fjGh+rSpvM1XpFhdAw7LyRY7Vvr+g==', 0.30, 1536, NULL, NULL, NULL, NULL, NULL, 12, 1, 512, NULL, NULL, 0.80, 1, '2026-07-28 23:13:57.646', '2026-07-28 23:13:57.762', 1);
INSERT INTO `rag_tenant_model_config` VALUES (5, 0, 'openai-compatible', 'LLM', 'qwen-plus', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'ENC:1dTVtoWCFdNK2YawwOcoSFqAZuQHANmdLMmNlFBCuWehlYddIq14BEL234+IOAx8Z8O6WW2KIOOFYl2NKsiq', 0.20, 1536, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-07-30 05:23:53.518', '2026-07-30 05:24:25.972', 0);
INSERT INTO `rag_tenant_model_config` VALUES (6, 0, 'openai-compatible', 'EMBEDDING', 'text-embedding-v4', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'ENC:rOvwre4G1t1iT9/g/qx56g5/b6oTSandDcL26N86wXiHy3M0qBa0ORgDO3jf0gbKK/Uzi0/jB0ZML+YHYjRq', 0.20, 1024, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '2026-07-30 05:24:43.650', '2026-07-30 05:24:49.859', 0);
INSERT INTO `rag_tenant_model_config` VALUES (7, 0, 'openai-compatible', 'IMAGE', 'wanx-v1', 'https://dashscope.aliyuncs.com/api/v1', 'ENC:rRrm1wpZOrNRyVtQoOLshB6Wk8VY9j1ZjDpJMjV0BVKu2+CTJFzWvQDuc7rFcpAlKQLvbvmO950rdlRGj+UL', 0.20, 1536, '1024x1024', 'standard', 2000, NULL, NULL, 60, NULL, NULL, NULL, NULL, NULL, 1, '2026-07-30 05:24:57.009', '2026-07-30 05:24:57.009', 0);

-- ----------------------------
-- Table structure for rag_tenant_quota
-- ----------------------------
DROP TABLE IF EXISTS `rag_tenant_quota`;
CREATE TABLE `rag_tenant_quota`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL,
  `max_documents` bigint UNSIGNED NULL DEFAULT NULL,
  `max_storage_bytes` bigint UNSIGNED NULL DEFAULT NULL,
  `max_file_bytes` bigint UNSIGNED NULL DEFAULT NULL,
  `daily_ocr_limit` bigint UNSIGNED NULL DEFAULT NULL,
  `daily_embedding_tokens` bigint UNSIGNED NULL DEFAULT NULL,
  `max_concurrent_ingestion_tasks` bigint UNSIGNED NULL DEFAULT NULL,
  `daily_query_limit` bigint UNSIGNED NULL DEFAULT NULL,
  `monthly_budget_cents` bigint UNSIGNED NULL DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_quota`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant quota' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_tenant_quota
-- ----------------------------

-- ----------------------------
-- Table structure for rag_tenant_usage_daily
-- ----------------------------
DROP TABLE IF EXISTS `rag_tenant_usage_daily`;
CREATE TABLE `rag_tenant_usage_daily`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL,
  `usage_date` date NOT NULL,
  `document_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `storage_bytes` bigint UNSIGNED NOT NULL DEFAULT 0,
  `ocr_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `embedding_tokens` bigint UNSIGNED NOT NULL DEFAULT 0,
  `vector_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `query_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `llm_tokens` bigint UNSIGNED NOT NULL DEFAULT 0,
  `estimated_cost_cents` bigint UNSIGNED NOT NULL DEFAULT 0,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_usage_day`(`tenant_id` ASC, `usage_date` ASC) USING BTREE,
  INDEX `idx_tenant_usage_date`(`usage_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant daily usage' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_tenant_usage_daily
-- ----------------------------

-- ----------------------------
-- Table structure for rag_workspace
-- ----------------------------
DROP TABLE IF EXISTS `rag_workspace`;
CREATE TABLE `rag_workspace`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `workspace_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `workspace_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_workspace_tenant_code`(`tenant_id` ASC, `workspace_code` ASC) USING BTREE,
  INDEX `idx_workspace_tenant`(`tenant_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG workspace' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_workspace
-- ----------------------------

-- ----------------------------
-- Table structure for rag_workspace_member
-- ----------------------------
DROP TABLE IF EXISTS `rag_workspace_member`;
CREATE TABLE `rag_workspace_member`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `workspace_id` bigint UNSIGNED NOT NULL,
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `member_role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'READER',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_workspace_member`(`tenant_id` ASC, `workspace_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_workspace_member_user`(`tenant_id` ASC, `user_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'RAG workspace member' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rag_workspace_member
-- ----------------------------

-- ----------------------------
-- Table structure for sys_admin_impersonation_session
-- ----------------------------
DROP TABLE IF EXISTS `sys_admin_impersonation_session`;
CREATE TABLE `sys_admin_impersonation_session`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT 'Tenant ID',
  `session_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `operator_user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `operator_tenant_id` bigint UNSIGNED NOT NULL,
  `target_tenant_id` bigint UNSIGNED NOT NULL,
  `impersonation_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `source_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `started_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `expires_at` datetime(3) NOT NULL,
  `revoked_at` datetime(3) NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_impersonation_session_no`(`session_no` ASC) USING BTREE,
  INDEX `idx_impersonation_operator`(`operator_user_id` ASC, `started_at` ASC) USING BTREE,
  INDEX `idx_impersonation_target`(`target_tenant_id` ASC, `started_at` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Admin impersonation session' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_admin_impersonation_session
-- ----------------------------

-- ----------------------------
-- Table structure for sys_operation_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_audit_log`;
CREATE TABLE `sys_operation_audit_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT 'Tenant ID',
  `operator_user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `operator_tenant_id` bigint UNSIGNED NULL DEFAULT NULL,
  `target_tenant_id` bigint UNSIGNED NULL DEFAULT NULL,
  `impersonation_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `request_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `source_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `operation` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `resource_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `resource_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `result` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS',
  `detail_json` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_audit_target_time`(`target_tenant_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_audit_operator_time`(`operator_user_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_audit_operation_time`(`operation` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 45 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant operation audit log' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_operation_audit_log
-- ----------------------------
INSERT INTO `sys_operation_audit_log` VALUES (1, 0, 'demo-user', 0, 0, NULL, 'req-66754b7888d14fd68c6c018be9c7632d', '0:0:0:0:0:0:0:1', 'AUTH_LOGIN', 'USER', 'admin', 'SUCCESS', '{}', '2026-07-31 08:28:36.725');
INSERT INTO `sys_operation_audit_log` VALUES (2, 0, 'demo-user', 0, 0, NULL, 'req-b3d49f9226bf4fe18947e83185cb6511', '0:0:0:0:0:0:0:1', 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 08:29:00.750');
INSERT INTO `sys_operation_audit_log` VALUES (3, 0, 'demo-user', 0, 0, NULL, 'req-45be34a6606643ed87af94c45ef2b0ef', '0:0:0:0:0:0:0:1', 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 08:31:08.607');
INSERT INTO `sys_operation_audit_log` VALUES (4, 0, 'demo-user', 0, 0, NULL, 'req-066988a666884bc7b5bb18a8a76b78e7', '0:0:0:0:0:0:0:1', 'AUTH_LOGIN', 'USER', 'admin', 'SUCCESS', '{}', '2026-07-31 08:37:12.449');
INSERT INTO `sys_operation_audit_log` VALUES (5, 0, 'demo-user', 0, 0, NULL, 'req-d6b4298fcff4438c8deb19fbd39b2857', '0:0:0:0:0:0:0:1', 'AUTH_LOGIN', 'USER', 'admin', 'SUCCESS', '{}', '2026-07-31 08:39:28.094');
INSERT INTO `sys_operation_audit_log` VALUES (6, 0, 'demo-user', 0, 0, NULL, 'req-17e513cae4924cd487de751e9fc7ecda', '0:0:0:0:0:0:0:1', 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 08:39:28.094');
INSERT INTO `sys_operation_audit_log` VALUES (7, 0, 'demo-user', 0, 0, NULL, 'req-82b8c09d2dbb4a72aa853ccfd64f1173', '0:0:0:0:0:0:0:1', 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 08:39:41.098');
INSERT INTO `sys_operation_audit_log` VALUES (8, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'admin', 'SUCCESS', '{}', '2026-07-31 09:15:41.920');
INSERT INTO `sys_operation_audit_log` VALUES (9, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 09:15:41.920');
INSERT INTO `sys_operation_audit_log` VALUES (10, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'admin', 'SUCCESS', '{}', '2026-07-31 09:17:47.230');
INSERT INTO `sys_operation_audit_log` VALUES (11, 0, 'admin', 0, 0, NULL, 'login-8a410908e3b34a0a92c796843cc4fdbc', '0:0:0:0:0:0:0:1', 'AUTH_LOGOUT', 'USER', 'admin', 'SUCCESS', '{}', '2026-07-31 09:17:55.764');
INSERT INTO `sys_operation_audit_log` VALUES (12, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'admin', 'SUCCESS', '{}', '2026-07-31 09:23:26.631');
INSERT INTO `sys_operation_audit_log` VALUES (13, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'admin', 'SUCCESS', '{}', '2026-07-31 18:21:32.178');
INSERT INTO `sys_operation_audit_log` VALUES (14, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'PLATFORM_ADMIN', 'admin', 'SUCCESS', '{}', '2026-07-31 18:23:16.054');
INSERT INTO `sys_operation_audit_log` VALUES (15, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 18:23:16.405');
INSERT INTO `sys_operation_audit_log` VALUES (16, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'PLATFORM_ADMIN', 'admin', 'SUCCESS', '{}', '2026-07-31 18:23:39.646');
INSERT INTO `sys_operation_audit_log` VALUES (17, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'PLATFORM_ADMIN', 'admin', 'SUCCESS', '{}', '2026-07-31 18:29:45.747');
INSERT INTO `sys_operation_audit_log` VALUES (18, 0, 'admin', NULL, NULL, NULL, 'login-10b7ac5929e145388cdc2f3a100dacfa', '0:0:0:0:0:0:0:1', 'AUTH_LOGOUT', 'USER', 'admin', 'SUCCESS', '{}', '2026-07-31 18:30:25.679');
INSERT INTO `sys_operation_audit_log` VALUES (19, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 19:59:20.816');
INSERT INTO `sys_operation_audit_log` VALUES (20, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:02:17.101');
INSERT INTO `sys_operation_audit_log` VALUES (21, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:02:33.486');
INSERT INTO `sys_operation_audit_log` VALUES (22, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:06:00.791');
INSERT INTO `sys_operation_audit_log` VALUES (23, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:06:00.800');
INSERT INTO `sys_operation_audit_log` VALUES (24, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:14:19.323');
INSERT INTO `sys_operation_audit_log` VALUES (25, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:15:48.299');
INSERT INTO `sys_operation_audit_log` VALUES (26, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'PLATFORM_ADMIN', 'admin', 'SUCCESS', '{}', '2026-07-31 20:15:48.285');
INSERT INTO `sys_operation_audit_log` VALUES (27, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:15:48.299');
INSERT INTO `sys_operation_audit_log` VALUES (28, 1, 'demo-user', 1, 1, NULL, 'login-ecceb1a12a7446269f437dd53b6d5e57', '0:0:0:0:0:0:0:1', 'TENANT_USER_UPDATE', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:25:37.937');
INSERT INTO `sys_operation_audit_log` VALUES (29, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:28:47.155');
INSERT INTO `sys_operation_audit_log` VALUES (30, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:32:44.096');
INSERT INTO `sys_operation_audit_log` VALUES (31, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:32:57.945');
INSERT INTO `sys_operation_audit_log` VALUES (32, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 20:44:32.958');
INSERT INTO `sys_operation_audit_log` VALUES (33, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 21:20:59.894');
INSERT INTO `sys_operation_audit_log` VALUES (34, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'PLATFORM_ADMIN', 'admin', 'SUCCESS', '{}', '2026-07-31 21:21:18.759');
INSERT INTO `sys_operation_audit_log` VALUES (35, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 21:22:00.284');
INSERT INTO `sys_operation_audit_log` VALUES (36, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'PLATFORM_ADMIN', 'admin', 'SUCCESS', '{}', '2026-07-31 21:22:14.521');
INSERT INTO `sys_operation_audit_log` VALUES (37, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 21:24:49.628');
INSERT INTO `sys_operation_audit_log` VALUES (38, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 21:26:14.398');
INSERT INTO `sys_operation_audit_log` VALUES (39, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-07-31 21:35:54.999');
INSERT INTO `sys_operation_audit_log` VALUES (40, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-08-01 08:56:00.673');
INSERT INTO `sys_operation_audit_log` VALUES (41, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-08-01 09:02:09.372');
INSERT INTO `sys_operation_audit_log` VALUES (42, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-08-01 09:02:28.461');
INSERT INTO `sys_operation_audit_log` VALUES (43, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-08-01 09:13:15.645');
INSERT INTO `sys_operation_audit_log` VALUES (44, 0, 'unknown', NULL, NULL, NULL, NULL, NULL, 'AUTH_LOGIN', 'USER', 'demo-user', 'SUCCESS', '{}', '2026-08-01 09:13:37.851');

-- ----------------------------
-- Table structure for sys_platform_admin
-- ----------------------------
DROP TABLE IF EXISTS `sys_platform_admin`;
CREATE TABLE `sys_platform_admin`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `singleton_key` tinyint NOT NULL DEFAULT 1,
  `admin_username` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BCrypt password hash',
  `password_updated_at` datetime(3) NULL DEFAULT NULL COMMENT 'Last password update time',
  `must_change_password` tinyint NOT NULL DEFAULT 0 COMMENT 'Whether admin must change password after login',
  `status` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_platform_admin_singleton`(`singleton_key` ASC) USING BTREE,
  UNIQUE INDEX `uk_sys_platform_admin_username`(`admin_username` ASC) USING BTREE,
  CONSTRAINT `chk_sys_platform_admin_singleton` CHECK (`singleton_key` = 1)
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Platform super administrator' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_platform_admin
-- ----------------------------
INSERT INTO `sys_platform_admin` VALUES (1, 1, 'admin', 'Super Administrator', 'admin@example.local', '$2a$10$LLIq5B/ubYU6/VE3o4uWMeREop6WGlD3WWh5EtKAmMfTHbcZsq1Ei', '2026-07-31 18:22:42.110', 0, 1, '2026-07-31 18:22:42.110', '2026-08-01 09:16:54.609', 0);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_scope` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_role_tenant_code`(`tenant_id` ASC, `role_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 51 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant role' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 0, 'SUPER_ADMIN', 'Super Administrator', 'PLATFORM', '2026-07-31 08:27:59.540', '2026-07-31 09:23:14.449', 0);
INSERT INTO `sys_role` VALUES (2, 0, 'PLATFORM_ADMIN', 'Platform Administrator', 'PLATFORM', '2026-07-31 08:27:59.549', '2026-07-31 09:23:14.456', 0);
INSERT INTO `sys_role` VALUES (3, 0, 'TENANT_ADMIN', 'Tenant Administrator', 'TENANT', '2026-07-31 08:27:59.553', '2026-07-31 09:23:14.462', 0);
INSERT INTO `sys_role` VALUES (25, 1, 'TENANT_ADMIN', 'Tenant Administrator', 'TENANT', '2026-07-31 18:22:42.162', '2026-08-01 09:16:54.671', 0);
INSERT INTO `sys_role` VALUES (26, 1, 'KB_OWNER', 'Knowledge Base Owner', 'TENANT', '2026-07-31 18:22:42.167', '2026-08-01 09:16:54.682', 0);

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tenant_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `external_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_tenant_code`(`tenant_code` ASC) USING BTREE,
  INDEX `idx_sys_tenant_status`(`status` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant registry' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (1, 'demo', 'Demo Tenant', 'demo', 1, '2026-07-31 18:22:42.153', '2026-08-01 09:16:54.659', 0);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `external_user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `display_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BCrypt password hash',
  `password_updated_at` datetime(3) NULL DEFAULT NULL COMMENT 'Last password update time',
  `must_change_password` tinyint NOT NULL DEFAULT 0 COMMENT 'Whether user must change password after login',
  `status` tinyint NOT NULL DEFAULT 1,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_tenant_user`(`tenant_id` ASC, `external_user_id` ASC) USING BTREE,
  INDEX `idx_sys_user_email`(`tenant_id` ASC, `email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant user' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 0, 'demo-user', 'demo-user', 'Demo User', 'demo-user@example.local', '$2a$10$afSzNfrH7cBg6AvK/JGa1eLM6Bc.X0T9.mFuBrzsOKEXIxLiestmC', '2026-07-31 08:27:59.616', 0, 1, '2026-07-31 08:27:59.616', '2026-07-31 09:23:14.529', 0);
INSERT INTO `sys_user` VALUES (2, 0, 'admin', 'admin', 'Super Administrator', 'admin@example.local', '$2a$10$lsy9sZIvku/MzfVt0JCAVOQvfz0Q7W.w6GeJ0O.CztOQlV/lCHMVe', '2026-07-31 08:27:59.665', 0, 1, '2026-07-31 08:27:59.665', '2026-07-31 09:23:14.587', 0);
INSERT INTO `sys_user` VALUES (17, 1, 'demo-user', 'demo-user', 'Demo User', 'demo-user@example.local', '$2a$10$3fD4am7/lCzghhM6Wv/K0eC.J/4lvpnZQB2LZyHV/a.ebvpxAV3N.', '2026-07-31 18:22:42.222', 0, 1, '2026-07-31 18:22:42.222', '2026-08-01 09:16:54.744', 0);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint UNSIGNED NOT NULL DEFAULT 0,
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_id` bigint UNSIGNED NULL DEFAULT NULL,
  `role_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_role`(`tenant_id` ASC, `user_id` ASC, `role_code` ASC) USING BTREE,
  INDEX `idx_sys_user_role_user`(`tenant_id` ASC, `user_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 59 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant user role' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 0, 'demo-user', NULL, 'TENANT_ADMIN', '2026-07-31 08:27:59.668', 0);
INSERT INTO `sys_user_role` VALUES (2, 0, 'demo-user', NULL, 'PLATFORM_ADMIN', '2026-07-31 08:27:59.675', 0);
INSERT INTO `sys_user_role` VALUES (3, 0, 'admin', NULL, 'SUPER_ADMIN', '2026-07-31 08:27:59.679', 0);
INSERT INTO `sys_user_role` VALUES (4, 0, 'admin', NULL, 'PLATFORM_ADMIN', '2026-07-31 08:27:59.682', 0);
INSERT INTO `sys_user_role` VALUES (33, 1, 'demo-user', NULL, 'TENANT_ADMIN', '2026-07-31 18:22:42.228', 0);
INSERT INTO `sys_user_role` VALUES (34, 1, 'demo-user', NULL, 'KB_OWNER', '2026-07-31 18:22:42.236', 0);

-- ----------------------------
-- Table structure for tenant_data_deletion_stage
-- ----------------------------
DROP TABLE IF EXISTS `tenant_data_deletion_stage`;
CREATE TABLE `tenant_data_deletion_stage`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_id` bigint UNSIGNED NOT NULL,
  `stage_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `stage_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `deleted_count` bigint UNSIGNED NOT NULL DEFAULT 0,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_deletion_stage`(`task_id` ASC, `stage_code` ASC) USING BTREE,
  INDEX `idx_deletion_stage_status`(`stage_status` ASC, `updated_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant data deletion stage' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tenant_data_deletion_stage
-- ----------------------------

-- ----------------------------
-- Table structure for tenant_data_deletion_task
-- ----------------------------
DROP TABLE IF EXISTS `tenant_data_deletion_task`;
CREATE TABLE `tenant_data_deletion_task`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tenant_id` bigint UNSIGNED NOT NULL,
  `requested_by` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `task_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `started_at` datetime(3) NULL DEFAULT NULL,
  `finished_at` datetime(3) NULL DEFAULT NULL,
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_tenant_deletion_task_no`(`task_no` ASC) USING BTREE,
  INDEX `idx_tenant_deletion_task`(`tenant_id` ASC, `task_status` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Tenant data deletion task' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tenant_data_deletion_task
-- ----------------------------

-- ----------------------------
-- Table structure for work_order
-- ----------------------------
DROP TABLE IF EXISTS `work_order`;
CREATE TABLE `work_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant ID',
  `ticket_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工单编号（唯一）',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工单标题',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工单状态（NEW/PROCESSING/DONE/CLOSED）',
  `priority` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '优先级（LOW/MEDIUM/HIGH/URGENT）',
  `assignee_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '处理人ID',
  `assignee` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '处理人姓名',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_no`(`ticket_no` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_assignee_id`(`assignee_id` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of work_order
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
