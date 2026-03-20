package cc.ivera.common.advice;

import com.alibaba.fastjson.JSON;
import cc.ivera.common.annotation.Cache;
import cc.ivera.common.intercepter.GlobalCacheInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.Duration;

@Slf4j
//@ControllerAdvice
public class GlobalCacheResponseBodyAdvice implements ResponseBodyAdvice {

    @Value("${cache.enable}")
    private Boolean enable;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // 全局缓存开关处于开启状态&是get请求&包含了@Cache注解
        return enable && returnType.hasMethodAnnotation(GetMapping.class)
                && returnType.hasMethodAnnotation(Cache.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (ObjectUtils.isEmpty(body)) {
            return null;
        }
        try {
            //构建cacheData
            String cacheData;
            if (body instanceof String) {
                cacheData = (String) body;
            } else {
                cacheData = JSON.toJSONString(body);
            }
            //构建缓存Key
            String cacheKey = GlobalCacheInterceptor.createCacheKey();

            //获取Cache注解
            Cache cache = returnType.getMethodAnnotation(Cache.class);

            //写缓存
            redisTemplate.opsForValue().set(cacheKey,cacheData, Duration.ofSeconds(Long.valueOf(cache.time())));
        } catch (Exception e) {
            log.error("全局缓存拦截器异常：{}",e);
            e.printStackTrace();
        }
        return body;
    }
}




/*@Value("${cache.pack}")
private String cachePackName;*/


//构建缓存Key
//String cacheKey = GlobalCacheInterceptor.getCacheKey(cache);


//写缓存
/*CachePack cachePack = null;
if("redis".equals(cachePackName)){
    cachePack = new RedisCache(redisTemplate);
}else if("memecache".equals(cachePackName)){
    cachePack = new MemerCache();
}
cachePack.writeCacheData(cacheKey,cacheData, Duration.ofSeconds(Long.valueOf(cache.time())));*/