package com.urlshortener.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenBucket {
    private static final Logger log = LoggerFactory.getLogger(TokenBucket.class);
    private final double capacity;
    private final double refillPerNanos;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(int capacity, int refillPerMinute) {
        if (capacity < 1 || refillPerMinute < 1)
            throw new IllegalArgumentException("capacity and refill rate must be positive");
        this.capacity = capacity;
        this.tokens = capacity;
        this.refillPerNanos = refillPerMinute / 60_000_000_000.0;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized boolean tryConsume() {
        refill();
        boolean allowed = tokens >= 1.0;
        if (allowed) tokens -= 1.0;
        log.trace("Token bucket consume allowed={}", allowed);
        return allowed;
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        tokens = Math.min(capacity, tokens + elapsed * refillPerNanos);
        lastRefillNanos = now;
    }
}
