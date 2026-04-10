package cc.ivera.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor paginationInnerInterceptor =
                new PaginationInnerInterceptor(DbType.MYSQL);

        // 超过最大页数后是否重定向到第一页，默认 false
        paginationInnerInterceptor.setOverflow(true);

        // 单页最大条数，防止一次查太多
        paginationInnerInterceptor.setMaxLimit(100L);

        // 分页插件要尽量放最后
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}