package com.example.cachepractice.strategy;

import com.example.cachepractice.cache.CacheKeys;
import com.example.cachepractice.cache.RedisJsonCache;
import com.example.cachepractice.item.Item;
import com.example.cachepractice.item.ItemDbService;
import com.example.cachepractice.item.ItemDto;
import com.example.cachepractice.metrics.AppMetrics;

import java.time.Duration;

public class WriteThroughStrategy implements ItemCachingStrategy {

    private final CacheProps props;
    private final ItemDbService db;
    private final RedisJsonCache cache;
    private final AppMetrics metrics;

    public WriteThroughStrategy(CacheProps props, ItemDbService db, RedisJsonCache cache, AppMetrics metrics) {
        this.props = props;
        this.db = db;
        this.cache = cache;
        this.metrics = metrics;
    }

    @Override
    public ItemDto get(long id) {
        String key = CacheKeys.item(id);

        return cache.get(key, ItemDto.class)
                .map(dto -> {
                    metrics.cacheHit();
                    return dto;
                })
                .orElseGet(() -> {
                    metrics.cacheMiss();
                    Item item = db.getOrThrow(id);
                    ItemDto dto = ItemDto.from(item);
                    cache.set(key, dto, Duration.ofSeconds(props.ttlSeconds()));
                    return dto;
                });
    }

    @Override
    public ItemDto put(long id, String value) {
        Item updated = db.upsert(id, value);
        ItemDto dto = ItemDto.from(updated);
        cache.set(CacheKeys.item(id), dto, Duration.ofSeconds(props.ttlSeconds()));
        return dto;
    }
}

