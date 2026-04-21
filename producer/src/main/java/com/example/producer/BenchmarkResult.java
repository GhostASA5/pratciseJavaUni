package com.example.producer;

public record BenchmarkResult(
        long sent,
        long received,
        long errors,
        double avgLatency,
        long p95Latency
) {}