package com.urlshortener.ratelimit;

public class TokenBucket {
    private final double capacity, refillPerMs;
    private double tokens;
    private long lastRefillMillis;

    public TokenBucket(int capacity, int refillPerMinute) {
        this.capacity = capacity;
        this.refillPerMs = refillPerMinute / 60000.0;
        this.tokens = capacity;
        this.lastRefillMillis = System.currentTimeMillis();
    }

    public synchronized boolean tryConsume() {
        long now = System.currentTimeMillis();
        long elapsed = Math.max(0, now - lastRefillMillis);
        tokens = Math.min(capacity, tokens + elapsed * refillPerMs);
        lastRefillMillis = now;
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }
}
