package com.urlshortener.service;

public interface ShortCodeStrategy {
    String nextCode();

    String name();
}
