package com.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Shortcode shortcode = new Shortcode();
    private final Ratelimit ratelimit = new Ratelimit();
    private final Cache cache = new Cache();
    private String baseUrl = "http://localhost:8080";
    private long maxTtlSeconds = 157680000L;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getMaxTtlSeconds() {
        return maxTtlSeconds;
    }

    public void setMaxTtlSeconds(long maxTtlSeconds) {
        this.maxTtlSeconds = maxTtlSeconds;
    }

    public Shortcode getShortcode() {
        return shortcode;
    }

    public Ratelimit getRatelimit() {
        return ratelimit;
    }

    public Cache getCache() {
        return cache;
    }

    public static class Shortcode {
        private int length = 7;
        private String strategy = "random";
        private int feistelBits = 40;
        private int feistelRounds = 4;
        private long feistelKey = 2685821657736338717L;
        private long sequenceBlock = 1000;

        public int getLength() {
            return length;
        }

        public void setLength(int v) {
            length = v;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String v) {
            strategy = v;
        }

        public int getFeistelBits() {
            return feistelBits;
        }

        public void setFeistelBits(int v) {
            feistelBits = v;
        }

        public int getFeistelRounds() {
            return feistelRounds;
        }

        public void setFeistelRounds(int v) {
            feistelRounds = v;
        }

        public long getFeistelKey() {
            return feistelKey;
        }

        public void setFeistelKey(long v) {
            feistelKey = v;
        }

        public long getSequenceBlock() {
            return sequenceBlock;
        }

        public void setSequenceBlock(long v) {
            sequenceBlock = v;
        }
    }

    public static class Ratelimit {
        private int capacity = 100;
        private int refillPerMinute = 100;
        private boolean trustForwardedFor = false;
        private int maxClients = 100000;
        private int clientIdleMinutes = 30;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int v) {
            capacity = v;
        }

        public int getRefillPerMinute() {
            return refillPerMinute;
        }

        public void setRefillPerMinute(int v) {
            refillPerMinute = v;
        }

        public boolean isTrustForwardedFor() {
            return trustForwardedFor;
        }

        public void setTrustForwardedFor(boolean v) {
            trustForwardedFor = v;
        }

        public int getMaxClients() {
            return maxClients;
        }

        public void setMaxClients(int v) {
            maxClients = v;
        }

        public int getClientIdleMinutes() {
            return clientIdleMinutes;
        }

        public void setClientIdleMinutes(int v) {
            clientIdleMinutes = v;
        }
    }

    public static class Cache {
        private boolean enabled = true;
        private int maxSize = 1000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean v) {
            enabled = v;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int v) {
            maxSize = v;
        }
    }
}
