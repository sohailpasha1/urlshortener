package com.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {
    private final ShortCodeGenerator generator = new ShortCodeGenerator();

    @Test
    void generatesRequestedLength() {
        assertEquals(7, generator.generate(7).length());
    }

    @Test
    void usesOnlyBase62Characters() {
        assertTrue(generator.generate(100).matches("[0-9A-Za-z]+"));
    }

    @Test
    void rejectsZeroLength() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(0));
    }

    @Test
    void hasNoCollisionsAcrossTenThousandSamples() {
        HashSet<String> codes = new HashSet<>();
        for (int i = 0; i < 10_000; i++)
            assertTrue(codes.add(generator.generate(7)), "unexpected collision at sample " + i);
    }
}
