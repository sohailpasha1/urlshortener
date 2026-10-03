package com.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "code_sequence")
public class CodeSequence {
    @Id
    private Long id;
    @Column(name = "next_value", nullable = false)
    private long nextValue;

    protected CodeSequence() {
    }

    public CodeSequence(Long id, long nextValue) {
        this.id = id;
        this.nextValue = nextValue;
    }

    public Long getId() {
        return id;
    }

    public long getNextValue() {
        return nextValue;
    }

    public void setNextValue(long v) {
        nextValue = v;
    }
}
