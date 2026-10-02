package com.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;

public final class ShortCodeGenerator {
    private static final Logger log = LoggerFactory.getLogger(ShortCodeGenerator.class);
    static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        log.trace("Generating random shortcode of length {}", length);
        if (length < 1) throw new IllegalArgumentException("length must be at least 1");
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
