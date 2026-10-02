package com.urlshortener.ratelimit;

import com.urlshortener.config.AppProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private final AppProperties properties;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().startsWith("/api/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String key = clientKey(request);
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(properties.getRatelimit().getCapacity(), properties.getRatelimit().getRefillPerMinute()));
        if (bucket.tryConsume()) {
            log.trace("Rate limit allowed POST {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }
        log.warn("Rate limit exceeded for POST {}", request.getRequestURI());
        response.setStatus(429);
        response.setContentType("application/json");
        String path = escape(request.getRequestURI());
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\",\"path\":\"" + path + "\"}");
    }

    private String clientKey(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",", 2)[0].trim();
        return request.getRemoteAddr();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
