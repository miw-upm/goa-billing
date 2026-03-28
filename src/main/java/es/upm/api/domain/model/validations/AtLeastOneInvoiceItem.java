package es.upm.api.domain.model.validations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOneInvoiceItemValidator.class)
public @interface AtLeastOneInvoiceItem {
    String message() default "Expected at least one expense or one income";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
