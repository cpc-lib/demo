package cc.ivera.datasource.c3p0;


import com.mchange.v2.c3p0.ComboPooledDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class C3P0 {
  public static void main(String[] args) throws SQLException {
    //读取xml配置信息中config的名称
    DataSource ds = new ComboPooledDataSource("mySource");
    for(int i=0;i<=20;i++){
      Connection connection = ds.getConnection();
      System.out.println(i+":"+connection);
    }
  }
}
