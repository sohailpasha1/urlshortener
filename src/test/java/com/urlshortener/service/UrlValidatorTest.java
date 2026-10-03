package com.urlshortener.service;

import com.urlshortener.exception.InvalidUrlException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlValidatorTest {
    private final UrlValidator v = new UrlValidator();

    @Test
    void acceptsHttp() {
        assertEquals("http://example.com/a", v.validateAndNormalize(" http://example.com/a "));
    }

    @Test
    void acceptsHttps() {
        assertEquals("https://example.com", v.validateAndNormalize("https://example.com"));
    }

    @Test
    void rejectsOtherScheme() {
        assertThrows(InvalidUrlException.class, () -> v.validateAndNormalize("ftp://example.com"));
    }

    @Test
    void rejectsMissingHost() {
        assertThrows(InvalidUrlException.class, () -> v.validateAndNormalize("https:///path"));
    }

    @Test
    void rejectsLocalhost() {
        assertThrows(InvalidUrlException.class, () -> v.validateAndNormalize("http://localhost/x"));
    }

    @Test
    void rejectsLocalhostSubdomain() {
        assertThrows(InvalidUrlException.class, () -> v.validateAndNormalize("http://a.localhost/x"));
    }

    @Test
    void rejectsPrivateIpv4() {
        assertThrows(InvalidUrlException.class, () -> v.validateAndNormalize("http://192.168.1.2/x"));
    }

    @Test
    void rejectsMetadataIp() {
        assertThrows(InvalidUrlException.class, () -> v.validateAndNormalize("http://169.254.169.254/latest"));
    }
}
