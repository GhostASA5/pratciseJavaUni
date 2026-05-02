package com.example.cachepractice.seed;

import com.example.cachepractice.item.Item;
import com.example.cachepractice.item.ItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    private final ItemRepository repository;

    @Value("${seed.enabled:true}")
    private boolean enabled;

    @Value("${seed.items:10000}")
    private int items;

    public DataSeeder(ItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        long existing = repository.count();
        if (existing >= items) {
            return;
        }

        repository.deleteAllInBatch();

        Instant now = Instant.now();
        List<Item> batch = new ArrayList<>(items);
        for (int i = 1; i <= items; i++) {
            batch.add(new Item((long) i, "value-" + i, now));
        }
        repository.saveAll(batch);
    }
}

