//package cc.ivera.mp.handler;
//
//import cn.hutool.crypto.SecureUtil;
//import cn.hutool.crypto.symmetric.AES;
//import org.apache.ibatis.type.BaseTypeHandler;
//import org.apache.ibatis.type.JdbcType;
//import org.apache.ibatis.type.MappedJdbcTypes;
//import org.springframework.util.StringUtils;
//
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//import java.sql.CallableStatement;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//
////表示该处理器处理的数据类型(不是本系统的数据直接查询报错)
//@MappedJdbcTypes(JdbcType.VARCHAR)
//public class EncryptHandler extends BaseTypeHandler<String> {
//    /**
//     * 线上运行后勿修改，会影响已加密数据解密
//     */
//    private static final String KEYS = "Q7rgu3Gep4NMJxpk";
//
//
//    private static final Charset UTF8 = StandardCharsets.UTF_8;
//
//    /**
//     * 设置参数
//     */
//    @Override
//    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
//        if (StringUtils.isEmpty(parameter)) {
//            ps.setString(i, null);
//            return;
//        }
//        AES aes = SecureUtil.aes(KEYS.getBytes(UTF8));
//        String encrypt = aes.encryptHex(parameter);
//        ps.setString(i, encrypt);
//    }
//
//    /**
//     * 获取值
//     */
//    @Override
//    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
//        return decrypt(rs.getString(columnName));
//    }
//
//    /**
//     * 获取值
//     */
//    @Override
//    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
//        return decrypt(rs.getString(columnIndex));
//    }
//
//    /**
//     * 获取值
//     */
//    @Override
//    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
//        return decrypt(cs.getString(columnIndex));
//    }
//
//    public String decrypt(String value) {
//        if (null == value) {
//            return null;
//        }
//        return SecureUtil.aes(KEYS.getBytes(UTF8)).decryptStr(value);
//    }
//}