package cc.ivera;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.junit.Test;
import cc.ivera.datasource.util.DruidUtil;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class DruidTest {
    @Test
    public void testDruid() throws Exception {
        //使用druid数据库连接池，推荐用properties配置文件
        //数据源配置
        Properties properties=new Properties();
        //通过当前类的class对象获取资源文件
        ClassLoader classLoader = DruidTest.class.getClassLoader();
        InputStream resourceAsStream = classLoader.getResourceAsStream("druid.properties");
        properties.load(resourceAsStream);
        //返回的是DataSource，不是DruidDataSource
        DataSource dataSource = DruidDataSourceFactory.createDataSource(properties);


        for(int i=0;i<=30;i++){
            //获取连接
            Connection connection = dataSource.getConnection();
            System.out.println(i+":"+connection);
        }


        /*//Statement接口
        Statement statement = connection.createStatement();
        String sql1 = "insert into tb_student (name,age) values ('chy',20)";
        statement.executeUpdate(sql1);

        //PreparedStatement接口
        String sql2 = "insert into tb_student (name,age) values ('chy',22)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql2);
        preparedStatement.execute();*/

        //关闭连接
        //connection.close();
    }


    @Test
    public void testDruidUtil(){
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DruidUtil.getConnection();
            String sql = "insert into account (id,name,balance) values (5,?,?)";

            preparedStatement = connection.prepareStatement(sql);
            //给占位符？进行赋值
            preparedStatement.setString(1,"wzm");
            preparedStatement.setInt(2,3000);
            //使用无参方式执行
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        DruidUtil.close(null,preparedStatement,connection);
    }
}
