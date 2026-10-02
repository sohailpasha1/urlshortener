package com.urlshortener.repository;

import com.urlshortener.domain.ShortUrl;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Modifying
    @Query("update ShortUrl s set s.clickCount = s.clickCount + 1, s.lastAccessedAt = :now where s.shortCode = :shortCode")
    int incrementClick(@Param("shortCode") String shortCode, @Param("now") Instant now);
}
