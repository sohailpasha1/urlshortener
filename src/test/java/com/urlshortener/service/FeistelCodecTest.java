package com.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeistelCodecTest {
    @Test
    void permutationIsBijective() {
        FeistelCodec c = new FeistelCodec(10, 4, 123);
        Set<Long> s = new HashSet<>();
        for (long i = 0; i < 1024; i++) assertTrue(s.add(c.permute(i)));
        assertEquals(1024, s.size());
    }

    @Test
    void encodingFixedWidthAndUnique() {
        FeistelCodec c = new FeistelCodec(12, 4, 77);
        Set<String> s = new HashSet<>();
        for (long i = 0; i < 4096; i++) {
            String x = c.encode(c.permute(i));
            assertEquals(c.width(), x.length());
            assertTrue(s.add(x));
        }
    }
}
