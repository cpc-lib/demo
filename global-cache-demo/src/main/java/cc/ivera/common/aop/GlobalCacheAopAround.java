package cc.ivera.common.aop;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import cc.ivera.common.annotation.Cache;
import cc.ivera.common.bean.ResponseBean;
import cc.ivera.common.cachepack.CachePack;
import cc.ivera.common.cachepack.impl.MemerCache;
import cc.ivera.common.cachepack.impl.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;

@Component
@Aspect
@Slf4j
public class GlobalCacheAopAround {
	@Value("${cache.enable}")
	private Boolean enable;
	@Value("${cache.pack}")
	private String cachePackName;
	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	//指定切入点
	@Pointcut("@annotation(cc.ivera.common.annotation.Cache) && " +
			"@annotation(org.springframework.web.bind.annotation.GetMapping)")
	public void doAspect(){}

	//环绕通知
	@Around("doAspect() && @annotation(cache)")
	public Object around(ProceedingJoinPoint joinPoint, Cache cache){
		//另一种获取注解方式
		/*MethodSignature methodSignature =(MethodSignature) joinPoint.getSignature();
		Cache cache = methodSignature.getMethod().getDeclaredAnnotation(Cache.class);*/


		//解析SpringEL获取动态参数
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		String[] expressionArr = cache.expression().split(",");
		String userName = parse(expressionArr[0], signature.getParameterNames(), joinPoint.getArgs());
		String sex = parse(expressionArr[1], signature.getParameterNames(), joinPoint.getArgs());

		//目标方法参数
		Object[] args = joinPoint.getArgs();
		//目标方法返回值
		Object object = null;
		//缓存Key
		String cacheKey = null;
		//缓存组件
		CachePack cachePack = null;
		try {
			//缓存全局开关如果为关闭状态则直接放行
			if (!enable) {
				object = joinPoint.proceed(args);
			}else{
				//构建缓存key
				//cacheKey = createCacheKey();
				cacheKey = getCacheKey(cache);

				//缓存命中
				//String cacheData = this.redisTemplate.opsForValue().get(cacheKey);
				if ("redis".equals(cachePackName)) {
					cachePack = new RedisCache(redisTemplate);
				} else if ("memecache".equals(cachePackName)) {
					cachePack = new MemerCache();
				}
				String cacheData = cachePack.getCacheData(cacheKey);

				if(ObjectUtils.isEmpty(cacheData)){
					//缓存未命中，放行
					object = joinPoint.proceed(args);
				}else{
					//缓存命中，返回数据，截断请求
					return JSON.parseObject(cacheData, ResponseBean.class);
				}
			}
			//以上代码为目标方法返回前增强
			//------------------------------------------------------------------------------------------------
			//以下代码为目标方法返回后增强
			//返回结果为空或者缓存开关为关闭状态直接返回数据
			if(null == object || !enable){
				return object;
			}
			String cacheData;
			if (object instanceof String) {
				cacheData = (String) object;
			} else {
				cacheData = JSON.toJSONString(object);
			}
			//设置缓存，缓存的时间单位是秒
			//this.redisTemplate.opsForValue().set(cacheKey, cacheData, Long.valueOf(cache.time()), TimeUnit.SECONDS);
			cachePack.writeCacheData(cacheKey,cacheData, Duration.ofSeconds(Long.valueOf(cache.time())));
		} catch (Throwable e) {
			log.error("全局缓存AOP异常：{}",e);
			e.printStackTrace();
		}
		return object;
	}

	/**
	 * 生成redis中的key，规则：SERVER_CACHE_DATA_MD5(url + param + token)
	 * @return
	 */
	public static String createCacheKey(){
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

	/*
	 * 获取缓存 key
	 * @param cache
	 * @return
	 * @throws Exception
	 */
	public static String getCacheKey(Cache cache) throws Exception {
		String cacheKey;
		if (StringUtils.isEmpty(cache.key())) {
			cacheKey = createCacheKey();
		} else {
			cacheKey = cache.key();
		}
		return cacheKey;
	}

	public String parse(String expression, String[] paraNames,Object [] paras) {
		if(StringUtils.isEmpty(expression)){
			return "";
		}
		StandardEvaluationContext context = new StandardEvaluationContext();
		for(int i=0;i<paraNames.length;i++) {
			context.setVariable(paraNames[i], paras[i]);
		}
		Expression exp = new SpelExpressionParser().parseExpression(expression);
		Object value = exp.getValue(context);
		return value == null ? "" : value.toString();
	}
}


/*
 * 获取缓存 key
 * @param cache
 * @return
 * @throws Exception
 */
/*public static String getCacheKey(Cache cache) throws Exception {
	String cacheKey;
	if(StringUtils.isEmpty(cache.key())){
		cacheKey = createCacheKey();
	}else{
		cacheKey = cache.key();
	}
	return cacheKey;
}*/

/*@Value("${cache.pack}")
private String cachePackName;*/

//构建缓存key
//cacheKey = getCacheKey(cache);

////缓存命中
/*if ("redis".equals(cachePackName)) {
cachePack = new RedisCache(redisTemplate);
} else if ("memecache".equals(cachePackName)) {
cachePack = new MemerCache();
}
String cacheData = cachePack.getCacheData(cacheKey);*/

//设置缓存，缓存的时间单位是秒
//cachePack.writeCacheData(cacheKey,cacheData, Duration.ofSeconds(Long.valueOf(cache.time())));






/*public String parse(String expression, String[] paraNames,Object [] paras) {
	if(StringUtils.isEmpty(expression)){
		return "";
	}
	StandardEvaluationContext context = new StandardEvaluationContext();
	for(int i=0;i<paraNames.length;i++) {
		context.setVariable(paraNames[i], paras[i]);
	}
	Expression exp = new SpelExpressionParser().parseExpression(expression);
	Object value = exp.getValue(context);
	return value == null ? "" : value.toString();
}*/

//解析SpringEL获取动态参数
/*MethodSignature signature = (MethodSignature) joinPoint.getSignature();
String[] expressionArr = cache.expression().split(",");
String userName = parse(expressionArr[0], signature.getParameterNames(), joinPoint.getArgs());
String sex = parse(expressionArr[1], signature.getParameterNames(), joinPoint.getArgs());*/