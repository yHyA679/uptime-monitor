package com.yahya.uptime_monitor.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = WebsiteUrlValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidWebsiteUrl {

    String message() default "must be an absolute HTTP or HTTPS URL with a valid host";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
