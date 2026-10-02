package com.urlshortener.exception;

public class ShortUrlGoneException extends RuntimeException {
    public ShortUrlGoneException(String message) {
        super(message);
    }
}
