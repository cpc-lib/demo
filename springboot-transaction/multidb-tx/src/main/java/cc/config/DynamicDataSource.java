package cc.ivera.config;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.druid.pool.DruidDataSource;
import org.jasypt.encryption.StringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;


/**
 * 动态数据源
 * @Order 定义加载优先级别，数字越小优先级越高
 * @author ruoyi
 */
@Order(1)
public class DynamicDataSource extends AbstractRoutingDataSource {

    public static Logger log = LoggerFactory.getLogger(DynamicDataSource.class);
    private static String lastDb = "default";
    private static StringEncryptor ENCRYPT;
    public static ConcurrentHashMap<String, DataSource> dsMap = new ConcurrentHashMap();
    public static DynamicDataSource dynamicDataSource;


    @PostConstruct
    public void init(){
        dynamicDataSource=this;
    }


    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey()
    {
        return DynamicDataSourceContextHolder.getDataSource();
    }


    public static void setDataSource(String dataSource) {
        if (!StringUtils.isEmpty(dataSource)) {
            if (!dataSource.equals(DynamicDataSourceContextHolder.getDataSource())) {
                lastDb = DynamicDataSourceContextHolder.getDataSource();
                DynamicDataSourceContextHolder.setDataSource(dataSource);
            }
        }
    }

    public static String getDataSource() {
        String db = DynamicDataSourceContextHolder.getDataSource();
        if (StringUtils.isEmpty(db)) {
            db = "default";
            setDataSource(db);
        }

        return db;
    }

    public static void clearDataSource() {
        DynamicDataSourceContextHolder.clearDataSource();
    }

    public Map<Object, Object> getTargetSource() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = AbstractRoutingDataSource.class.getDeclaredField("targetDataSources");
        field.setAccessible(true);
        return (Map)field.get(this);
    }


    private static String decrypt(String encryptStr) {
        if (!StrUtil.startWith(encryptStr, "ENC(")) {
            return encryptStr;
        } else {
            if (ObjectUtil.isNull(ENCRYPT)) {
                ENCRYPT = (StringEncryptor) SpringUtil.getBean(StringEncryptor.class);
            }

            encryptStr = StrUtil.removePrefix(encryptStr, "ENC(");
            encryptStr = StrUtil.removeSuffix(encryptStr, ")");
            return ENCRYPT.decrypt(encryptStr);
        }
    }


    /**系统在初始化阶段，在yml中配置系统的连接信息，使用一个实体类@Component，去执行，获取后台配置的所有的连接信息
     * 可以将数据源设置到数据库中,然后根据fun功能捆绑的数据源指定特定的数据库，然后可以匹配一些功能字段完成某个数据表的查询
     */
    public static boolean managerDbSource(ToolDbSource toolDbsource,DruidProperties druidProperties) throws NoSuchFieldException, IllegalAccessException {
        Map<Object, Object> targetSource = dynamicDataSource.getTargetSource();
        String key = toolDbsource.getDbSourceName();
        if (targetSource.containsKey(key)) {
            targetSource.remove(key);
        }
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.type(DruidDataSource.class);
        dataSourceBuilder.driverClassName(toolDbsource.getDriverClassName());
        dataSourceBuilder.url(toolDbsource.getJbcUrl());
        dataSourceBuilder.username(decrypt(toolDbsource.getUsername()));
        dataSourceBuilder.password(decrypt(toolDbsource.getPassword()));
        DruidDataSource dataSource = (DruidDataSource)dataSourceBuilder.build();
        targetSource.put(key,dataSource);
        dynamicDataSource.setTargetDataSources(targetSource);
        dynamicDataSource.afterPropertiesSet();
        Iterator entrySet = targetSource.entrySet().iterator();

        while(entrySet.hasNext()) {
            Map.Entry<String, DataSource> entry = (Map.Entry)entrySet.next();
            log.info("DbSource Key = " + (String)entry.getKey());
        }
        return true;
    }



    public static boolean managerDbSourceList(List<ToolDbSource> toolDbSources, DruidProperties druidProperties) throws NoSuchFieldException, IllegalAccessException {
        Map<Object, Object> targetSource = dynamicDataSource.getTargetSource();
        ToolDbSource toolDbsource = null;
        for (int i = 0; i < toolDbSources.size(); i++) {
            toolDbsource=toolDbSources.get(i);
            if(toolDbsource!=null){
                toolDbsource=toolDbSources.get(i);
                String key = toolDbSources.get(i).getDbSourceName();
                if (targetSource.containsKey(key)) {
                    targetSource.remove(key);
                }
                DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
                dataSourceBuilder.type(DruidDataSource.class);
                dataSourceBuilder.driverClassName(toolDbsource.getDriverClassName());
                dataSourceBuilder.url(toolDbsource.getJbcUrl());
                dataSourceBuilder.username(decrypt(toolDbsource.getUsername()));
                dataSourceBuilder.password(decrypt(toolDbsource.getPassword()));
                DruidDataSource dataSource = (DruidDataSource)dataSourceBuilder.build();
                targetSource.put(key,dataSource);
            }
        }
        dynamicDataSource.setTargetDataSources(targetSource);
        dynamicDataSource.afterPropertiesSet();
        Iterator entrySet = targetSource.entrySet().iterator();

        while(entrySet.hasNext()) {
            Map.Entry<String, DataSource> entry = (Map.Entry)entrySet.next();
            log.info("DbSource Key = " + (String)entry.getKey());
        }
        return true;
    }




}
