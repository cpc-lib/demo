package cc.ivera.annotation;

import cc.ivera.validator.AgeConstraintValidator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.*;

import javax.validation.Constraint;
import javax.validation.Payload;


/**
 * 年龄约束
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AgeConstraintValidator.class)
public @interface Age {
    int max() default AgeConstraintValidator.MAXDEFAULT;

    int min() default AgeConstraintValidator.MINDEFAULT;

    String message() default AgeConstraintValidator.TIP_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
