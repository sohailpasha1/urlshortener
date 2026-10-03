package com.urlshortener.service;

import com.urlshortener.cache.CachedUrl;
import com.urlshortener.cache.ShortUrlCache;
import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.ShortUrl;
import com.urlshortener.exception.AliasAlreadyExistsException;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.ShortUrlGoneException;
import com.urlshortener.exception.ShortUrlNotFoundException;
import com.urlshortener.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;

@Service
public class UrlShortenerService {
    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);
    private final ShortUrlRepository repo;
    private final UrlValidator validator;
    private final AppProperties props;
    private final ShortUrlCache cache;
    private final ShortCodeStrategy strategy;
    private final ShortUrlWriter writer;

    public UrlShortenerService(ShortUrlRepository repo, UrlValidator validator, AppProperties props, ShortUrlCache cache, ShortCodeStrategy strategy, ShortUrlWriter writer) {
        this.repo = repo;
        this.validator = validator;
        this.props = props;
        this.cache = cache;
        this.strategy = strategy;
        this.writer = writer;
    }

    public ShortUrl create(String rawUrl, String customAlias, Long ttlSeconds) {
        String url = validator.validateAndNormalize(rawUrl);
        Instant now = Instant.now();
        log.debug("Creating short URL aliasPresent={} ttlSeconds={}", customAlias != null && !customAlias.isBlank(), ttlSeconds);
        Instant expires = expiry(now, ttlSeconds);
        if (customAlias != null && !customAlias.isBlank()) {
            try {
                return writer.save(new ShortUrl(customAlias, url, now, expires));
            } catch (DataIntegrityViolationException e) {
                log.info("Custom alias conflict alias={}", customAlias);
                throw new AliasAlreadyExistsException("Custom alias already exists");
            }
        }
        for (int i = 0; i < 5; i++) {
            String code = strategy.nextCode();
            try {
                return writer.save(new ShortUrl(code, url, now, expires));
            } catch (DataIntegrityViolationException ignored) {
                log.debug("Short-code collision code={}", code);
            }
        }
        log.error("Unable to allocate a unique short code after five attempts");
        throw new IllegalStateException("Unable to generate a unique short code after 5 attempts");
    }

    private Instant expiry(Instant now, Long ttl) {
        if (ttl == null) return null;
        if (ttl <= 0) throw new InvalidUrlException("TTL must be greater than zero");
        if (ttl > props.getMaxTtlSeconds()) throw new InvalidUrlException("TTL exceeds maximum allowed value");
        try {
            return now.plusSeconds(ttl);
        } catch (DateTimeException | ArithmeticException e) {
            throw new InvalidUrlException("TTL produces an invalid expiry");
        }
    }

    @Transactional
    public String resolveAndRecordClick(String shortCode) {
        Instant now = Instant.now();
        CachedUrl c = cache.get(shortCode);
        if (c != null) {
            if (!c.active() || c.isExpired(now)) {
                cache.evict(shortCode);
                throw new ShortUrlGoneException("Short URL is inactive or expired");
            }
            if (repo.incrementClick(shortCode, now) == 0) {
                cache.evict(shortCode);
                throw new ShortUrlNotFoundException("Short URL not found");
            }
            return c.originalUrl();
        }
        ShortUrl s = repo.findByShortCode(shortCode).orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found"));
        if (!s.isActive() || s.isExpired(now)) throw new ShortUrlGoneException("Short URL is inactive or expired");
        cache.put(shortCode, CachedUrl.from(s));
        if (repo.incrementClick(shortCode, now) == 0) {
            cache.evict(shortCode);
            throw new ShortUrlNotFoundException("Short URL not found");
        }
        return s.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public ShortUrl getByCode(String code) {
        return repo.findByShortCode(code).orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found"));
    }

    public String getBaseUrl() {
        return props.getBaseUrl();
    }
}
