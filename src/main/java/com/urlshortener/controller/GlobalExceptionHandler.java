package com.urlshortener.controller;
import com.urlshortener.dto.ApiError; import com.urlshortener.exception.*; import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger; import org.slf4j.LoggerFactory; import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log=LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(ShortUrlNotFoundException.class) ResponseEntity<ApiError> notFound(ShortUrlNotFoundException ex,HttpServletRequest req){return clientError(404,"Not Found",ex,req);}
    @ExceptionHandler(ShortUrlGoneException.class) ResponseEntity<ApiError> gone(ShortUrlGoneException ex,HttpServletRequest req){return clientError(410,"Gone",ex,req);}
    @ExceptionHandler(AliasAlreadyExistsException.class) ResponseEntity<ApiError> conflict(AliasAlreadyExistsException ex,HttpServletRequest req){return clientError(409,"Conflict",ex,req);}
    @ExceptionHandler(InvalidUrlException.class) ResponseEntity<ApiError> badUrl(InvalidUrlException ex,HttpServletRequest req){return clientError(400,"Bad Request",ex,req);}
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex,HttpServletRequest req){
        Map<String,String> fields=new LinkedHashMap<>(); ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(),e.getDefaultMessage())); log.warn("Validation error on {}",req.getRequestURI());
        return ResponseEntity.badRequest().body(ApiError.of(400,"Bad Request","Validation failed",req.getRequestURI()).withFields(fields));
    }
    @ExceptionHandler(Exception.class) ResponseEntity<ApiError> generic(Exception ex,HttpServletRequest req){ log.error("Unhandled server error on {}",req.getRequestURI(),ex); return ResponseEntity.status(500).body(ApiError.of(500,"Internal Server Error","An unexpected error occurred",req.getRequestURI())); }
    private ResponseEntity<ApiError> clientError(int status,String error,RuntimeException ex,HttpServletRequest req){ log.warn("{} on {}: {}",status,req.getRequestURI(),ex.getMessage()); return ResponseEntity.status(status).body(ApiError.of(status,error,ex.getMessage(),req.getRequestURI())); }
}
