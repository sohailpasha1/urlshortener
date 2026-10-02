package com.urlshortener.cache;

import com.urlshortener.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ShortUrlCache {
    private static final Logger log = LoggerFactory.getLogger(ShortUrlCache.class);
    private final boolean enabled;
    private final int maxSize;
    private final Map<String, CachedUrl> cache;
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();


    @Autowired
    public ShortUrlCache(AppProperties properties) {
        this.enabled = properties.getCache().isEnabled();
        this.maxSize = properties.getCache().getMaxSize();

        this.cache = Collections.synchronizedMap(
                new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(
                            Map.Entry<String, CachedUrl> eldest) {
                        boolean evict = size() > maxSize;
                        if (evict) {
                            log.debug("LRU eviction for short code={}", eldest.getKey());
                        }
                        return evict;
                    }
                }
        );

        log.info("ShortUrlCache initialized: enabled={}, maxSize={}",
                enabled, maxSize);
    }

    public CachedUrl get(String shortCode) {
        log.trace("Cache get for shortcode {}", shortCode);
        if (!enabled) {
            missCount.incrementAndGet();
            return null;
        }
        CachedUrl value = cache.get(shortCode);
        if (value == null) missCount.incrementAndGet();
        else hitCount.incrementAndGet();
        return value;
    }

    public void put(String shortCode, CachedUrl value) {
        log.trace("Cache put for shortcode {}", shortCode);
        if (enabled) cache.put(shortCode, value);
    }

    public void evict(String shortCode) {
        log.debug("Evicting shortcode {}", shortCode);
        cache.remove(shortCode);
    }

    public int size() {
        return cache.size();
    }

    public long hitCount() {
        return hitCount.get();
    }

    public long missCount() {
        return missCount.get();
    }
}
