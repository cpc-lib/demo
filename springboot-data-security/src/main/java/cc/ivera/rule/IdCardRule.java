package cc.ivera.rule;

/**
 * 身份证处理方法
 *
 * @Author
 * @Description
 * @Date 2021/9/29 11:49
 */
public class IdCardRule extends BaseRule {

    /**
     * 仅显示前6位和后4位
     */
    @Override
    void initRule() {
        setRule(new RuleItem()
                .setRegex("(\\d{6})\\d*(\\w{4})")
                .setFormat("$1********$2"));
    }

}
