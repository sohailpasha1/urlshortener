package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeistelCounterStrategyTest {
    @Test
    void monotonicInputsProduceUniqueCodes() {
        SequenceBlockAllocator a = mock(SequenceBlockAllocator.class);
        when(a.next()).thenReturn(0L, 1L, 2L);
        AppProperties p = new AppProperties();
        p.getShortcode().setFeistelBits(10);
        FeistelCounterStrategy s = new FeistelCounterStrategy(a, p);
        assertNotEquals(s.nextCode(), s.nextCode());
    }
}
