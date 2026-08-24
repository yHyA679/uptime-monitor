package com.yahya.uptime_monitor.validation;

import com.yahya.uptime_monitor.model.Website;
import com.yahya.uptime_monitor.util.WebsiteUrlSupport;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebsiteValidationTest {

    private final Validator validator = Validation
            .buildDefaultValidatorFactory()
            .getValidator();

    @Test
    void normalizesEquivalentHttpUrls() {
        assertEquals(
                "https://example.com",
                WebsiteUrlSupport.normalize(" HTTPS://Example.COM:443/ ")
        );
        assertEquals(
                "http://example.com/health?full=true",
                WebsiteUrlSupport.normalize("HTTP://EXAMPLE.COM:80/health?full=true#ignored")
        );
    }

    @Test
    void rejectsUnsupportedOrMalformedUrls() {
        assertThrows(
                IllegalArgumentException.class,
                () -> WebsiteUrlSupport.normalize("ftp://example.com")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> WebsiteUrlSupport.normalize("example.com")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> WebsiteUrlSupport.normalize("https:///missing-host")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> WebsiteUrlSupport.normalize("https://example.com/a path")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> WebsiteUrlSupport.normalize("https://user:secret@example.com")
        );
    }

    @Test
    void validatesWebsiteNameUrlAndMonitoringInterval() {
        Website website = new Website(" ", "ftp://example.com");
        website.setCheckIntervalSeconds(5);

        Set<String> invalidFields = validator.validate(website).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(invalidFields.contains("name"));
        assertTrue(invalidFields.contains("url"));
        assertTrue(invalidFields.contains("checkIntervalSeconds"));
    }
}
