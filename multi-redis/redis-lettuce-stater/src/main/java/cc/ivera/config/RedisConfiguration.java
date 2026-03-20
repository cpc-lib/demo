package cc.ivera.config;

import cc.ivera.entity.RedisEntity;
import cc.ivera.manager.RedisManager;
import cc.ivera.register.RedisRegister;
import cc.ivera.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @description:
 * @author: jianjun.ren
 * @date: Created in 2020/9/27 22:53
 */
@AutoConfigureBefore({RedisAutoConfiguration.class})
@Import(RedisRegister.class)
@EnableCaching
@Configuration
public class RedisConfiguration implements EnvironmentAware , ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    private static String key1 = "redisTemplate";
    private static String key2 = "stringRedisTemplate";

    Map<String, RedisTemplate> redisTemplateMap = new HashMap<>();
    Map<String, StringRedisTemplate> stringRedisTemplateMap = new HashMap<>();
    private Binder binder;
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        this.binder = Binder.get(this.environment);
    }
    @PostConstruct
    public Map<String,RedisTemplate> initRedisTemplate(){
        RedisEntity redisEntity;
        try {
            redisEntity = binder.bind("knife4j.redis", RedisEntity.class).get();
        } catch (NoSuchElementException e) {
            throw new RuntimeException("Failed to configure knife4j redis: 'knife4j.redis' attribute is not specified and no embedded redis could be configured.");
        }

        //根据多个库实例化出多个连接池和Template
        List<Integer> databases = redisEntity.getDatabases();
        if(databases == null || databases.size() == 0){
            logger.warn("no config property knife4j.redis.databases , default use db0！！！");
            databases.add(0);
        }

        //根据指定的数据库个数来加载对应的RedisTemplate
        for (Integer database : databases) {
            String key = key1 + database;
            RedisTemplate redisTemplate = applicationContext.getBean(key , RedisTemplate.class);
            if(redisTemplate != null){
                redisTemplateMap.put(key , redisTemplate);
            }

            key = key2 + database;
            if(stringRedisTemplateMap != null){
                StringRedisTemplate stringRedisTemplate = applicationContext.getBean(key , StringRedisTemplate.class);
                stringRedisTemplateMap.put(key , stringRedisTemplate);
            }
        }
        if(redisTemplateMap.size() == 0 && stringRedisTemplateMap.size() == 0){
            throw new RuntimeException("load redisTemplate failure , please check knife4j.redis property config！！！");
        }
        return redisTemplateMap;
    }

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Bean
    public RedisManager knife4jRedisManager(){
        return new RedisManager(redisTemplateMap , stringRedisTemplateMap);
    }

    @Bean
    public RedisBaseUtil redisBaseUtil(){
        return new RedisBaseUtil();
    }
    @Bean
    public RedisValUtil redisValUtil(){
        return new RedisValUtil();
    }
    @Bean
    public RedisListUtil redisListUtil(){
        return new RedisListUtil();
    }
    @Bean
    public RedisHashUtil redisHashUtil(){
        return new RedisHashUtil();
    }
    @Bean
    public RedisSetUtil redisSetUtil(){
        return new RedisSetUtil();
    }


}
