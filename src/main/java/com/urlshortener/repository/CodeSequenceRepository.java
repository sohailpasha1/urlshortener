package com.urlshortener.repository;

import com.urlshortener.domain.CodeSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodeSequenceRepository extends JpaRepository<CodeSequence, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CodeSequence c where c.id = :id")
    Optional<CodeSequence> findByIdForUpdate(@Param("id") Long id);
}
