package com.urlshortener.service;

import com.urlshortener.exception.InvalidUrlException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlValidatorTest {
    private final UrlValidator validator = new UrlValidator();

    @Test
    void acceptsHttpAndHttps() {
        assertEquals("http://example.com", validator.validateAndNormalize("http://example.com"));
        assertEquals("https://example.com/path", validator.validateAndNormalize("https://example.com/path"));
    }

    @Test
    void trimsInput() {
        assertEquals("https://example.com", validator.validateAndNormalize("  https://example.com  "));
    }

    @Test
    void rejectsBlank() {
        assertThrows(InvalidUrlException.class, () -> validator.validateAndNormalize("  "));
    }

    @Test
    void rejectsDisallowedScheme() {
        assertThrows(InvalidUrlException.class, () -> validator.validateAndNormalize("javascript:alert(1)"));
        assertThrows(InvalidUrlException.class, () -> validator.validateAndNormalize("data:text/plain,hello"));
    }

    @Test
    void rejectsMissingHost() {
        assertThrows(InvalidUrlException.class, () -> validator.validateAndNormalize("https:///path"));
    }

    @Test
    void rejectsMalformed() {
        assertThrows(InvalidUrlException.class, () -> validator.validateAndNormalize("http://exa mple.com"));
    }
}
