package com.urlshortener.service;

import com.urlshortener.exception.InvalidUrlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class UrlValidator {
    private static final Logger log = LoggerFactory.getLogger(UrlValidator.class);

    public String validateAndNormalize(String raw) {
        log.debug("Validating URL");
        if (raw == null || raw.isBlank()) throw new InvalidUrlException("URL must not be blank");
        String value = raw.trim();
        final URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidUrlException("Malformed URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("Only http and https URLs are allowed");
        }
        if (uri.getHost() == null || uri.getHost().isBlank())
            throw new InvalidUrlException("URL must include a valid host");
        return uri.normalize().toString();
    }
}
