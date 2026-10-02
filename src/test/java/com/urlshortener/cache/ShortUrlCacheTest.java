package com.urlshortener.cache;

import com.urlshortener.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShortUrlCacheTest {
    private ShortUrlCache newCache(boolean enabled, int maxSize) {
        AppProperties props = new AppProperties();
        props.getCache().setEnabled(enabled);
        props.getCache().setMaxSize(maxSize);
        return new ShortUrlCache(props);
    }

    private static CachedUrl value(String suffix) {
        return new CachedUrl("https://example.com/" + suffix, null, true);
    }

    @Test
    void putGetTracksHitAndMissCounts() {
        ShortUrlCache cache = newCache(true, 2);
        cache.put("abc", value("a"));
        assertNotNull(cache.get("abc"));
        assertNull(cache.get("missing"));
        assertEquals(1, cache.hitCount());
        assertEquals(1, cache.missCount());
    }

    @Test
    void evictsLeastRecentlyUsedEntryBeyondCapacity() {
        ShortUrlCache cache = newCache(true, 2);
        cache.put("a", value("a"));
        cache.put("b", value("b"));
        cache.get("a");
        cache.put("c", value("c"));
        assertNotNull(cache.get("a"));
        assertNull(cache.get("b"));
        assertNotNull(cache.get("c"));
        assertEquals(2, cache.size());
    }

    @Test
    void evictRemovesEntry() {
        ShortUrlCache cache = newCache(true, 10);
        cache.put("a", value("a"));
        cache.evict("a");
        assertNull(cache.get("a"));
    }

}
