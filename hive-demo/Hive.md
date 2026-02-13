# Hive速查手册

## 目录

- [1. 数据库（Database）](#1-数据库database)
  - [1.1 创建数据库](#11-创建数据库)
  - [1.2 查询数据库](#12-查询数据库)
  - [1.3 查看数据库描述](#13-查看数据库描述)
  - [1.4 修改数据库](#14-修改数据库)
  - [1.5 删除数据库](#15-删除数据库)
  - [1.6 切换当前数据库](#16-切换当前数据库)
- [2. 数据表（Table）](#2-数据表table)
  - [2.1 建表语法总览](#21-建表语法总览)
  - [2.2 数据类型](#22-数据类型)
  - [2.3 分区表与分桶表关键字](#23-分区表与分桶表关键字)
  - [2.4 Row Format / SerDe](#24-row-format--serde)
  - [2.5 文件格式 Stored As](#25-文件格式-stored-as)
  - [2.6 Location / TBLPROPERTIES](#26-location--tblproperties)
  - [2.7 CTAS 与 LIKE](#27-ctas-与-like)
  - [2.8 内部表 vs 外部表](#28-内部表-vs-外部表)
  - [2.9 JSON SerDe 示例](#29-json-serde-示例)
  - [2.10 查看/修改表](#210-查看修改表)
- [3. DML（数据操作）](#3-dml数据操作)
  - [3.1 LOAD 导入](#31-load-导入)
  - [3.2 INSERT 写入](#32-insert-写入)
  - [3.3 INSERT OVERWRITE DIRECTORY](#33-insert-overwrite-directory)
  - [3.4 Export / Import](#34-export--import)
- [4. 查询（SELECT）](#4-查询select)
  - [4.1 基本语法与注意事项](#41-基本语法与注意事项)
  - [4.2 常用子句示例](#42-常用子句示例)
  - [4.3 Join / Union / 笛卡尔积](#43-join--union--笛卡尔积)
  - [4.4 排序（Order/Sort/Distribute/Cluster）](#44-排序ordersortdistributecluster)
- [5. 函数（Functions）](#5-函数functions)
  - [5.1 查看函数](#51-查看函数)
  - [5.2 数值/字符串/日期/流程控制/集合函数](#52-数值字符串日期流程控制集合函数)
  - [5.3 自定义函数 UDF/UDAF/UDTF](#53-自定义函数-udfudafudtf)
- [6. 分区表与分桶表](#6-分区表与分桶表)
  - [6.1 分区表](#61-分区表)
  - [6.2 动态分区](#62-动态分区)
  - [6.3 分桶表](#63-分桶表)
  - [6.4 分区 vs 分桶对比表](#64-分区-vs-分桶对比表)

---

## 1. 数据库（Database）

### 1.1 创建数据库

语法：

```sql
CREATE DATABASE [IF NOT EXISTS] database_name
[COMMENT database_comment]
[LOCATION hdfs_path]
[WITH DBPROPERTIES (property_name=property_value, ...)];
```

示例：

```sql
create database db_hive1;

create database db_hive2 location '/db_hive2';

create database db_hive3
with dbproperties('create_date'='2022-11-18');
```

### 1.2 查询数据库

```sql
SHOW DATABASES [LIKE 'identifier_with_wildcards'];
```

示例：

```sql
show databases like 'db_hive*';
```

### 1.3 查看数据库描述

```sql
DESCRIBE DATABASE [EXTENDED] db_name;
```

示例：

```sql
desc database db_hive3;
```

示例输出结构（字段含义）：

| 字段 | 含义 |
|---|---|
| db_name | 数据库名 |
| comment | 注释 |
| location | HDFS 路径 |
| owner_name / owner_type | 所有者信息 |
| parameters | DBPROPERTIES |

### 1.4 修改数据库

> 可修改：`dbproperties`、`location`、`owner user`  
> 注意：修改 database 的 `location` **不会影响已有表的路径**，只会影响后续新表默认父目录。

```sql
ALTER DATABASE db_hive3
SET DBPROPERTIES ('create_date'='2022-11-20');
```

### 1.5 删除数据库

```sql
DROP DATABASE [IF EXISTS] database_name [RESTRICT|CASCADE];
```

- `RESTRICT`（默认）：库不为空则删除失败  
- `CASCADE`：级联删除库中表

示例（失败场景：库非空）：

```sql
drop database db_hive3;
-- FAILED: ... Database db_hive3 is not empty. One or more tables exist.
```

### 1.6 切换当前数据库

```sql
USE database_name;
```

---

## 2. 数据表（Table）

### 2.1 建表语法总览

```sql
CREATE [TEMPORARY] [EXTERNAL] TABLE [IF NOT EXISTS] [db_name.]table_name
(
  col_name data_type [COMMENT col_comment],
  ...
)
[COMMENT table_comment]
[PARTITIONED BY (col_name data_type [COMMENT col_comment], ...)]
[CLUSTERED BY (col_name, col_name, ...)
  [SORTED BY (col_name [ASC|DESC], ...)] INTO num_buckets BUCKETS]
[ROW FORMAT row_format]
[STORED AS file_format]
[LOCATION hdfs_path];
```

关键概念：

- **TEMPORARY**：临时表，仅当前会话可见，会话结束自动删除  
- **EXTERNAL**：外部表（只管理元数据，不管理 HDFS 数据）

### 2.2 数据类型

#### 基本数据类型

| Hive 类型 | 说明 | 示例 |
|---|---|---|
| tinyint | 1 byte 有符号整数 |  |
| smallint | 2 byte 有符号整数 |  |
| int | 4 byte 有符号整数 |  |
| bigint | 8 byte 有符号整数 |  |
| boolean | 布尔 | true/false |
| float | 单精度浮点 |  |
| double | 双精度浮点 |  |
| decimal | 十进制精准数 | `decimal(16,2)` |
| varchar | 定长上限字符串 | `varchar(32)` |
| string | 字符串 |  |
| timestamp | 时间类型 |  |
| binary | 二进制 |  |

#### 复杂数据类型

| 类型 | 说明 | 定义示例 | 取值示例 |
|---|---|---|---|
| array | 相同类型数组 | `array<string>` | `arr[0]` |
| map | 键值对集合 | `map<string,int>` | `map['key']` |
| struct | 结构体 | `struct<id:int,name:string>` | `struct.id` |

#### 类型转换

- **隐式转换**（举例）
  - tinyint → int → bigint
  - int/float/string → double
  - tinyint/smallint/int → float
  - boolean 不能转换为其它类型
- **显式转换**：`cast`

```sql
cast(expr as <type>);

select '1' + 2, cast('1' as int) + 2;
```

### 2.3 分区表与分桶表关键字

- **PARTITIONED BY**：分区表（按目录拆分）
- **CLUSTERED BY ... SORTED BY ... INTO ... BUCKETS**：分桶表（按文件拆分）

### 2.4 Row Format / SerDe

#### 2.4.1 默认 SerDe：ROW FORMAT DELIMITED

```sql
ROW FORMAT DELIMITED
  [FIELDS TERMINATED BY char]
  [COLLECTION ITEMS TERMINATED BY char]
  [MAP KEYS TERMINATED BY char]
  [LINES TERMINATED BY char]
  [NULL DEFINED AS char]
```

含义：

- `FIELDS TERMINATED BY`：列分隔符
- `COLLECTION ITEMS TERMINATED BY`：array/map/struct 内元素分隔符
- `MAP KEYS TERMINATED BY`：map 的 key/value 分隔符
- `LINES TERMINATED BY`：行分隔符

#### 2.4.2 指定 SerDe：ROW FORMAT SERDE

```sql
ROW FORMAT SERDE 'serde_name'
WITH SERDEPROPERTIES (...);
```

### 2.5 文件格式 Stored As

常见：

- `textfile`（默认）
- `sequencefile`
- `orc`
- `parquet`

### 2.6 Location / TBLPROPERTIES

- **LOCATION**：指定 HDFS 路径  
  默认：`${hive.metastore.warehouse.dir}/db_name.db/table_name`

- **TBLPROPERTIES**：表级参数 KV 配置

### 2.7 CTAS 与 LIKE

#### CTAS（Create Table As Select）

> 直接用查询结果建表，表结构与查询结果一致，并写入数据。

```sql
CREATE [TEMPORARY] TABLE [IF NOT EXISTS] table_name
[COMMENT table_comment]
[ROW FORMAT row_format]
[STORED AS file_format]
[LOCATION hdfs_path]
[TBLPROPERTIES (...)]
AS select_statement;
```

#### Create Table Like

> 复制已有表结构，不包含数据。

```sql
CREATE [TEMPORARY] [EXTERNAL] TABLE [IF NOT EXISTS] [db_name.]table_name
LIKE exist_table_name
[ROW FORMAT row_format]
[STORED AS file_format]
[LOCATION hdfs_path]
[TBLPROPERTIES (...)];
```

### 2.8 内部表 vs 外部表

#### 内部表（管理表）

- Hive 管理 **元数据 + HDFS 数据**
- `drop table` 会删除 HDFS 数据目录

示例：

```sql
create table if not exists student(
  id int,
  name string
)
row format delimited fields terminated by '\t'
location '/user/hive/warehouse/db_hive3.db/student';
```

数据准备：

```bash
vim /opt/module/datas/student.txt
# 内容示例：
# 1001    student1
# 1002    student2
# ...

hadoop fs -put /opt/module/datas/student.txt /user/hive/warehouse/db_hive3.db/student
```

#### 外部表（EXTERNAL）

- Hive 只管理 **元数据**
- `drop table` 仅删除元数据，HDFS 数据仍保留

```sql
create external table if not exists student(
  id int,
  name string
)
row format delimited fields terminated by '\t'
location '/user/hive/warehouse/db_hive3.db/student';
```

### 2.9 JSON SerDe 示例

JSON 示例：

```json
{
  "name": "dasongsong",
  "friends": ["bingbing", "lili"],
  "students": {"xiaohaihai": 18, "xiaoyangyang": 16},
  "address": {"street": "hui long guan", "city": "beijing", "postal_code": 10010}
}
```

建表：

```sql
create table teacher (
  name     string,
  friends  array<string>,
  students map<string,int>,
  address  struct<city:string,street:string,postal_code:int>
)
row format serde 'org.apache.hadoop.hive.serde2.JsonSerDe'
location '/user/hive/warehouse/db_hive3.db/teacher';
```

导入数据文件：

```bash
vim /opt/module/datas/teacher.txt
# 单行 JSON：
# {"name":"dasongsong","friends":["bingbing","lili"],"students":{"xiaohaihai":18,"xiaoyangyang":16},"address":{"street":"hui long guan","city":"beijing","postal_code":10010}}

hadoop fs -put /opt/module/datas/teacher.txt /user/hive/warehouse/db_hive3.db/teacher
```

CTAS / LIKE 示例：

```sql
create table teacher1 as select * from teacher;  -- teacher1 有数据
create table teacher2 like teacher;              -- teacher2 无数据（只复制结构）
```

### 2.10 查看/修改表

#### 查看表

```sql
SHOW TABLES [IN database_name] LIKE 'identifier_with_wildcards';
```

示例：

```sql
show tables like 'teacher*';
```

#### 查看表信息

```sql
DESCRIBE [EXTENDED | FORMATTED] [db_name.]table_name;
```

示例：

```sql
desc extended teacher;
desc formatted teacher;
```

#### 修改表

1）重命名：

```sql
ALTER TABLE table_name RENAME TO new_table_name;

alter table student rename to stu;
alter table teacher1 rename to teacher_demo;
```

2）添加列（追加到末尾）：

```sql
ALTER TABLE table_name ADD COLUMNS (col_name data_type [COMMENT col_comment], ...);

alter table stu add columns(age int);
```

3）更新列（改名/类型/注释/位置）：

```sql
ALTER TABLE table_name
CHANGE [COLUMN] col_old_name col_new_name column_type
[COMMENT col_comment] [FIRST|AFTER column_name];

alter table stu change column age ages double;
```

4）替换列（用新列集替换所有列）：

```sql
ALTER TABLE table_name REPLACE COLUMNS (col_name data_type [COMMENT col_comment], ...);

alter table stu replace columns(id int, name string);
```

#### 删除表 / 清空表

```sql
DROP TABLE [IF EXISTS] table_name;
TRUNCATE TABLE table_name;
```

---

## 3. DML（数据操作）

### 3.1 LOAD 导入

语法：

```sql
LOAD DATA [LOCAL] INPATH 'filepath'
[OVERWRITE] INTO TABLE tablename
[PARTITION (partcol1=val1, partcol2=val2, ...)];
```

示例表：

```sql
create table student(
  id int,
  name string
)
row format delimited fields terminated by '\t';
```

导入本地文件：

```sql
load data local inpath '/opt/module/hive/datas/student.txt' into table student;
```

导入 HDFS 文件（先 put 到 HDFS）：

```bash
hadoop fs -put /opt/module/hive/datas/student.txt /user/atguigu
```

```sql
load data inpath '/user/atguigu/student.txt' into table student; -- 追加写
load data inpath '/user/atguigu/student.txt' overwrite into table student; -- 覆盖写
```

### 3.2 INSERT 写入

#### INSERT (INTO | OVERWRITE) ... SELECT

```sql
INSERT (INTO | OVERWRITE) TABLE tablename
[PARTITION (...)]
select_statement;
```

示例：

```sql
create table student1(
  id int,
  name string
)
row format delimited fields terminated by '\t';

insert overwrite table student1
select id, name from student;
```

#### INSERT ... VALUES

```sql
INSERT (INTO | OVERWRITE) TABLE tablename VALUES
  (v1, v2, ...),
  (v1, v2, ...);
```

示例：

```sql
insert into table student1 values (1,'wangwu'),(2,'zhaoliu');
```

### 3.3 INSERT OVERWRITE DIRECTORY

```sql
INSERT OVERWRITE [LOCAL] DIRECTORY 'directory'
[ROW FORMAT row_format]
[STORED AS file_format]
select_statement;
```

示例（写本地目录）：

```sql
insert overwrite local directory '/opt/module/hive/datas/student_out'
row format delimited fields terminated by '\t'
select id, name from student;
```

### 3.4 Export / Import

> Export 会导出 **数据 + 元数据** 到 HDFS；Import 可在另一个 Hive 实例恢复。

导出：

```sql
EXPORT TABLE tablename TO 'export_target_path';
```

导入：

```sql
IMPORT [EXTERNAL] TABLE new_or_original_tablename
FROM 'source_path'
[LOCATION 'import_target_path'];
```

示例：

```sql
export table db_hive3.student to '/user/hive/warehouse/export/student';
import table student from '/user/hive/warehouse/export/student';
```

注意：若 import 的目标表已存在且包含数据文件，会失败。

---

## 4. 查询（SELECT）

### 4.1 基本语法与注意事项

```sql
SELECT [ALL | DISTINCT] select_expr, ...
FROM table_reference
[WHERE where_condition]
[GROUP BY col_list]
[HAVING having_condition]
[ORDER BY col_list]
[CLUSTER BY col_list | [DISTRIBUTE BY col_list] [SORT BY col_list]]
[LIMIT number];
```

书写习惯：

- SQL 大小写不敏感
- 关键字不能拆分
- 子句建议分行、缩进提高可读性

### 4.2 常用子句示例

列别名：

```sql
select
  ename as name,
  deptno as dn
from emp;
```

Limit：

```sql
select * from emp limit 5;
select * from emp limit 2, 3;
```

Where：

```sql
select * from emp where sal > 1000;
select ename as name, deptno as dn from emp where sal > 1000;
```

聚合与分组：

```sql
select count(*) cnt from emp;
select max(sal) max_sal from emp;
select min(sal) min_sal from emp;
select sum(sal) sum_sal from emp;
select avg(sal) avg_sal from emp;

select deptno, avg(sal) avg_sal
from emp
group by deptno
having avg_sal > 2000;
```

### 4.3 Join / Union / 笛卡尔积

准备表：

```sql
create table if not exists dept(
  deptno int,
  dname string,
  loc int
)
row format delimited fields terminated by '\t';

create table if not exists emp(
  empno int,
  ename string,
  job string,
  sal double,
  deptno int
)
row format delimited fields terminated by '\t';
```

等值 Join（Hive 仅支持等值连接）：

```sql
select e.empno, e.ename, d.dname
from emp e
join dept d
on e.deptno = d.deptno;
```

左外连接：

```sql
select e.empno, e.ename, d.deptno
from emp e
left join dept d
on e.deptno = d.deptno;
```

满外连接：

```sql
select e.empno, e.ename, d.deptno
from emp e
full join dept d
on e.deptno = d.deptno;
```

多表连接：

```sql
create table if not exists location(
  loc int,
  loc_name string
)
row format delimited fields terminated by '\t';

select e.ename, d.dname, l.loc_name
from emp e
join dept d on d.deptno = e.deptno
join location l on d.loc = l.loc;
```

笛卡尔积（慎用）：

```sql
select empno, dname
from emp, dept;
```

Union / Union All：

```sql
select * from emp where deptno = 30
union
select * from emp where deptno = 40;

-- union all 不去重
```

要求：

1) 两个查询列数必须一致  
2) 对应列类型必须一致

### 4.4 排序（Order/Sort/Distribute/Cluster）

> 提示：演示时可设置 reduce 数量。

```sql
set mapreduce.job.reduces = 3;
```

#### 全局排序：ORDER BY

```sql
select * from emp order by sal desc;
```

#### 每个 Reduce 内排序：SORT BY

```sql
insert overwrite local directory '/opt/module/hive/datas/sortby-result'
select * from emp sort by deptno desc;
```

#### 分区：DISTRIBUTE BY（常与 sort by 配合）

```sql
insert overwrite local directory '/opt/module/hive/datas/distribute-result'
select *
from emp
distribute by deptno
sort by sal desc;
```

说明：

- 分区规则：`hash(字段) % reduce_num` 相同落同一 reducer
- 语法要求：`distribute by` 必须在 `sort by` 之前

#### 分区排序：CLUSTER BY

当 `distribute by` 与 `sort by` 字段相同，可用 `cluster by`（只支持升序）：

```sql
select * from emp cluster by deptno;
```

---

## 5. 函数（Functions）

### 5.1 查看函数

```sql
show functions;
desc function upper;
desc function extended upper;
```

### 5.2 数值/字符串/日期/流程控制/集合函数

#### 数值函数

```sql
select round(3.3);  -- 3
select ceil(3.1);   -- 4
select floor(4.8);  -- 4
```

#### 字符串函数

```sql
-- substring
select substring('atguigu', 3, 2);

-- replace
select replace('a-b-c', '-', '_');

-- regexp_replace（正则替换）
select regexp_replace('abc123', '\\d+', 'X');

-- regexp（正则匹配）
select 'dfsaaaa' regexp 'dfsa+';

-- repeat
select repeat('123', 3);

-- split
select split('a-b-c-d', '-');

-- nvl
select nvl(null, 'fallback');

-- concat / concat_ws
select concat('a','b','c');
select concat_ws('-', 'a','b','c');

-- get_json_object
select get_json_object(
  '[{"name":"大海海","sex":"男","age":"25"},{"name":"小宋宋","sex":"男","age":"47"}]',
  '$.[0]'
);
```

#### 日期函数

```sql
select unix_timestamp('2022/08/08 08-08-08','yyyy/MM/dd HH-mm-ss');
select from_unixtime(1700000000, 'yyyy-MM-dd');

select current_date;
select current_timestamp;

select month('2022-08-08 08:08:08');
select day('2022-08-08 08:08:08');
select hour('2022-08-08 08:08:08');

select datediff('2022-08-10', '2022-08-08');
select date_add('2022-08-08', 2);
select date_sub('2022-03-01', 1);
select date_format('2022-08-08','yyyy年-MM月-dd日');
```

#### 流程控制

```sql
-- case when
select
  case
    when sal >= 5000 then 'A'
    when sal >= 3000 then 'B'
    else 'C'
  end as level
from emp;

-- if
select if(sal > 3000, 'high', 'low') from emp;
```

#### 集合函数

```sql
-- size
select size(friends) from teacher;

-- map / map_keys / map_values
select map('xiaohai',1,'dahai',2);
select map_keys(map('k1',1,'k2',2));
select map_values(map('k1',1,'k2',2));

-- array / array_contains / sort_array
select array('1','2','3','4');
select array_contains(array('a','b'), 'b');
select sort_array(array('a','d','c'));

-- struct / named_struct
select struct('name','age','weight');
select named_struct('name','xiaosong','age',18,'weight',80);
```

### 5.3 自定义函数 UDF/UDAF/UDTF

分类：

- **UDF**：一进一出
- **UDAF**：多进一出（如 count/max/min）
- **UDTF**：一进多出（如 explode）

开发/使用流程（概览）：

1. 继承 Hive 类：  
   - `org.apache.hadoop.hive.ql.udf.generic.GenericUDF`  
   - `org.apache.hadoop.hive.ql.udf.generic.GenericUDTF`
2. 实现抽象方法
3. Hive 中注册函数并使用

#### 临时函数（会话级）

```sql
add jar /opt/module/hive/datas/myudf.jar;

create temporary function my_len
as "cc.ivera.hive.udf.MyUDF";

select ename, my_len(ename) as ename_len from emp;

drop temporary function my_len;
```

#### 永久函数（库级，依赖 HDFS jar）

```bash
hdfs dfs -mkdir /udf
hdfs dfs -put my_len.jar /udf/my_len.jar
```

```sql
create function my_len2
as "cc.ivera.hive.udf.MyUDF"
using jar "hdfs://hadoop102:8020/udf/myudf.jar";

select ename, my_len2(ename) as ename_len from emp;

drop function my_len2;
```

---

## 6. 分区表与分桶表

### 6.1 分区表

**分区**：把大表按业务维度拆成多个目录（路径级隔离），查询可通过分区字段减少扫描。

建表：

```sql
create table dept_partition(
  deptno int,
  dname  string,
  loc    string
)
partitioned by (day string)
row format delimited fields terminated by '\t';
```

写数据（load）：

```sql
load data local inpath '/opt/module/hive/datas/dept_20220401.log'
into table dept_partition
partition(day='20220401');
```

读数据：

```sql
select deptno, dname, loc, day
from dept_partition
where day = '20220401';
```

分区操作：

```sql
show partitions dept_partition;

alter table dept_partition add partition(day='20220403');
alter table dept_partition add partition(day='20220404') partition(day='20220405');

alter table dept_partition drop partition(day='20220403');
alter table dept_partition drop partition(day='20220404'), partition(day='20220405');
```

修复分区（元数据与 HDFS 不一致时）：

```sql
msck repair table dept_partition;
-- 或
msck repair table dept_partition add partitions;
msck repair table dept_partition drop partitions;
msck repair table dept_partition sync partitions;
```

### 6.2 动态分区

含义：insert 时分区值不手写，由数据行决定。

关键参数：

```sql
set hive.exec.dynamic.partition=true;
set hive.exec.dynamic.partition.mode=nonstrict;
set hive.exec.max.dynamic.partitions=1000;
set hive.exec.max.dynamic.partitions.pernode=100;
set hive.exec.max.created.files=100000;
set hive.error.on.empty.partition=false;
```

案例：按 `loc` 动态写入分区

```sql
create table dept_partition_dynamic(
  id int,
  name string
)
partitioned by (loc int)
row format delimited fields terminated by '\t';

set hive.exec.dynamic.partition.mode = nonstrict;

insert into table dept_partition_dynamic
partition(loc)
select deptno, dname, loc
from dept;
```

### 6.3 分桶表

**分桶**：更细粒度的数据组织（文件级），对 shuffle/采样/某些 join 优化有帮助。

建表：

```sql
create table stu_buck(
  id int,
  name string
)
clustered by(id)
into 4 buckets
row format delimited fields terminated by '\t';
```

导入：

```sql
load data local inpath '/opt/module/hive/datas/student.txt'
into table stu_buck;
```

分桶排序表：

```sql
create table stu_buck_sort(
  id int,
  name string
)
clustered by(id) sorted by(id)
into 4 buckets
row format delimited fields terminated by '\t';

load data local inpath '/opt/module/hive/datas/student.txt'
into table stu_buck_sort;
```

### 6.4 分区 vs 分桶对比表

| 项目 | 分区（Partition） | 分桶（Bucket） |
|---|---|---|
| 作用 | 减少扫描目录/文件（裁剪分区） | 减少 shuffle / 更均匀切分数据 |
| 查询常见写法 | `WHERE dt='20240101'` | `WHERE user_id=...`（配合分桶字段） |
| 常搭配维度 | 时间、地区、业务日期 | 用户/ID/订单ID 等高基数字段 |
| 存储表现 | 多目录 | 少目录内多文件（桶文件） |
