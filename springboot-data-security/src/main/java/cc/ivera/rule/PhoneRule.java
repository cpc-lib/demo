package cc.ivera.rule;

/**
 * 手机号处理方法
 *
 * @Author
 * @Description
 * @Date 2021/9/29 11:49
 */
public class PhoneRule extends BaseRule {

    /**
     * 仅显示前3位和后4位
     */
    @Override
    void initRule() {
        setRule(new RuleItem()
                .setRegex("(\\d{3})\\d*(\\d{4})")
                .setFormat("$1****$2"));
    }

}
