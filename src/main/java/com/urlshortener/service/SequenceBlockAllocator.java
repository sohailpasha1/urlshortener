package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.CodeSequence;
import com.urlshortener.repository.CodeSequenceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SequenceBlockAllocator {
    private final CodeSequenceRepository repo;
    private final long blockSize;
    private long next;
    private long end;

    public SequenceBlockAllocator(CodeSequenceRepository repo, AppProperties props) {
        this.repo = repo;
        this.blockSize = props.getShortcode().getSequenceBlock();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized long next() {
        if (next >= end) {
            next = reserveBlock();
            end = Math.addExact(next, blockSize);
        }
        return next++;
    }

    public long reserveBlock() {
        CodeSequence c = repo.findByIdForUpdate(1L).orElseThrow();
        long start = c.getNextValue();
        c.setNextValue(Math.addExact(start, blockSize));
        repo.saveAndFlush(c);
        return start;
    }
}
