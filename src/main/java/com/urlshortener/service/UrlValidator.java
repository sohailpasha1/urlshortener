package com.urlshortener.service;

import com.urlshortener.exception.InvalidUrlException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class UrlValidator {
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    public String validateAndNormalize(String raw) {
        if (raw == null) throw new InvalidUrlException("URL is required");
        String url = raw.trim();
        try {
            URI u = URI.create(url);
            String scheme = u.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
                throw new InvalidUrlException("Only http and https URLs are allowed");
            String host = u.getHost();
            if (host == null || host.isBlank()) throw new InvalidUrlException("URL host is required");
            String h = host.toLowerCase(Locale.ROOT);
            if (h.equals("localhost") || h.endsWith(".localhost"))
                throw new InvalidUrlException("Localhost URLs are not allowed");
            if (host.contains(":") || IPV4.matcher(host).matches()) {
                InetAddress a = InetAddress.getByName(host);
                if (a.isLoopbackAddress() || a.isAnyLocalAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress())
                    throw new InvalidUrlException("Private or local IP addresses are not allowed");
            }
            return url;
        } catch (InvalidUrlException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidUrlException("Invalid URL");
        }
    }
}
