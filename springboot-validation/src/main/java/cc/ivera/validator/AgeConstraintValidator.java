package cc.ivera.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cc.ivera.annotation.Age;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 自定义Validator
 */
public class AgeConstraintValidator implements ConstraintValidator<Age, Integer> {
    private final static Logger LOGGER = LoggerFactory.getLogger(AgeConstraintValidator.class);

    public final static int MAXDEFAULT = 150;
    public final static int MINDEFAULT = 18;
    public final static String TIP_MESSAGE = "Age参数值最大最小范围不正确!";

    private int realMax = MAXDEFAULT;
    private int realMin = MINDEFAULT;

    @Override
    public boolean isValid(Integer realValue, ConstraintValidatorContext context) {
        LOGGER.info("---**********AgeConstraintValidator-isValid的realValue参数值{}**********---", realValue);
        return realValue == null ? false : ((realValue >= realMin && realValue <= realMax) ? true : false);
    }

    @Override
    public void initialize(Age constraintAnnotation) {
        realMax = constraintAnnotation.max();
        realMin = constraintAnnotation.min();
    }
}
