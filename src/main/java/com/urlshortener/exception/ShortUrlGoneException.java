package com.urlshortener.exception;

public class ShortUrlGoneException extends RuntimeException {
    public ShortUrlGoneException(String m) {
        super(m);
    }
}
