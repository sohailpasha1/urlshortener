package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortCodeGeneratorTest {
    private AppProperties props(int n) {
        AppProperties p = new AppProperties();
        p.getShortcode().setLength(n);
        return p;
    }

    @Test
    void length() {
        assertEquals(7, new ShortCodeGenerator(props(7)).generate().length());
    }

    @Test
    void alphabet() {
        assertTrue(new ShortCodeGenerator(props(10)).generate().matches("[0-9A-Za-z]{10}"));
    }

    @Test
    void configuredLength() {
        assertEquals(3, new ShortCodeGenerator(props(3)).generate().length());
    }

    @Test
    void oneCharacterLength() {
        assertEquals(1, new ShortCodeGenerator(props(1)).generate().length());
    }
}
