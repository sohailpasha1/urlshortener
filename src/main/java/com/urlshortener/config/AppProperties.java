package com.urlshortener.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public class AppProperties {
    private static final Logger log = LoggerFactory.getLogger(AppProperties.class);
    private String baseUrl;
    private final Shortcode shortcode = new Shortcode();
    private final Ratelimit ratelimit = new Ratelimit();
    private final Cache cache = new Cache();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        log.debug("Configuring base URL");
        this.baseUrl = baseUrl;
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

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }

    public static class Ratelimit {
        private int capacity = 100;
        private int refillPerMinute = 100;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillPerMinute() {
            return refillPerMinute;
        }

        public void setRefillPerMinute(int refillPerMinute) {
            this.refillPerMinute = refillPerMinute;
        }
    }

    public static class Cache {
        private boolean enabled = true;
        private int maxSize = 1000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }
}
