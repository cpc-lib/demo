package cc.ivera.rule;

import lombok.Data;

import java.util.function.Function;

/**
 * @Author
 * @Description
 * @Date 2021/9/29 15:16
 */
@Data
public abstract class BaseRule implements Function<String, String> {

    /**
     * 脱敏规则对象
     */
    private RuleItem rule;

    public String apply(String str) {
        if (null == str) {
            return null;
        }
        //初始化脱敏规则
        initRule();
        if (null == rule || null == rule.getRegex() || null == rule.getFormat()) {
            return str;
        }
        //正则替换
        return str.replaceAll(rule.getRegex(), rule.getFormat());
    }

    abstract void initRule();

}
