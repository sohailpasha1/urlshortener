package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.shortcode", name = "strategy", havingValue = "feistel")
public class FeistelCounterStrategy implements ShortCodeStrategy {
    private final SequenceBlockAllocator allocator;
    private final FeistelCodec codec;
    private final long limit;

    public FeistelCounterStrategy(SequenceBlockAllocator a, AppProperties p) {
        allocator = a;
        int bits = p.getShortcode().getFeistelBits();
        codec = new FeistelCodec(bits, p.getShortcode().getFeistelRounds(), p.getShortcode().getFeistelKey());
        limit = 1L << bits;
    }

    public String nextCode() {
        long n = allocator.next();
        if (n >= limit) throw new IllegalStateException("Feistel counter exhausted");
        return codec.encode(codec.permute(n));
    }

    public String name() {
        return "feistel";
    }
}
