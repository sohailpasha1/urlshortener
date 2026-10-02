package com.urlshortener.controller;

import com.urlshortener.dto.*;
import com.urlshortener.domain.ShortUrl;
import com.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlApiController {
    private static final Logger log = LoggerFactory.getLogger(UrlApiController.class);
    private final UrlShortenerService service;

    public UrlApiController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        log.info("Create URL request received");
        ShortUrl e = service.create(request.url(), request.customAlias(), request.ttlSeconds());
        return ResponseEntity.status(HttpStatus.CREATED).body(ShortUrlResponse.from(e, service.getBaseUrl()));
    }

    @GetMapping("/{shortCode}")
    public ShortUrlResponse get(@PathVariable String shortCode) {
        log.debug("Metadata request for shortcode {}", shortCode);
        return ShortUrlResponse.from(service.getByCode(shortCode), service.getBaseUrl());
    }

    @GetMapping("/{shortCode}/analytics")
    public AnalyticsResponse analytics(@PathVariable String shortCode) {
        log.debug("Analytics request for shortcode {}", shortCode);
        return AnalyticsResponse.from(service.getByCode(shortCode));
    }
}
