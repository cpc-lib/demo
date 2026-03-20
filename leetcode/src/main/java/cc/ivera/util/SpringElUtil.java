package cc.ivera.util;


import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpringElUtil {


    public SpringElUtil() {
    }

    private static final Logger log = LoggerFactory.getLogger(SpringElUtil.class);

    private static Map<String, Expression> expressions = new ConcurrentHashMap(16);
    /**
     * 用于SpEL表达式解析.
     */
    private static final SpelExpressionParser parser = new SpelExpressionParser();
    /**
     * 用于获取方法参数定义名字.
     */
    private static final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public static String generateKeyBySpEL(String spELString, ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = nameDiscoverer.getParameterNames(methodSignature.getMethod());
        Expression expression = parser.parseExpression(spELString);
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        return expression.getValue(context).toString();
    }


    public static String parse(String spel, Method method, Object[] args) {
        Expression expression = getExpressionByTemplate(spel);
        if (expression == null) {
            return spel;
        } else {
            LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
            String[] paraNameArr = u.getParameterNames(method);
            new SpelExpressionParser();
            StandardEvaluationContext context = new StandardEvaluationContext();

            for (int i = 0; i < paraNameArr.length; ++i) {
                context.setVariable(paraNameArr[i], args[i]);
            }

            return (String) expression.getValue(context, String.class);
        }
    }

    public static String parse(Object rootObject, String spel, Method method, Object[] args) {
        Expression expression = getExpressionByTemplate(spel);
        if (expression == null) {
            return spel;
        } else {
            LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
            String[] paraNameArr = u.getParameterNames(method);
            new SpelExpressionParser();
            StandardEvaluationContext context = new MethodBasedEvaluationContext(rootObject, method, args, u);

            for (int i = 0; i < paraNameArr.length; ++i) {
                context.setVariable(paraNameArr[i], args[i]);
            }

            return (String) expression.getValue(context, String.class);
        }
    }

    private static Expression getExpressionByTemplate(String template) {
        if (!StringUtils.isEmpty(template) && (template.contains("#") || template.contains("'"))) {
            Expression expression = (Expression) expressions.get(template);
            if (expression != null) {
                return expression;
            } else {
                ExpressionParser expressionParser = new SpelExpressionParser();
                expression = expressionParser.parseExpression(template);
                expressions.putIfAbsent(template, expression);
                return expression;
            }
        } else {
            return null;
        }
    }


    /**
     * 执行el表达式
     *
     * @param val
     * @param expression
     * @param <T>
     * @return
     */
    public static <T> T parseExpression(Map<String, Object> val, String expression) {
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(expression);
        EvaluationContext ctx = null;
        if (val != null) {
            ctx = new StandardEvaluationContext();
            //在上下文中设置变量，变量名为user，内容为user对象
            for (Map.Entry<String, Object> entry : val.entrySet()) {
                ctx.setVariable(entry.getKey(), entry.getValue());
            }
        }
        return (T) (ctx == null ? exp.getValue() : exp.getValue(ctx));
    }


    /**
     * 执行el表达式
     *
     * @param expression
     * @param <T>
     * @return
     */
    public static <T> T parseExpression(String expression) {
        return parseExpression(null, expression);
    }


    /**
     * 执行el表达式
     *
     * @param beanObj
     * @param expression
     * @param <T>
     * @return
     */
    public static <T> T parseExpression(Object beanObj, String expression) {
        Map<String, Object> ret = (beanObj instanceof Map) ? (Map<String, Object>) beanObj : BeanUtil.bean2Map(beanObj);
        return parseExpression(ret, expression);
    }


    /**
     * spring el转换
     *
     * @param expressionString  分析表达式字符串
     * @param variables         参数
     * @param desiredResultType 所需结果类型，类引用
     * @param <T>               类引用
     * @return 解析结果
     */
    public static <T> T parse(String expressionString, Map<String, Object> variables, Class<T> desiredResultType) {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(variables);
        Expression exp = parser.parseExpression(expressionString);
        return exp.getValue(context, desiredResultType);
    }


}