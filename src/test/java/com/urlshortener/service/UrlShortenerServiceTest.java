package com.urlshortener.service;

import com.urlshortener.cache.CachedUrl;
import com.urlshortener.cache.ShortUrlCache;
import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.ShortUrl;
import com.urlshortener.exception.AliasAlreadyExistsException;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.ShortUrlGoneException;
import com.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UrlShortenerServiceTest {
    ShortUrlRepository repo;
    UrlValidator validator;
    AppProperties props;
    ShortUrlCache cache;
    ShortCodeStrategy strategy;
    ShortUrlWriter writer;
    UrlShortenerService service;

    @BeforeEach
    void setup() {
        repo = mock(ShortUrlRepository.class);
        validator = mock(UrlValidator.class);
        props = new AppProperties();
        cache = new ShortUrlCache(props);
        strategy = mock(ShortCodeStrategy.class);
        writer = mock(ShortUrlWriter.class);
        service = new UrlShortenerService(repo, validator, props, cache, strategy, writer);
        when(validator.validateAndNormalize(anyString())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createRandom() {
        when(strategy.nextCode()).thenReturn("abc1234");
        when(writer.save(any())).thenAnswer(i -> i.getArgument(0));
        ShortUrl s = service.create("https://e.com", null, null);
        assertEquals("abc1234", s.getShortCode());
    }

    @Test
    void createAlias() {
        when(writer.save(any())).thenAnswer(i -> i.getArgument(0));
        ShortUrl s = service.create("https://e.com", "custom", null);
        assertEquals("custom", s.getShortCode());
        verifyNoInteractions(strategy);
    }

    @Test
    void aliasConflict() {
        when(writer.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
        assertThrows(AliasAlreadyExistsException.class, () -> service.create("https://e.com", "custom", null));
    }

    @Test
    void collisionRetries() {
        when(strategy.nextCode()).thenReturn("aaa", "bbb");
        when(writer.save(any())).thenThrow(new DataIntegrityViolationException("dup")).thenAnswer(i -> i.getArgument(0));
        assertEquals("bbb", service.create("https://e.com", null, null).getShortCode());
    }

    @Test
    void fiveCollisionsFail() {
        when(strategy.nextCode()).thenReturn("a");
        when(writer.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
        assertThrows(IllegalStateException.class, () -> service.create("https://e.com", null, null));
        verify(writer, times(5)).save(any());
    }

    @Test
    void positiveTtlCreatesExpiry() {
        when(strategy.nextCode()).thenReturn("abc");
        when(writer.save(any())).thenAnswer(i -> i.getArgument(0));
        ShortUrl s = service.create("https://e.com", null, 60L);
        assertNotNull(s.getExpiresAt());
    }

    @Test
    void zeroTtlInvalid() {
        assertThrows(InvalidUrlException.class, () -> service.create("https://e.com", null, 0L));
    }

    @Test
    void ttlAboveCapInvalid() {
        props.setMaxTtlSeconds(10);
        assertThrows(InvalidUrlException.class, () -> service.create("https://e.com", null, 11L));
    }

    @Test
    void resolveCacheMissLoadsAndClicks() {
        ShortUrl s = new ShortUrl("abc", "https://e.com", Instant.now(), null);
        when(repo.findByShortCode("abc")).thenReturn(Optional.of(s));
        when(repo.incrementClick(eq("abc"), any())).thenReturn(1);
        assertEquals("https://e.com", service.resolveAndRecordClick("abc"));
        assertNotNull(cache.get("abc"));
    }

    @Test
    void resolveCacheHitClicksWithoutLoad() {
        cache.put("abc", new CachedUrl("https://e.com", true, null));
        when(repo.incrementClick(eq("abc"), any())).thenReturn(1);
        assertEquals("https://e.com", service.resolveAndRecordClick("abc"));
        verify(repo, never()).findByShortCode(anyString());
    }

    @Test
    void expiredIsGone() {
        ShortUrl s = new ShortUrl("abc", "https://e.com", Instant.now().minusSeconds(10), Instant.now().minusSeconds(1));
        when(repo.findByShortCode("abc")).thenReturn(Optional.of(s));
        assertThrows(ShortUrlGoneException.class, () -> service.resolveAndRecordClick("abc"));
    }

    @Test
    void inactiveIsGone() {
        ShortUrl s = new ShortUrl("abc", "https://e.com", Instant.now(), null);
        s.setActive(false);
        when(repo.findByShortCode("abc")).thenReturn(Optional.of(s));
        assertThrows(ShortUrlGoneException.class, () -> service.resolveAndRecordClick("abc"));
    }
}
