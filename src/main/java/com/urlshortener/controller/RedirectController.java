package com.urlshortener.controller;

import com.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class RedirectController {
    private final UrlShortenerService service;

    public RedirectController(UrlShortenerService s) {
        service = s;
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9_\\-]{3,16}}")
    public void redirect(@PathVariable String shortCode, HttpServletResponse response) throws IOException {
        String target = service.resolveAndRecordClick(shortCode);
        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(target);
    }
}
