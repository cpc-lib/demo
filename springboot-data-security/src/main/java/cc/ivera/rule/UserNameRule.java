package cc.ivera.rule;

/**
 * 姓名处理方法
 *
 * @Author
 * @Description
 * @Date 2021/9/29 11:49
 */
public class UserNameRule extends BaseRule {

    /**
     * 仅显示最后一个汉字
     */
    @Override
    void initRule() {
        setRule(new RuleItem()
                .setRegex("\\S*(\\S)")
                .setFormat("**$1"));
    }
}
