package com.urlshortener.service;

import com.urlshortener.cache.*;
import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.ShortUrl;
import com.urlshortener.exception.*;
import com.urlshortener.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UrlShortenerService {
    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);
    private final ShortUrlRepository repository;
    private final UrlValidator validator;
    private final AppProperties properties;
    private final ShortUrlCache cache;
    private final ShortCodeGenerator generator;

    public UrlShortenerService(ShortUrlRepository repository, UrlValidator validator, AppProperties properties, ShortUrlCache cache) {
        this.repository = repository;
        this.validator = validator;
        this.properties = properties;
        this.cache = cache;
        this.generator = new ShortCodeGenerator();
    }

    @Transactional
    public ShortUrl create(String url, String customAlias, Long ttlSeconds) {
        log.info("Creating short URL customAliasPresent={} ttlPresent={}", customAlias != null && !customAlias.isBlank(), ttlSeconds != null);
        String normalized = validator.validateAndNormalize(url);
        Instant now = Instant.now();
        Instant expiry = null;
        if (ttlSeconds != null) {
            if (ttlSeconds <= 0) throw new InvalidUrlException("TTL must be greater than zero");
            expiry = now.plusSeconds(ttlSeconds);
        }
        if (customAlias != null && !customAlias.isBlank()) {
            if (repository.existsByShortCode(customAlias))
                throw new AliasAlreadyExistsException("Custom alias already exists");
            return repository.save(new ShortUrl(customAlias, normalized, now, expiry));
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = generator.generate(properties.getShortcode().getLength());
            if (!repository.existsByShortCode(code))
                return repository.save(new ShortUrl(code, normalized, now, expiry));
        }
        throw new IllegalStateException("Unable to generate a unique shortcode after 5 attempts");
    }

    @Transactional
    public String resolveAndRecordClick(String shortCode) {
        Instant now = Instant.now();
        CachedUrl cached = cache.get(shortCode);
        if (cached != null) {
            log.debug("Cache hit for shortcode {}", shortCode);
            if (!cached.active() || cached.isExpired(now)) {
                cache.evict(shortCode);
                throw new ShortUrlGoneException("Short URL is inactive or expired");
            }
            if (repository.incrementClick(shortCode, now) == 0) {
                cache.evict(shortCode);
                throw new ShortUrlNotFoundException("Short URL not found");
            }
            return cached.originalUrl();
        }
        log.debug("Cache miss for shortcode {}", shortCode);
        ShortUrl entity = repository.findByShortCode(shortCode).orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found"));
        if (!entity.isActive() || entity.isExpired(now)) {
            cache.evict(shortCode);
            throw new ShortUrlGoneException("Short URL is inactive or expired");
        }
        cache.put(shortCode, CachedUrl.from(entity));
        if (repository.incrementClick(shortCode, now) == 0) {
            cache.evict(shortCode);
            throw new ShortUrlNotFoundException("Short URL not found");
        }
        return entity.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public ShortUrl getByCode(String shortCode) {
        log.debug("Reading shortcode {} directly from DB", shortCode);
        return repository.findByShortCode(shortCode).orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found"));
    }

    public String getBaseUrl() {
        return properties.getBaseUrl();
    }
}
