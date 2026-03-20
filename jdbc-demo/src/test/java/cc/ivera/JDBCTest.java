package cc.ivera;

import org.junit.Test;
import cc.ivera.pojo.Account;
import cc.ivera.util.JDBCUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class JDBCTest {

    @Test
    public void testQuery() throws SQLException {
        List<Account> list = null;
        Connection connection = JDBCUtil.getConnection();
        String sql = "select id,name,balance from account";
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);
        list = new ArrayList<>();
        while (resultSet.next()) {
            Account account = new Account();
            account.setId(resultSet.getInt("id"));
            account.setName(resultSet.getString("name"));
            account.setBalance(resultSet.getInt("balance"));
            list.add(account);
        }
        list.forEach(s -> System.out.println(s));
        JDBCUtil.close(resultSet, statement, connection);
    }

    @Test
    public void testUpdate() throws SQLException {
        Connection connection = JDBCUtil.getConnection();
        String sql = "update account set balance = 500 where id =1";
        Statement statement = connection.createStatement();
        int i = statement.executeUpdate(sql);
        System.out.println(i);
        JDBCUtil.close(statement, connection);
    }

    @Test
    public void testDelete() throws SQLException {
        Connection connection = JDBCUtil.getConnection();
        String sql = "delete from account where id=1";
        Statement statement = connection.createStatement();
        int i = statement.executeUpdate(sql);
        System.out.println(i);
        JDBCUtil.close(statement, connection);
    }

    @Test
    public void testInsert() throws SQLException {
        Connection connection = JDBCUtil.getConnection();
        String sql = "insert into account (id,name,balance) value (5,'WangYuting',500000)";
        Statement statement = connection.createStatement();
        int i = statement.executeUpdate(sql);
        System.out.println(i);
        JDBCUtil.close(statement, connection);
    }

    @Test
    public void testPage() throws SQLException {
        Connection connection = JDBCUtil.getConnection();
        String sql = "select id,name,balance from account limit ";
        System.out.print("请输入查询页码:");
        int pageNum = new Scanner(System.in).nextInt();
        System.out.print("请输入查询条数:");
        int pageSize = new Scanner(System.in).nextInt();
        //使用stringBuffer来进行字符串的拼接
        new StringBuilder();
        StringBuffer stringBuffer = new StringBuffer();
        String newSQL = stringBuffer
                .append(sql)
                .append((pageNum - 1) * pageSize)
                .append(",")
                .append(pageSize).toString();

        System.out.println(newSQL);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(newSQL);
        List<Account> list = new ArrayList<>();
        while (resultSet.next()) {
            Account account = new Account();
            account.setId(resultSet.getInt("id"));
            account.setName(resultSet.getString("name"));
            account.setBalance(resultSet.getInt("balance"));
            list.add(account);
        }
        if (list != null) {
            list.stream().forEach(account -> System.out.println(account));
        }
        JDBCUtil.close(resultSet,statement,connection);
    }


    @Test
    public void testCommit() throws SQLException {
        Connection connection = JDBCUtil.getConnection();
        connection.setAutoCommit(false);
        String sqlA = "update account set balance=balance+100 where id=3";
        String sqlB = "update account set balance=balance+100 where id=2";
        Statement statement = connection.createStatement();
        try {
            statement.executeUpdate(sqlA);
            int i=1/0;
            statement.executeUpdate(sqlB);
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
        }
        JDBCUtil.close(statement,connection);
    }
}
