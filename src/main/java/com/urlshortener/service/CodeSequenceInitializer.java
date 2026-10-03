package com.urlshortener.service;

import com.urlshortener.domain.CodeSequence;
import com.urlshortener.repository.CodeSequenceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class CodeSequenceInitializer implements ApplicationRunner {
    private final CodeSequenceRepository repo;

    public CodeSequenceInitializer(CodeSequenceRepository r) {
        repo = r;
    }

    public void run(ApplicationArguments args) {
        if (repo.existsById(1L)) return;
        try {
            repo.saveAndFlush(new CodeSequence(1L, 0));
        } catch (DataIntegrityViolationException ignored) {
        }
    }
}
