package cc.ivera.dynamic;

import org.apache.ibatis.jdbc.SQL;

public class SqlStatement {

    //返回查看sql语句
    public String selectAll(){
        String sql=new SQL(){
            {
                SELECT("*");
                FROM("person");
            }
        }.toString();
        return sql;
    }

    //返回删除的sql语句
    public String delete(){
        String sql=new SQL(){
            {
               DELETE_FROM("person");
               WHERE("id=#{id}");
            }
        }.toString();
        return sql;
    }

    //放回修改的sql语句
    public String update(){
        String sql = new SQL(){
            {
                UPDATE("person");
                SET("name=#{name}");
                WHERE("id=#{id}");
            }
        }.toString();
        return sql;
    }

    public String insert(){
        String sql = new SQL(){
            {
                INSERT_INTO("person");
                //INTO_VALUES("#{id},#{name},#{age}");
                VALUES("id,name,age","#{id},#{name},#{age}");
            }
        }.toString();
        return sql;
    }
}
