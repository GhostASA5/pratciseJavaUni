package com.example.cachepractice.item;

import com.example.cachepractice.metrics.AppMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ItemDbService {

    private final ItemRepository repository;
    private final AppMetrics metrics;
    private final Clock clock;

    public ItemDbService(ItemRepository repository, AppMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public Item getOrThrow(long id) {
        metrics.dbRead();
        return repository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    @Transactional
    public Item upsert(long id, String value) {
        metrics.dbRead();
        metrics.dbWrite();
        Instant now = Instant.now(clock);

        return repository.findById(id)
                .map(existing -> {
                    existing.updateValue(value, now);
                    return existing;
                })
                .orElseGet(() -> repository.save(new Item(id, value, now)));
    }
}

