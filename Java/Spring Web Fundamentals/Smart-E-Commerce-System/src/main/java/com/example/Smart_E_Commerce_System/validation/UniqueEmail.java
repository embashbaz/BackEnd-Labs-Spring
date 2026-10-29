package com.example.Smart_E_Commerce_System.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Fails if a user with this email already exists (case-insensitive).
 * Use on create requests only; updates check "taken by someone else" in the service.
 */
@Documented
@Constraint(validatedBy = UniqueEmailValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmail {

    String message() default "Email is already registered";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
