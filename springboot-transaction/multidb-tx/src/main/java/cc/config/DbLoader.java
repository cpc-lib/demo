package cc.ivera.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${BANK0_DB_URL:jdbc:mysql://localhost:3306/bank0?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useLegacyDatetimeCode=false&useSSL=false&useJDBCCompliantTimezoneShift=true&serverTimezone=Asia/Shanghai}")
    private String bank0Url;
    @Value("${BANK0_DB_USERNAME:root}")
    private String bank0Username;
    @Value("${BANK0_DB_PASSWORD:root}")
    private String bank0Password;

    @Value("${REMOTE_DB_URL:jdbc:mysql://127.0.0.1:3306/remote?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useLegacyDatetimeCode=false&useSSL=false&useJDBCCompliantTimezoneShift=true&serverTimezone=Asia/Shanghai}")
    private String remoteUrl;
    @Value("${REMOTE_DB_USERNAME:root}")
    private String remoteUsername;
    @Value("${REMOTE_DB_PASSWORD:}")
    private String remotePassword;

    @PostConstruct
    public void init() throws NoSuchFieldException, IllegalAccessException {
        ToolDbSource toolDbsource1=new ToolDbSource();
        toolDbsource1.setId("1");
        toolDbsource1.setDbSourceName("bank0");
        toolDbsource1.setPassword(bank0Password);
        toolDbsource1.setUsername(bank0Username);
        toolDbsource1.setDriverClassName("com.mysql.cj.jdbc.Driver");
        toolDbsource1.setJbcUrl(bank0Url);

        ToolDbSource toolDbsource2=new ToolDbSource();
        toolDbsource2.setId("2");
        toolDbsource2.setDbSourceName("remote");
        toolDbsource2.setPassword(remotePassword);
        toolDbsource2.setUsername(remoteUsername);
        toolDbsource2.setDriverClassName("com.mysql.cj.jdbc.Driver");
        toolDbsource2.setJbcUrl(remoteUrl);

        List<ToolDbSource> toolDbSources = new ArrayList<>();
        toolDbSources.add(toolDbsource1);
        toolDbSources.add(toolDbsource2);
        DynamicDataSource.managerDbSourceList(toolDbSources,druidProperties);
    }
}
