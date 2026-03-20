package cc.ivera;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


public class C3P0Test {
    @Test
    public void testC3P0() throws SQLException {
        //配置使用C3P0作为数据库推荐使用xml配置文件
        //读取xml配置信息中config的名称
        DataSource ds = new ComboPooledDataSource("mySource");
        for (int i = 0; i <= 20; i++) {
            Connection connection = ds.getConnection();
            System.out.println(i + ":" + connection);
        }
    }
}
