package com.urlshortener.service;

import com.urlshortener.UrlShortenerApplication;
import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.CodeSequence;
import com.urlshortener.repository.CodeSequenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = UrlShortenerApplication.class, properties = {"app.shortcode.sequence-block=3"})
class SequenceBlockAllocatorTest {
    @Autowired
    CodeSequenceRepository repo;
    @Autowired
    AppProperties props;
    @Autowired
    PlatformTransactionManager txm;

    @Test
    void reservedBlocksAreDurableAndNotReused() {
        repo.saveAndFlush(new CodeSequence(1L, 0));
        TransactionTemplate tx = new TransactionTemplate(txm);
        SequenceBlockAllocator a1 = new SequenceBlockAllocator(repo, props);
        long first = tx.execute(s -> a1.next());
        SequenceBlockAllocator a2 = new SequenceBlockAllocator(repo, props);
        long second = tx.execute(s -> a2.next());
        assertEquals(0, first);
        assertEquals(3, second);
    }
}
