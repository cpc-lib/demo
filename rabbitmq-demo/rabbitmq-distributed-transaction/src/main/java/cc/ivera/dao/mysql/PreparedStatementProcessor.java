package cc.ivera.dao.mysql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @version v1.0
 * @description
 * @since 2020/2/5 11:51
 */
@FunctionalInterface
public interface PreparedStatementProcessor {

    void process(PreparedStatement ps) throws SQLException;
}
