package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {
    static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();
    private final AppProperties props;

    public ShortCodeGenerator(AppProperties props) {
        this.props = props;
    }

    public String generate() {
        StringBuilder s = new StringBuilder();
        int n = props.getShortcode().getLength();
        for (int i = 0; i < n; i++) s.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return s.toString();
    }
}
