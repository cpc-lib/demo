package cc.ivera;

import org.junit.Test;
import cc.ivera.pojo.Account;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author e2607
 * @version 1.0
 * @description: JDBC
 * @date 12/12/2021 9:01 PM
 */
public class SQLTest {

    @Test
    public void testQuery() {
        List<Account> list = null;
        ResultSet resultSet = null;
        Statement statement = null;
        Connection connection = null;
        try {
            //1.注册驱动
            Class.forName("com.mysql.jdbc.Driver");
            //2.获取数据库连接对象
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
            //3.定义sql语句
            String sql = "select id,name,balance from account ";
            //4.获取执行sql的对象
            statement = connection.createStatement();
            //5.执行sql语句，获取影响条数
            resultSet = statement.executeQuery(sql);
            list = new ArrayList<>();
            while (resultSet.next()) {
                Account account = new Account();
                account.setId(resultSet.getInt("id"));
                account.setName(resultSet.getString("name"));
                account.setBalance(resultSet.getInt("balance"));
                list.add(account);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (list != null) {
            list.stream().forEach(account -> System.out.println(account));
            System.out.println("--------------------------------------");
            list.forEach(account -> System.out.println(account));
        }
        try {
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testUpdate() {
        try {
            //1.注册驱动
            Class.forName("com.mysql.jdbc.Driver");
            //2.获取数据库连接对象
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
            //3.定义sql语句
            String sql = "update account set balance = 500 where id =1";
            //4.获取执行sql的对象
            Statement statement = connection.createStatement();
            //5.执行sql语句，获取影响条数
            int i = statement.executeUpdate(sql);
            //6.答应影响条数
            System.out.println(i);
            statement.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testDelete() {
        try {
            //1.注册驱动
            Class.forName("com.mysql.jdbc.Driver");
            //2.获取数据库连接对象
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
            //3.定义sql语句
            String sql = "delete from account where id =1";
            //4.获取执行sql的对象
            Statement statement = connection.createStatement();
            //5.执行sql语句，获取影响条数
            int i = statement.executeUpdate(sql);
            //6.答应影响条数
            System.out.println(i);
            statement.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testInsert() {
        try {
            //1.注册驱动
            Class.forName("com.mysql.jdbc.Driver");
            //2.获取数据库连接对象
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
            //3.定义sql语句
            String sql = "insert into account (id,name,balance) value (1,'WangYuting',500000)";
            //4.获取执行sql的对象
            Statement statement = connection.createStatement();
            //5.执行sql语句，获取影响条数
            int i = statement.executeUpdate(sql);
            //6.答应影响条数
            System.out.println(i);
            statement.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPage() {
        List<Account> list = null;
        ResultSet resultSet = null;
        Statement statement = null;
        Connection connection = null;
        try {
            //1.注册驱动
            Class.forName("com.mysql.jdbc.Driver");
            //2.获取数据库连接对象
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
            //3.定义sql语句
            String sql = "select id,name,balance from account limit ";
            System.out.print("请输入查询页码:");
            int pageNum = new Scanner(System.in).nextInt();
            System.out.println("请输入查询条数:");
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
            //4.获取执行sql的对象
            statement = connection.createStatement();
            //5.执行sql语句，获取影响条数
            resultSet = statement.executeQuery(newSQL);
            list = new ArrayList<>();
            while (resultSet.next()) {
                Account account = new Account();
                account.setId(resultSet.getInt("id"));
                account.setName(resultSet.getString("name"));
                account.setBalance(resultSet.getInt("balance"));
                list.add(account);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (list != null) {
            list.stream().forEach(account -> System.out.println(account));
        }
        try {
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCommit() throws SQLException, ClassNotFoundException {
        List<Account> list = null;
        //1.注册驱动
        Class.forName("com.mysql.jdbc.Driver");
        //2.获取数据库连接对象
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
        connection.setAutoCommit(false);
        //3.定义sql语句
        String sqlA = "update account set balance=balance+100 where id=3";
        String sqlB = "update account set balance=balance+100 where id=2";
        //4.获取执行sql的对象
        Statement statement = connection.createStatement();
        //5.执行sql语句，获取影响条数
        try {
            statement.executeUpdate(sqlA);
            int i = 1 / 0;
            statement.executeUpdate(sqlB);
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
        }
        statement.close();
        connection.close();
    }

}
