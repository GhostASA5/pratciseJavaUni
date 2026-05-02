package com.example.cachepractice.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class AppMetrics {

    private final AtomicLong dbReads = new AtomicLong();
    private final AtomicLong dbWrites = new AtomicLong();

    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();

    private final AtomicLong writeBackFlushes = new AtomicLong();
    private final AtomicLong writeBackFlushedEntries = new AtomicLong();

    public void dbRead() {
        dbReads.incrementAndGet();
    }

    public void dbWrite() {
        dbWrites.incrementAndGet();
    }

    public void cacheHit() {
        cacheHits.incrementAndGet();
    }

    public void cacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public void writeBackFlush(long entries) {
        writeBackFlushes.incrementAndGet();
        writeBackFlushedEntries.addAndGet(entries);
    }

    public Snapshot snapshot() {
        return new Snapshot(
                dbReads.get(),
                dbWrites.get(),
                cacheHits.get(),
                cacheMisses.get(),
                writeBackFlushes.get(),
                writeBackFlushedEntries.get()
        );
    }

    public record Snapshot(
            long dbReads,
            long dbWrites,
            long cacheHits,
            long cacheMisses,
            long writeBackFlushes,
            long writeBackFlushedEntries
    ) {}
}

