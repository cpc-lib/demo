package cc.ivera.rule;

/**
 * 密码处理方法
 *
 * @Author
 * @Description
 * @Date 2021/9/29 11:49
 */
public class PasswordRule extends BaseRule {

    /**
     * 全部隐藏
     */
    @Override
    public String apply(String str) {
        return "******";
    }

    @Override
    void initRule() {

    }
}
