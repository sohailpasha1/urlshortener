package com.urlshortener.controller;

import com.urlshortener.exception.ShortUrlGoneException;
import com.urlshortener.exception.ShortUrlNotFoundException;
import com.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RedirectControllerTest {
    private MockMvc mvc;
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = mock(UrlShortenerService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RedirectController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void redirectReturns302LocationAndNoStore() throws Exception {
        when(service.resolveAndRecordClick("abc123")).thenReturn("https://example.com/target");

        mvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/target"))
                .andExpect(header().string("Cache-Control", "no-store"));

        verify(service).resolveAndRecordClick("abc123");
    }

    @Test
    void redirectMapsMissingCodeTo404() throws Exception {
        when(service.resolveAndRecordClick("missing"))
                .thenThrow(new ShortUrlNotFoundException("Short URL not found: missing"));

        mvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/missing"));
    }

    @Test
    void redirectMapsExpiredCodeTo410() throws Exception {
        when(service.resolveAndRecordClick("gone1"))
                .thenThrow(new ShortUrlGoneException("Short URL is expired or disabled: gone1"));

        mvc.perform(get("/gone1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.error").value("Gone"));
    }

    @Test
    void redirectControllerReceivesOnlyValidShapeInStandaloneTest() throws Exception {
        when(service.resolveAndRecordClick("ab1")).thenReturn("https://example.com/target");

        mvc.perform(get("/ab1"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/target"));

        verify(service).resolveAndRecordClick("ab1");
    }
}
