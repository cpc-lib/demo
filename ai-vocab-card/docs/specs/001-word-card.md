# SPEC 001 - AI Word Card

状态：implemented

## 目标
用户输入英文单词，系统调用 LangChain4j 适配的大模型生成词卡，用户预览、编辑后保存到数据库。

## 契约
- 输入：英文单词 word。
- 输出：word、phonetic、partOfSpeech、englishDefinition、chineseMeaning、usageNote、slangs、examples、tags。
- 生成接口只返回预览，不写数据库。
- 保存接口以英文 word 做唯一约束，重复保存执行更新。

## 验收标准
- 未配置 LLM 时，本地 fallback 可生成可编辑词卡。
- 配置 LLM_ENABLED=true 和 LLM_API_KEY 后，通过 LangChain4j 调用 OpenAI-compatible API。
- 保存后可通过搜索列表查询并查看详情。
