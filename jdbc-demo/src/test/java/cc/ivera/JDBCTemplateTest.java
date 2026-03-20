package cc.ivera;

import org.junit.Test;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import cc.ivera.datasource.util.DruidUtil;
import cc.ivera.pojo.Account;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class JDBCTemplateTest {

    @Test
    public void testInsert(){
        //JdbcTemplate自动完成了资源的归还，简化了操作。
        JdbcTemplate jdbcTemplate = new JdbcTemplate(DruidUtil.getDataSource());
        String sql = "insert into account (id, name, balance) values (6,?,?)";
        int count = jdbcTemplate.update(sql, "sun", 50000);
        System.out.println(count);
    }

    @Test
    public void testUpdate(){
        //JdbcTemplate自动完成了资源的归还，简化了操作。
        JdbcTemplate jdbcTemplate = new JdbcTemplate(DruidUtil.getDataSource());
        String sql = "update account set balance=balance+500";
        int count = jdbcTemplate.update(sql);
        System.out.println(count);
    }

    @Test
    public void testDelete(){
        //JdbcTemplate自动完成了资源的归还，简化了操作。
        JdbcTemplate jdbcTemplate = new JdbcTemplate(DruidUtil.getDataSource());
        String sql = "delete from  account where id = 1";
        int count = jdbcTemplate.update(sql);
        System.out.println(count);
    }


    @Test
    public void testQueryV1(){
        //JdbcTemplate自动完成了资源的归还，简化了操作。
        JdbcTemplate jdbcTemplate = new JdbcTemplate(DruidUtil.getDataSource());
        String sql = "select * from account";
        List<Account> list = jdbcTemplate.query(sql, new RowMapper<Account>() {
            @Override
            public Account mapRow(ResultSet resultSet, int i) throws SQLException {
                Account account = new Account();
                account.setId(resultSet.getInt(1));
                account.setName(resultSet.getString(2));
                account.setBalance(resultSet.getInt(3));
                return account;
            }
        });
        for (Account account : list) {
            System.out.println(account);
        }
    }

    @Test
    public void testQueryV2(){
        //JdbcTemplate自动完成了资源的归还，简化了操作。
        JdbcTemplate jdbcTemplate = new JdbcTemplate(DruidUtil.getDataSource());
        String sql = "select * from account";
        //BeanPropertyRowMapper<T>(T.class)
        List<Account> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<Account>(Account.class));
        for (Account account : list) {
            System.out.println(account);
        }
    }

    @Test
    public void testQueryForMap(){
        JdbcTemplate jdbcTemplate = new JdbcTemplate(DruidUtil.getDataSource());
        String sql = "select * from account where id = ?";
        Map<String, Object> stringObjectMap = jdbcTemplate.queryForMap(sql, 5);
        System.out.println(stringObjectMap);
    }

    @Test
    public void testQueryForList(){
        JdbcTemplate jdbcTemplate = new JdbcTemplate(DruidUtil.getDataSource());
        String sql = "select * from account where id = ? or id = ?";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, 5,6);
        list.forEach(item-> System.out.println(item));
    }
}
