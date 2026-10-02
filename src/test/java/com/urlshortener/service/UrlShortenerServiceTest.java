package com.urlshortener.service;

import com.urlshortener.cache.ShortUrlCache;
import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.ShortUrl;
import com.urlshortener.exception.AliasAlreadyExistsException;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.ShortUrlGoneException;
import com.urlshortener.exception.ShortUrlNotFoundException;
import com.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UrlShortenerServiceTest {
    private ShortUrlCache newCache(boolean enabled, int maxSize) {
        AppProperties props = new AppProperties();
        props.getCache().setEnabled(enabled);
        props.getCache().setMaxSize(maxSize);
        return new ShortUrlCache(props);
    }

    ShortUrlRepository repository;
    AppProperties properties;
    ShortUrlCache cache;
    UrlShortenerService service;

    @BeforeEach
    void setUp() {
        repository = mock(ShortUrlRepository.class);
        properties = new AppProperties();
        properties.setBaseUrl("http://localhost:8080");
        properties.getShortcode().setLength(7);
        cache = newCache(true, 100);
        service = new UrlShortenerService(repository, new UrlValidator(), properties, cache);
        when(repository.save(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createsGeneratedCode() {
        when(repository.existsByShortCode(anyString())).thenReturn(false);
        ShortUrl result = service.create("https://example.com", null, null);
        assertEquals(7, result.getShortCode().length());
        assertEquals("https://example.com", result.getOriginalUrl());
        verify(repository).save(any());
    }

    @Test
    void createsCustomAlias() {
        when(repository.existsByShortCode("my_alias")).thenReturn(false);
        ShortUrl result = service.create("https://example.com", "my_alias", null);
        assertEquals("my_alias", result.getShortCode());
    }

    @Test
    void rejectsDuplicateAlias() {
        when(repository.existsByShortCode("duplicate")).thenReturn(true);
        assertThrows(AliasAlreadyExistsException.class, () -> service.create("https://example.com", "duplicate", null));
        verify(repository, never()).save(any());
    }

    @Test
    void appliesTtl() {
        ShortUrl result = service.create("https://example.com", "ttlcode", 60L);
        assertNotNull(result.getExpiresAt());
        assertTrue(result.getExpiresAt().isAfter(result.getCreatedAt()));
    }

    @Test
    void rejectsNonPositiveTtl() {
        assertThrows(InvalidUrlException.class, () -> service.create("https://example.com", "ttlcode", 0L));
    }

    @Test
    void resolveRecordsClick() {
        ShortUrl entity = new ShortUrl("abc", "https://example.com", Instant.now(), null);
        when(repository.findByShortCode("abc")).thenReturn(Optional.of(entity));
        when(repository.incrementClick(eq("abc"), any())).thenReturn(1);
        assertEquals("https://example.com", service.resolveAndRecordClick("abc"));
        verify(repository).incrementClick(eq("abc"), any());
    }

    @Test
    void resolveNotFoundThrows() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());
        assertThrows(ShortUrlNotFoundException.class, () -> service.resolveAndRecordClick("missing"));
    }

    @Test
    void expiredLinkIsGone() {
        ShortUrl entity = new ShortUrl("old", "https://example.com", Instant.now().minusSeconds(20), Instant.now().minusSeconds(1));
        when(repository.findByShortCode("old")).thenReturn(Optional.of(entity));
        assertThrows(ShortUrlGoneException.class, () -> service.resolveAndRecordClick("old"));
        verify(repository, never()).incrementClick(anyString(), any());
    }

    @Test
    void disabledLinkIsGone() {
        ShortUrl entity = new ShortUrl("off", "https://example.com", Instant.now(), null);
        entity.setActive(false);
        when(repository.findByShortCode("off")).thenReturn(Optional.of(entity));
        assertThrows(ShortUrlGoneException.class, () -> service.resolveAndRecordClick("off"));
    }

    @Test
    void secondResolveServesFromCacheAndSkipsDbRead() {
        ShortUrl entity = new ShortUrl("hot", "https://example.com/hot", Instant.now(), null);
        when(repository.findByShortCode("hot")).thenReturn(Optional.of(entity));
        when(repository.incrementClick(eq("hot"), any())).thenReturn(1);
        assertEquals("https://example.com/hot", service.resolveAndRecordClick("hot"));
        assertEquals("https://example.com/hot", service.resolveAndRecordClick("hot"));
        verify(repository, times(1)).findByShortCode("hot");
        verify(repository, times(2)).incrementClick(eq("hot"), any());
    }
}
