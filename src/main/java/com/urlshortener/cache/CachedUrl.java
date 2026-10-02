package com.urlshortener.cache;

import com.urlshortener.domain.ShortUrl;

import java.time.Instant;

public record CachedUrl(String originalUrl, Instant expiresAt, boolean active) {
    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public static CachedUrl from(ShortUrl entity) {
        return new CachedUrl(entity.getOriginalUrl(), entity.getExpiresAt(), entity.isActive());
    }
}
