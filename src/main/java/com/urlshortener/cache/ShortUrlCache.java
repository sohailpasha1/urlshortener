package com.urlshortener.cache;

import com.urlshortener.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ShortUrlCache {
    private final boolean enabled;
    private final int maxSize;
    private final Map<String, CachedUrl> map;
    private final AtomicLong hits = new AtomicLong(), misses = new AtomicLong();

    public ShortUrlCache(AppProperties p) {
        enabled = p.getCache().isEnabled();
        maxSize = p.getCache().getMaxSize();
        map = Collections.synchronizedMap(new LinkedHashMap<>(16, .75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, CachedUrl> e) {
                return size() > ShortUrlCache.this.maxSize;
            }
        });
    }

    public CachedUrl get(String k) {
        if (!enabled) {
            misses.incrementAndGet();
            return null;
        }
        CachedUrl v = map.get(k);
        if (v == null) misses.incrementAndGet();
        else hits.incrementAndGet();
        return v;
    }

    public void put(String k, CachedUrl v) {
        if (enabled && maxSize > 0) map.put(k, v);
    }

    public void evict(String k) {
        map.remove(k);
    }

    public int size() {
        return map.size();
    }

    public long hits() {
        return hits.get();
    }

    public long misses() {
        return misses.get();
    }
}
