package com.urlshortener.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "short_url", indexes = @Index(name = "ux_short_url_short_code", columnList = "short_code", unique = true))
public class ShortUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;
    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "click_count", nullable = false)
    private long clickCount;
    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;
    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected ShortUrl() {
    }

    public ShortUrl(String shortCode, String originalUrl, Instant createdAt, Instant expiresAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = 0;
        this.active = true;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getClickCount() {
        return clickCount;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
