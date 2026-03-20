package cc.ivera.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class DbLoader {

    @Autowired
    private DynamicDataSource dynamicDataSourceLoader;

    @Autowired
    private DruidProperties druidProperties;

    @PostConstruct
    public void init() throws NoSuchFieldException, IllegalAccessException {
        ToolDbSource toolDbsource1=new ToolDbSource();
        toolDbsource1.setId("1");
        toolDbsource1.setDbSourceName("bank0");
        toolDbsource1.setPassword("root");
        toolDbsource1.setUsername("root");
        toolDbsource1.setDriverClassName("com.mysql.cj.jdbc.Driver");
        toolDbsource1.setJbcUrl("jdbc:mysql://localhost:3306/bank0?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useLegacyDatetimeCode=false&useSSL=false&useJDBCCompliantTimezoneShift=true&serverTimezone=Asia/Shanghai");

        ToolDbSource toolDbsource2=new ToolDbSource();
        toolDbsource2.setId("2");
        toolDbsource2.setDbSourceName("remote");
        toolDbsource2.setPassword("Iloveyou3,000");
        toolDbsource2.setUsername("root");
        toolDbsource2.setDriverClassName("com.mysql.cj.jdbc.Driver");
        toolDbsource2.setJbcUrl("jdbc:mysql://114.132.210.77:3306/remote?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useLegacyDatetimeCode=false&useSSL=false&useJDBCCompliantTimezoneShift=true&serverTimezone=Asia/Shanghai");

        List<ToolDbSource> toolDbSources = new ArrayList<>();
        toolDbSources.add(toolDbsource1);
        toolDbSources.add(toolDbsource2);
        DynamicDataSource.managerDbSourceList(toolDbSources,druidProperties);
    }
}
