# springboot-geo
springboot-geo是基于 mybatis 和 tk通用mapper 扩展出对 mysql数据库（5.7以上版本） 地理位置geometry 类型字段和 虚拟列(Virtual Generated Column) 的支持，主要功能如下：
- 增加TypeHandler支持查询geometry字段，映射到GeoPoint类型
- 使tk通用mapper的insert/insertSelective方法支持GeoPoint类型的属性，插入到geometry类型字段
- 使tk通用mapper的updateByPrimaryKey/updateByPrimaryKeySelective方法支持GeoPoint类型的属性，更新到geometry类型字段
- 增加@VirtualGenerated注解，使tk通用mapper的insert/insertSelective/updateByPrimaryKey/updateByPrimaryKeySelective方法忽略虚拟列

## 如何使用
该项目基于spring boot2.0，下载代码，运行springboot-geo，开箱即用，你可以自由的修改代码，做任何你需要的定制。

### 距离单位米
``` sql
SELECT
id,name,
gis,
st_distance(gis,POINT(118.7196143, 31.770287)) / 0.0000111  AS distance
FROM
tb_location
HAVING distance<=20000
``` 
