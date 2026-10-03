package com.urlshortener.exception;

public class ShortUrlNotFoundException extends RuntimeException {
    public ShortUrlNotFoundException(String m) {
        super(m);
    }
}
