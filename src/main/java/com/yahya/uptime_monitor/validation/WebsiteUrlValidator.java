package com.yahya.uptime_monitor.validation;

import com.yahya.uptime_monitor.util.WebsiteUrlSupport;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class WebsiteUrlValidator implements ConstraintValidator<ValidWebsiteUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        try {
            WebsiteUrlSupport.normalize(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
