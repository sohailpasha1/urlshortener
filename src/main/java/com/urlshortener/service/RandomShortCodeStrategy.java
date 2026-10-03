package com.urlshortener.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.shortcode", name = "strategy", havingValue = "random", matchIfMissing = true)
public class RandomShortCodeStrategy implements ShortCodeStrategy {
    private final ShortCodeGenerator generator;

    public RandomShortCodeStrategy(ShortCodeGenerator g) {
        generator = g;
    }

    public String nextCode() {
        return generator.generate();
    }

    public String name() {
        return "random";
    }
}
