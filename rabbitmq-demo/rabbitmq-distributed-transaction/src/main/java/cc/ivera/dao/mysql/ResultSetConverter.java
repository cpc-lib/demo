package cc.ivera.dao.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @version v1.0
 * @description
 * @since 2020/2/5 11:35
 */
@FunctionalInterface
public interface ResultSetConverter<T> {

    T convert(ResultSet resultSet) throws SQLException;
}
