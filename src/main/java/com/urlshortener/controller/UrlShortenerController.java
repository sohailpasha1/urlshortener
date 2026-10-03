package com.urlshortener.controller;

import com.urlshortener.domain.ShortUrl;
import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.CreateShortUrlRequest;
import com.urlshortener.dto.ShortUrlResponse;
import com.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlShortenerController {
    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService s) {
        service = s;
    }

    @PostMapping("/create")
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest req) {
        ShortUrl s = service.create(req.url(), req.customAlias(), req.ttlSeconds());
        return ResponseEntity.status(HttpStatus.CREATED).body(ShortUrlResponse.from(s, service.getBaseUrl()));
    }

    @GetMapping("/{shortCode}")
    public ShortUrlResponse get(@PathVariable String shortCode) {
        return ShortUrlResponse.from(service.getByCode(shortCode), service.getBaseUrl());
    }

    @GetMapping("/{shortCode}/analytics")
    public AnalyticsResponse analytics(@PathVariable String shortCode) {
        return AnalyticsResponse.from(service.getByCode(shortCode));
    }
}
