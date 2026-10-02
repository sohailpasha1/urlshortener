package com.urlshortener.dto;

import com.urlshortener.domain.ShortUrl;

import java.time.Instant;

public record AnalyticsResponse(String shortCode, String originalUrl, long clickCount, Instant createdAt,
                                Instant lastAccessedAt, Instant expiresAt, boolean active) {
    public static AnalyticsResponse from(ShortUrl e) {
        return new AnalyticsResponse(e.getShortCode(), e.getOriginalUrl(), e.getClickCount(), e.getCreatedAt(), e.getLastAccessedAt(), e.getExpiresAt(), e.isActive());
    }
}
