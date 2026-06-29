# 003 Review Plan and Anki Export SPEC

- 状态：implemented
- 领域：review / export
- 更新于：2026-06-29

## 1. 背景

用户保存词卡后，需要把词卡加入个人词库，系统根据用户复习反馈计算下一次复习时间，并支持导出 Anki 可导入的 TSV 文件。

## 2. 契约

### 加入词库

`POST /api/wordbook/add`

请求：

```json
{
  "userId": 1,
  "wordCardId": 100
}
```

要求：

- 同一用户同一词卡只能加入一次。
- 重复加入返回已有学习计划。
- 词卡不存在时返回业务异常。

### 待复习列表

`GET /api/wordbook/due?userId=1&limit=20`

要求：

- 只返回 `next_review_time <= now` 的词卡。
- 按 next_review_time 升序返回。

### 提交复习结果

`POST /api/wordbook/review`

```json
{
  "userId": 1,
  "wordCardId": 100,
  "result": 2
}
```

result 语义：

- 0：忘记，1 小时后复习，掌握度归 0。
- 1：模糊，1 天后复习。
- 2：记住，按 1 / 3 / 7 / 15 / 30 天递进。

### Anki 导出

`GET /api/wordbook/export/anki?userId=1`

返回：`text/tab-separated-values;charset=UTF-8`

字段：

```text
Front	Back	Tags
```

## 3. 验收标准

- [x] 可以加入个人词库。
- [x] 重复加入具备幂等性。
- [x] 可以查询到期复习词卡。
- [x] 可以提交复习结果并生成下一次复习时间。
- [x] 每次复习记录落表。
- [x] 可以导出 Anki TSV。
- [x] 复习计划核心算法有单元测试。
