package com.example.producer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BenchmarkMetrics {

    public static final AtomicLong sent = new AtomicLong();
    public static final AtomicLong received = new AtomicLong();
    public static final AtomicLong errors = new AtomicLong();

    private final List<Long> latencies = new CopyOnWriteArrayList<>();

    public void sent() {
        sent.incrementAndGet();
    }

    public void received(long latency) {
        received.incrementAndGet();
        latencies.add(latency);
    }

    public void error() {
        errors.incrementAndGet();
    }

    public BenchmarkResult snapshot() {
        List<Long> sorted = latencies.stream().sorted().toList();

        long p95 = sorted.isEmpty() ? 0 : sorted.get((int)(sorted.size() * 0.95));

        double avg = latencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        return new BenchmarkResult(
                sent.get(),
                received.get(),
                errors.get(),
                avg,
                p95
        );
    }
}