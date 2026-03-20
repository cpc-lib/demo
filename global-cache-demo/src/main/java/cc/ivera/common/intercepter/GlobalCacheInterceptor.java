package cc.ivera.common.intercepter;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import cc.ivera.common.annotation.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class GlobalCacheInterceptor implements HandlerInterceptor {
    @Value("${cache.enable}")
    private Boolean enable;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //如果缓存的全局开关没开,放行
        if (!enable) {
            return true;
        }
        //如果不是HandlerMethod,放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod method = (HandlerMethod) handler;
        //如果是非GET请求,放行
        if (!method.hasMethodAnnotation(GetMapping.class)) {
            return true;
        }
        //如果请求方法未被@Cache标注,放行
        if (!method.hasMethodAnnotation(Cache.class)) {
            return true;
        }
        //构建缓存key
        String cacheKey = createCacheKey();
        //缓存命中
        String cacheData = this.redisTemplate.opsForValue().get(cacheKey);
        //缓存未命中,放行
        if(ObjectUtils.isEmpty(cacheData)){
            return true;
        }
        //缓存命中,返回数据，截断请求
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        response.getWriter().write(cacheData);
        return false;
    }

    /**
     * 生成缓存中的key，规则：SERVER_CACHE_DATA_MD5(url + param + token)
     */
    public static String createCacheKey() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        //请求url
        String url = request.getRequestURI();
        //请求参数
        String param = JSON.toJSONString(request.getParameterMap());
        //请求Token
        String token = request.getHeader("Authorization");

        String data = url + "_" + param + "_" + token;
        String keyLastFix = SecureUtil.md5(data);

        return "SERVER_CACHE_DATA_" + keyLastFix;
    }
}





    /*
     * 获取缓存 key
     * @param cache
     * @return
     * @throws Exception
     /*
    public static String getCacheKey(Cache cache) throws Exception {
        String cacheKey;
        if(StringUtils.isEmpty(cache.key())){
            cacheKey = createRedisKey();
        }else{
            cacheKey = cache.key();
        }
        return cacheKey;
    }*/

/*@Value("${cache.pack}")
private String cachePackName;*/

//获取Cache注解
//Cache cache = method.getMethodAnnotation(Cache.class);

//构建缓存key
//String cacheKey = getCacheKey(cache);

//缓存命中
/*CachePack cachePack = null;
if("redis".equals(cachePackName)){
    cachePack = new RedisCache(redisTemplate);
}else if("memecache".equals(cachePackName)){
    cachePack = new MemerCache();
}
String cacheData = cachePack.getCacheData(cacheKey);*/