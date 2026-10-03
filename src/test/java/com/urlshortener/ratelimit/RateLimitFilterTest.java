package com.urlshortener.ratelimit;

import com.urlshortener.config.AppProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {
    @Test
    void onlyPostApiIsScoped() throws Exception {
        AppProperties p = new AppProperties();
        p.getRatelimit().setCapacity(1);
        p.getRatelimit().setRefillPerMinute(0);
        RateLimitFilter f = new RateLimitFilter(p);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/v1/urls/create");
        f.doFilter(get, new MockHttpServletResponse(), chain);
        f.doFilter(get, new MockHttpServletResponse(), chain);
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void repeatedPostApiGets429() throws Exception {
        AppProperties p = new AppProperties();
        p.getRatelimit().setCapacity(1);
        p.getRatelimit().setRefillPerMinute(0);
        RateLimitFilter f = new RateLimitFilter(p);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/urls/create");
        req.setRemoteAddr("1.2.3.4");
        f.doFilter(req, new MockHttpServletResponse(), chain);
        MockHttpServletResponse r2 = new MockHttpServletResponse();
        f.doFilter(req, r2, chain);
        assertEquals(429, r2.getStatus());
        assertEquals("1", r2.getHeader("Retry-After"));
    }
}
