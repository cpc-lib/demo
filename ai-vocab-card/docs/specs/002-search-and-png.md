# SPEC 002 - Search and PNG Export

状态：implemented

## 搜索
支持英文单词、英文解释、中文含义、标签查询。V1 使用 MySQL 索引 + LIKE + FULLTEXT 预留；后续可升级 Elasticsearch 和向量库。

## PNG 导出
前端使用 html-to-image 将 Ant Design 卡片导出为 PNG，支持用户在预览页导出图片。
