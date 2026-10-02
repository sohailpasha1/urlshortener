package com.urlshortener.controller;

import com.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class RedirectController {
    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);
    private final UrlShortenerService service;

    public RedirectController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9_-]{3,16}}")
    public void redirect(@PathVariable String shortCode, HttpServletResponse response) throws IOException {
        log.info("Redirect request for shortcode {}", shortCode);
        String target = service.resolveAndRecordClick(shortCode);
        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(target);
        log.info("Redirected request {} -> {}", shortCode, target);
    }
}
