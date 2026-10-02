package com.urlshortener;

import com.urlshortener.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class UrlShortenerApplication {
    private static final Logger log = LoggerFactory.getLogger(UrlShortenerApplication.class);

    public static void main(String[] args) {
        log.info("Starting URL shortener application");
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
