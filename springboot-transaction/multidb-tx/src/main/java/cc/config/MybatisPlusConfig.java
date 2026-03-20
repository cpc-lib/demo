package cc.ivera.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(TableProperties.class)
public class MybatisPlusConfig {

    @Resource
    private MetaObjectHandler metaObjectHandler;

    @Autowired
    private TableProperties tableProperties;


    /**
     * 自定义批量插入 SQL 注入器
     */
    @Bean
    public InsertBatchSqlInjector insertBatchSqlInjector() {
        return new InsertBatchSqlInjector();
    }


    /***
     * DbConfig(idType=ASSIGN_ID,
     * tablePrefix=null,
     * schema=null,
     * columnFormat=null,
     * propertyFormat=null,
     * replacePlaceholder=false,
     * escapeSymbol=null,
     * tableUnderline=true,
     * capitalMode=false,
     * keyGenerators=null,
     * logicDeleteField=null,
     * logicDeleteValue=1,
     * logicNotDeleteValue=0,
     * insertStrategy=NOT_NULL,
     * updateStrategy=NOT_NULL,
     * selectStrategy=null,
     * whereStrategy=NOT_NULL)
     * @return
     */
    @Bean(name = "globalConfig")
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = GlobalConfigUtils.defaults();
        //globalConfig.setSqlInjector(new DefaultSqlInjector());
        //采用用默认DbConfig配置
        //mybatis-plus自定义公用数据填充
        //获取DbConfig参数,设置IdType
        GlobalConfig.DbConfig dbConfig = globalConfig.getDbConfig();

        //声明IdType生成策略，但是需要开发者去实现代码
        dbConfig.setIdType(IdType.INPUT);

        //表的前缀为tb_ 通用mapper
        dbConfig.setTablePrefix(tableProperties.getTablePrefix());
        //逻辑删除的配置
        dbConfig.setLogicDeleteField(tableProperties.getLogicDeleteField());
        dbConfig.setLogicDeleteValue(tableProperties.getLogicDeleteValue());
        dbConfig.setLogicNotDeleteValue(tableProperties.getLogicNotDeleteValue());
        globalConfig.setMetaObjectHandler(metaObjectHandler);
        return globalConfig;
    }


    @Bean(name = "mybatisInterceptor")
    public MybatisPlusInterceptor mybatisInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //指定分页插件
        /*采用单一数据库可以提高程序的处理速度*/
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        //添加乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }


    @Bean(name = "mybatisConfiguration")
    public MybatisConfiguration mybatisConfiguration(@Qualifier("mybatisInterceptor") Interceptor mybatisInterceptor) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setCallSettersOnNulls(true);
        //开启驼峰配置
        configuration.setMapUnderscoreToCamelCase(true);
        //开启缓存
        configuration.setCacheEnabled(true);
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        configuration.setLazyLoadingEnabled(true);
        configuration.setAggressiveLazyLoading(true);
        configuration.addInterceptor(mybatisInterceptor);
        return configuration;
    }


}
