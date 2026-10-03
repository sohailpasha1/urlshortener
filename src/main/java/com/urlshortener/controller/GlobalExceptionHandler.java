package com.urlshortener.controller;

import com.urlshortener.dto.ApiError;
import com.urlshortener.exception.AliasAlreadyExistsException;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.ShortUrlGoneException;
import com.urlshortener.exception.ShortUrlNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ShortUrlNotFoundException.class)
    ResponseEntity<ApiError> notFound(ShortUrlNotFoundException e, HttpServletRequest r) {
        return body(404, "Not Found", e.getMessage(), r);
    }

    @ExceptionHandler(ShortUrlGoneException.class)
    ResponseEntity<ApiError> gone(ShortUrlGoneException e, HttpServletRequest r) {
        return body(410, "Gone", e.getMessage(), r);
    }

    @ExceptionHandler(AliasAlreadyExistsException.class)
    ResponseEntity<ApiError> conflict(AliasAlreadyExistsException e, HttpServletRequest r) {
        return body(409, "Conflict", e.getMessage(), r);
    }

    @ExceptionHandler(InvalidUrlException.class)
    ResponseEntity<ApiError> invalid(InvalidUrlException e, HttpServletRequest r) {
        return body(400, "Bad Request", e.getMessage(), r);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiError.withFields(400, "Bad Request", "Validation failed", r.getRequestURI(), fields));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> other(Exception e, HttpServletRequest r) {
        log.error("Unhandled request failure path={}", r.getRequestURI(), e);
        return body(500, "Internal Server Error", "An unexpected error occurred", r);
    }

    private ResponseEntity<ApiError> body(int status, String error, String message, HttpServletRequest r) {
        return ResponseEntity.status(status).body(ApiError.of(status, error, message, r.getRequestURI()));
    }
}
