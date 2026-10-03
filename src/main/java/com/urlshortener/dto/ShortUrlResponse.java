package com.urlshortener.dto;

import com.urlshortener.domain.ShortUrl;

import java.time.Instant;

public record ShortUrlResponse(String shortCode, String shortUrl, String originalUrl, Instant createdAt,
                               Instant expiresAt, long clickCount, boolean active) {
    public static ShortUrlResponse from(ShortUrl s, String baseUrl) {
        String b = baseUrl != null && baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return new ShortUrlResponse(s.getShortCode(), b + "/" + s.getShortCode(), s.getOriginalUrl(), s.getCreatedAt(), s.getExpiresAt(), s.getClickCount(), s.isActive());
    }
}
