package com.urlshortener.ratelimit;

import com.urlshortener.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final AppProperties.Ratelimit cfg;
    private final ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties p) {
        cfg = p.getRatelimit();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest r) {
        return !("POST".equalsIgnoreCase(r.getMethod()) && r.getRequestURI().startsWith("/api/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String key = clientKey(req);
        long now = System.currentTimeMillis();
        Client c = clients.computeIfAbsent(key, k -> new Client(new TokenBucket(cfg.getCapacity(), cfg.getRefillPerMinute()), new volatileHolder(now)));
        c.last.value = now;
        if (clients.size() > cfg.getMaxClients()) {
            evictIdle(now);
            trimToMax(key);
        }
        if (!c.bucket.tryConsume()) {
            log.debug("Rate limit exceeded client={}", key);
            res.setStatus(429);
            res.setHeader("Retry-After", "1");
            res.setContentType("application/json");
            String path = escape(req.getRequestURI());
            String ts = Instant.now().toString();
            res.getWriter().write("{\"timestamp\":\"" + ts + "\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please retry later.\",\"path\":\"" + path + "\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    private String clientKey(HttpServletRequest r) {
        if (cfg.isTrustForwardedFor()) {
            String x = r.getHeader("X-Forwarded-For");
            if (x != null && !x.isBlank()) return x.split(",", 2)[0].trim();
        }
        return r.getRemoteAddr();
    }

    private void evictIdle(long now) {
        long cutoff = now - cfg.getClientIdleMinutes() * 60_000L;
        clients.entrySet().removeIf(e -> e.getValue().last.value < cutoff);
    }

    private void trimToMax(String currentKey) {
        while (clients.size() > cfg.getMaxClients()) {
            String victim = clients.keySet().stream().filter(k -> !k.equals(currentKey)).findFirst().orElse(null);
            if (victim == null) break;
            clients.remove(victim);
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    int clientCount() {
        return clients.size();
    }

    private record Client(TokenBucket bucket, volatileHolder last) {
    }

    private static final class volatileHolder {
        volatile long value;

        volatileHolder(long v) {
            value = v;
        }
    }
}
