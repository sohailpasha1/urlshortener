package com.urlshortener.cache;

import com.urlshortener.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ShortUrlCacheTest {
    private ShortUrlCache cache(int max, boolean enabled) {
        AppProperties p = new AppProperties();
        p.getCache().setMaxSize(max);
        p.getCache().setEnabled(enabled);
        return new ShortUrlCache(p);
    }

    @Test
    void storesAndGets() {
        ShortUrlCache c = cache(2, true);
        c.put("a", new CachedUrl("u", true, null));
        assertNotNull(c.get("a"));
    }

    @Test
    void evictsLru() {
        ShortUrlCache c = cache(2, true);
        c.put("a", new CachedUrl("a", true, null));
        c.put("b", new CachedUrl("b", true, null));
        c.get("a");
        c.put("c", new CachedUrl("c", true, null));
        assertNull(c.get("b"));
        assertNotNull(c.get("a"));
    }

    @Test
    void disabledDoesNotStore() {
        ShortUrlCache c = cache(2, false);
        c.put("a", new CachedUrl("u", true, null));
        assertNull(c.get("a"));
        assertEquals(0, c.size());
    }

    @Test
    void countsHitsAndMisses() {
        ShortUrlCache c = cache(2, true);
        c.put("a", new CachedUrl("u", true, Instant.now().plusSeconds(5)));
        c.get("a");
        c.get("z");
        assertEquals(1, c.hits());
        assertEquals(1, c.misses());
    }
}
