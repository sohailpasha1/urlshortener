package com.urlshortener.dto;

import com.urlshortener.domain.ShortUrl;

import java.time.Instant;

public record AnalyticsResponse(
        String shortCode,
        long clickCount,
        Instant lastAccessedAt,
        Instant expiresAt,
        boolean active) {
    public static AnalyticsResponse from(ShortUrl s) {
        return new AnalyticsResponse(s.getShortCode(), s.getClickCount(), s.getLastAccessedAt(), s.getExpiresAt(), s.isActive());
    }
}
