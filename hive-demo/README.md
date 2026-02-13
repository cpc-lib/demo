### 自定义UDF函数

编程步骤
```
（1）继承Hive提供的类
org.apache.hadoop.hive.ql.udf.generic.GenericUDF
org.apache.hadoop.hive.ql.udf.generic.GenericUDAF;
（2）实现类中的抽象方法
（3）在hive的命令行窗口创建函数
添加jar。
add jar linux_jar_path
创建function。
create [temporary] function [dbname.]function_name AS class_name;
（4）在hive的命令行窗口删除函数
drop [temporary] function [if exists] [dbname.]function_name;
```

### 建表语句
```
create table if not exists emp(
empno int, -- 员工编号
ename string, -- 员工姓名
job string, -- 员工岗位（大数据工程师、前端工程师、java工程师）
sal double, -- 员工薪资
deptno int -- 部门编号
)
row format delimited fields terminated by '\t';
```


### 创建临时函数
```
（1）打成jar包上传到服务器/opt/module/hive/datas/myudf.jar
（2）将jar包添加到hive的classpath，临时生效
hive (default)> add jar /opt/module/hive/datas/myudf.jar;
（3）创建临时函数与开发好的java class关联
hive (default)>
create temporary function my_len
as "cc.ivera.hive.udf.MyUDF";
（4）即可在hql中使用自定义的临时函数
select
ename,
my_len(ename) ename_len
from emp;
5）删除临时函数
hive (default)> drop temporary function my_len;

注意：临时函数只跟会话有关系，跟库没有关系。只要创建临时函数的会话不断，在当前会话下，任意一个库都可以使用，其他会话全都不能使用。
```


创建永久函数
```
hdfs dfs -mkdir /udf
hdfs dfs -put my_len.jar /udf/my_len.jar

（1）创建永久函数
注意：因为add jar本身也是临时生效，所以在创建永久函数的时候，需要制定路径（并且因为元数据的原因，这个路径还得是HDFS上的路径）。
hive (default)>
create function my_len2
as "cc.ivera.hive.udf.MyUDF"
using jar "hdfs://hadoop102:8020/udf/myudf.jar";
（2）即可在hql中使用自定义的永久函数 hive (default)>
select
ename,
my_len2(ename) ename_len
from emp;
（3）删除永久函数 hive (default)> drop function my_len2;
注意：永久函数跟会话没有关系，创建函数的会话断了以后，其他会话也可以使用。
永久函数创建的时候，在函数名之前需要自己加上库名，如果不指定库名的话，会默认把当前库的库名给加上。
永久函数使用的时候，需要在指定的库里面操作，或者在其他库里面使用的话加上，库名.函数名。
```

