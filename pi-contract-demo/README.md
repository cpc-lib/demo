# 优化说明

## 本次新增优化重点

### 1. 统一外部入口为 calculateByCycleType
之前金额计算入口散落在：
- `doMonthCalculate`
- `doSeasonCalculate`
- `doHalfYearCalculate`
- `doYearCalculate`

现在已经统一收口为：
- `MoneyCalculator.calculateByCycleType(..., BillingCycleType)`

固定映射关系：
- `MONTH = 1`
- `SEASON = 3`
- `HALF_YEAR = 6`
- `YEAR = 12`

已按要求移除旧入口，不再做兼容保留。

---

### 2. 账期生成与金额计算真正对齐
`PeriodCalculator.getContractPeriod(minTime, maxTime, gap)` 仍然负责按月数拆账期。

对于标准账单周期：
- `gap=1` → `ONE`
- `gap=3` → `THREE`
- `gap=6` → `SIX`
- `gap=12` → `TWELVE`

这样“账期怎么拆”和“金额怎么算”使用的是同一套周期语义，不会再出现入口分散、规则重复的问题。

---

### 3. 增加付款周期边界校验
增加业务约束：
- 付款周期必须在 `1~12` 之间

非法值会直接抛出明确异常：
- `0`
- 负数
- `13` 及以上
- `null`

避免系统默默生成异常账期。

---

### 4. 给账期自动编号
生成账期时新增顺序编号：
- 第 1 期
- 第 2 期
- 第 3 期

写入 `ContractPeriodVO.number`

这样后续如果要做：
- 账单展示
- 导出 Excel
- 对账排序
- 第 N 期账单回查

会更方便。

---

### 5. 修复区间重叠判断边界
原先重叠判断使用的是严格大于/小于，边界时刻可读性较差。

现已改为“闭区间重叠判断”：
- `!stageEnd.before(periodStart) && !stageStart.after(periodEnd)`

更符合当前项目里大量使用 `00:00:00 ~ 23:59:59` 闭区间的建模方式。

---

### 6. 去掉对 commons-lang3 的强依赖
原项目里 `DateUtils` 只用了 commons-lang3 的极少量日期能力，但本地编译会强依赖整个三方包。

这次已补充项目内最小实现：
- `org.apache.commons.lang3.time.DateUtils`
- `org.apache.commons.lang3.time.DateFormatUtils`

并移除了 `pom.xml` 中对 commons-lang3 的依赖，让项目更轻量，也更容易在受限环境直接编译。

---

## 之前已处理的优化仍然保留

### 7. 修复固定账期拆分 bug
`BillCycleCalculator.calculateBillCycles` 原先在追加固定账期后，错误地把游标推进到：
- `fixedCycle.getStartTime().plusSeconds(1)`

这会导致后续普通账期和固定账期发生重叠。

已修复为：
- `fixedCycle.getEndTime().plusSeconds(1)`

---

### 8. 收敛重复的金额计算逻辑
原 `MoneyCalculator` 中：
- 月 / 季 / 半年 / 年 计算逻辑高度重复
- 多处区间交集判断手写 `if` 分支

已统一抽象为：
- 通用账期计算入口
- 通用交集金额累加逻辑
- 通过 `ChargeMode` 控制不同计费方式

---

### 9. 排序逻辑修复
`PeriodCalculator.getContractPeriodVos` 原排序比较器存在问题：
- 相等场景不满足比较器契约

已改为标准：
- `Comparator.comparing(...).thenComparing(...)`

---

### 10. 调整项目结构
已把测试/示例代码移动到 `src/test/java`，并清理无关目录：
- `.idea/`
- `target/`

---

## 本次验证结果
已在本地做了实际运行验证：

1. `1 ~ 12` 个月付款周期均可生成账期并完成金额计算
2. 合同 `2024-01-01 ~ 2024-12-31`、月租 `1000` 时：
   - 不论按 `1/2/3/.../12` 月支付
   - 汇总金额都正确为 `12000.00`
3. `2` 个月一付样例账期已验证：
   - `2024-01-15 00:00:00 -> 2024-03-14 23:59:59`
   - `2024-03-15 00:00:00 -> 2024-05-14 23:59:59`
   - `2024-05-15 00:00:00 -> 2024-06-14 23:59:59`

---

## 建议下一步继续优化的方向

1. 把 `wholeFlag` 从 `String` 升级为枚举，去掉魔法值 `0/1`
2. 把 `monthRent` 更名为更贴切的 `billAmount`，避免“非月付但字段叫月租”带来的歧义
3. 为固定账期 + 任意付款周期补更多断言测试
4. 长期建议逐步把 `Date` 收敛到 `LocalDateTime` / `LocalDate`
