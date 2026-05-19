package com.ipscentir.appointments.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ColombiaCedulaValidator.class)
public @interface ValidColombiaCedula {
    String message() default "La cédula debe ser un número válido de Colombia (8-10 dígitos)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
