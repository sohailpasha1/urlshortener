package com.urlshortener.cache;

import com.urlshortener.domain.ShortUrl;

import java.time.Instant;

public record CachedUrl(String originalUrl, boolean active, Instant expiresAt) {
    public static CachedUrl from(ShortUrl s) {
        return new CachedUrl(s.getOriginalUrl(), s.isActive(), s.getExpiresAt());
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}
