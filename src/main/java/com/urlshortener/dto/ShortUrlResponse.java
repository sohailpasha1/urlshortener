package com.urlshortener.dto;

import com.urlshortener.domain.ShortUrl;

import java.time.Instant;

public record ShortUrlResponse(String shortCode, String shortUrl, String originalUrl, Instant createdAt,
                               Instant expiresAt, long clickCount, boolean active) {
    public static ShortUrlResponse from(ShortUrl e, String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return new ShortUrlResponse(e.getShortCode(), normalized + "/" + e.getShortCode(), e.getOriginalUrl(), e.getCreatedAt(), e.getExpiresAt(), e.getClickCount(), e.isActive());
    }
}
