package cc.ivera.config;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;;

/**
 * druid 配置多数据源
 *
 * @author ruoyi
 */
@Configuration
@Import({MybatisPlusConfig.class})
@EnableConfigurationProperties({MybatisProperties.class})
public class DruidConfig {

    @Autowired
    private MybatisProperties properties;


    @Bean(name = "defaultSource")
    @ConfigurationProperties("spring.datasource.default")
    public DruidDataSource defaultDataSource(DruidProperties druidProperties) {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        return druidProperties.dataSource(dataSource);
    }

    @Bean(name = "platformSource")
    @ConfigurationProperties("spring.datasource.platform")
    public DruidDataSource platformDataSource(DruidProperties druidProperties) {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        return druidProperties.dataSource(dataSource);
    }


    @Primary
    @Bean(name = "dynamicDataSource")
    public DynamicDataSource dataSource(@Qualifier("defaultSource") DataSource defaultSource) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceNames.DEFAULT, defaultSource);
        setDataSource(targetDataSources, DataSourceNames.PLATFORM, "platformSource");
        return new DynamicDataSource(defaultSource, targetDataSources);
    }


    @Bean(name="platformTransactionManager")
    public PlatformTransactionManager platformTransactionManager(@Qualifier("dynamicDataSource") DynamicDataSource dynamicDataSource){
        return new DataSourceTransactionManager(dynamicDataSource);
    }


    /**
     * 设置数据源
     *
     * @param targetDataSources 备选数据源集合
     * @param sourceName        数据源名称
     * @param beanName          bean名称
     */
    public void setDataSource(Map<Object, Object> targetDataSources, String sourceName, String beanName) {
        try {
            DataSource dataSource = SpringUtils.getBean(beanName);
            targetDataSources.put(sourceName, dataSource);
        } catch (Exception e) {
        }
    }


    //声明SqlSessionFactory,定义测试多线程插入与单线程插入数据对比
    //使用mp,需要使用MybatisSqlSessionFacotry，否则BaseMapper方法报错Invaild bind statement
    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dynamicDataSource") DynamicDataSource dynamicDataSource,
                                               @Qualifier("globalConfig") GlobalConfig globalConfig,
                                               @Qualifier("mybatisConfiguration")MybatisConfiguration mybatisConfiguration) throws Exception {
        MybatisSqlSessionFactoryBean sqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dynamicDataSource);
        //配置mapperLocation的地址(mp的自动配置无效果！注意)
        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().
                getResources(properties.getLocationPattern()));
        sqlSessionFactoryBean.setGlobalConfig(globalConfig);
        sqlSessionFactoryBean.setConfiguration(mybatisConfiguration);
        //设置枚举类扫描地址(无需在实体类的字段上添加@Enum注解)
        sqlSessionFactoryBean.setTypeEnumsPackage(properties.getTypeEnumsPackage());
        //设置实体类的别名
        // 可以使用类名作为resultType
        sqlSessionFactoryBean.setTypeAliasesPackage(properties.getTypeAliasesPackage());
        return sqlSessionFactoryBean.getObject();
    }


    @Bean(name="sqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        SqlSessionTemplate sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
        return sqlSessionTemplate;
    }

}
