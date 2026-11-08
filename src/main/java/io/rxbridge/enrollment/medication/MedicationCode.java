package io.rxbridge.enrollment.medication;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MedicationCodeValidator.class)
public @interface MedicationCode {

    String message() default "must be a medication code like M123456";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}
