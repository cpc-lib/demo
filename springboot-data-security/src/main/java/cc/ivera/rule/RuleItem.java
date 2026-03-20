package cc.ivera.rule;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author
 * @Description
 * @Date 2021/9/29 15:18
 */
@Data
@Accessors(chain = true)
public class RuleItem {

    /**
     * 正则
     */
    private String regex;

    /**
     * 格式化显示
     */
    private String format;
}
