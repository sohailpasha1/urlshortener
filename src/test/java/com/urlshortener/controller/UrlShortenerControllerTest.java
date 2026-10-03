package com.urlshortener.controller;

import com.urlshortener.domain.ShortUrl;
import com.urlshortener.exception.AliasAlreadyExistsException;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.ShortUrlGoneException;
import com.urlshortener.exception.ShortUrlNotFoundException;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UrlShortenerControllerTest {
    private MockMvc mvc;
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = mock(UrlShortenerService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new UrlShortenerController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createReturns201AndResponse() throws Exception {
        ShortUrl entity = new ShortUrl("abc123", "https://example.com/path", Instant.parse("2026-01-01T00:00:00Z"), null);
        when(service.create("https://example.com/path", "abc123", null)).thenReturn(entity);
        when(service.getBaseUrl()).thenReturn("http://localhost:8080/");

        mvc.perform(post("/api/v1/urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/path\",\"customAlias\":\"abc123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc123"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/path"))
                .andExpect(jsonPath("$.clickCount").value(0))
                .andExpect(jsonPath("$.active").value(true));

        verify(service).create("https://example.com/path", "abc123", null);
    }

    @Test
    void createPassesTtlToService() throws Exception {
        ShortUrl entity = new ShortUrl("ttl01", "https://example.com", Instant.now(), Instant.parse("2026-01-02T00:00:00Z"));
        when(service.create("https://example.com", null, 3600L)).thenReturn(entity);
        when(service.getBaseUrl()).thenReturn("http://localhost:8080");

        mvc.perform(post("/api/v1/urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"ttlSeconds\":3600}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("ttl01"))
                .andExpect(jsonPath("$.expiresAt").exists());

        verify(service).create("https://example.com", null, 3600L);
    }

    @Test
    void createRejectsInvalidRequestAndDoesNotCallService() throws Exception {
        mvc.perform(post("/api/v1/urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\",\"customAlias\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/urls/create"))
                .andExpect(jsonPath("$.fieldErrors.url").exists())
                .andExpect(jsonPath("$.fieldErrors.customAlias").exists());

        verifyNoInteractions(service);
    }

    @Test
    void getReturnsMetadata() throws Exception {
        ShortUrl entity = new ShortUrl("meta1", "https://example.com", Instant.parse("2026-01-01T00:00:00Z"), null);
        when(service.getByCode("meta1")).thenReturn(entity);
        when(service.getBaseUrl()).thenReturn("http://localhost:8080");

        mvc.perform(get("/api/v1/urls/meta1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("meta1"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/meta1"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"));

        verify(service).getByCode("meta1");
    }

    @Test
    void analyticsReturnsAnalyticsPayload() throws Exception {
        ShortUrl entity = new ShortUrl("ana01", "https://example.com", Instant.parse("2026-01-01T00:00:00Z"), null);
        when(service.getByCode("ana01")).thenReturn(entity);

        mvc.perform(get("/api/v1/urls/ana01/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("ana01"))
                .andExpect(jsonPath("$.clickCount").value(0))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.originalUrl").doesNotExist());

        verify(service).getByCode("ana01");
    }

    @Test
    void mapsNotFoundTo404() throws Exception {
        when(service.getByCode("missing")).thenThrow(new ShortUrlNotFoundException("Short URL not found: missing"));

        mvc.perform(get("/api/v1/urls/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/v1/urls/missing"));
    }

    @Test
    void mapsGoneTo410() throws Exception {
        when(service.getByCode("gone1")).thenThrow(new ShortUrlGoneException("Short URL is expired or disabled: gone1"));

        mvc.perform(get("/api/v1/urls/gone1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.error").value("Gone"));
    }

    @Test
    void mapsInvalidUrlTo400() throws Exception {
        when(service.create(anyString(), any(), any())).thenThrow(new InvalidUrlException("Only http and https URLs are supported"));

        mvc.perform(post("/api/v1/urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Only http and https URLs are supported"));
    }

    @Test
    void mapsAliasConflictTo409() throws Exception {
        when(service.create("https://example.com", "same1", null))
                .thenThrow(new AliasAlreadyExistsException("Alias already exists: same1"));

        mvc.perform(post("/api/v1/urls/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"customAlias\":\"same1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}
