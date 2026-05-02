package com.example.cachepractice.strategy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache")
public record CacheProps(
        CacheStrategyType strategy,
        long ttlSeconds,
        WriteBackProps writeback
) {
    public record WriteBackProps(
            long flushIntervalMs,
            long maxBufferSize
    ) {}
}

