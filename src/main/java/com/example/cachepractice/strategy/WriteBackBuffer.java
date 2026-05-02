package com.example.cachepractice.strategy;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class WriteBackBuffer {

    private final ConcurrentHashMap<Long, String> pending = new ConcurrentHashMap<>();
    private final AtomicLong dropped = new AtomicLong();

    public void put(long id, String value, long maxSize) {
        if (pending.size() >= maxSize) {
            dropped.incrementAndGet();
            return;
        }
        pending.put(id, value);
    }

    public Snapshot snapshotAndClear() {
        if (pending.isEmpty()) {
            return new Snapshot(Map.of(), dropped.get());
        }

        Map<Long, String> copy = Map.copyOf(pending);
        pending.clear();
        return new Snapshot(copy, dropped.get());
    }

    public int size() {
        return pending.size();
    }

    public long dropped() {
        return dropped.get();
    }

    public record Snapshot(
            Map<Long, String> entries,
            long dropped
    ) {}
}

