package com.urlshortener.service;

import com.urlshortener.domain.ShortUrl;
import com.urlshortener.repository.ShortUrlRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ShortUrlWriter {
    private final ShortUrlRepository repo;

    public ShortUrlWriter(ShortUrlRepository r) {
        repo = r;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ShortUrl save(ShortUrl s) {
        return repo.saveAndFlush(s);
    }
}
