package cc.ivera;

import cc.ivera.pojo.Account;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author e2607
 * @version 1.0
 * @description: JDBC
 * @date 12/12/2021 9:01 PM
 */
public class Main {
    public static void main(String[] args) {
        testUpdate();
        testQuery();
    }

    private static void testQuery() {
        List<Account> list = null;
        try {
            //1.注册驱动
            Class.forName("com.mysql.jdbc.Driver");
            //2.获取数据库连接对象
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
            //3.定义sql语句
            String sql = "select id,name,balance from account ";
            //4.获取执行sql的对象
            Statement statement = connection.createStatement();
            //5.执行sql语句，获取影响条数
            ResultSet resultSet = statement.executeQuery(sql);
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
    }

    private static void testUpdate() {
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
}
