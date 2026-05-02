package com.example.cachepractice.strategy;

import com.example.cachepractice.item.Item;
import com.example.cachepractice.item.ItemRepository;
import com.example.cachepractice.metrics.AppMetrics;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WriteBackFlusher {

    private final WriteBackBuffer buffer;
    private final ItemRepository repository;
    private final AppMetrics metrics;

    public WriteBackFlusher(WriteBackBuffer buffer, ItemRepository repository, AppMetrics metrics) {
        this.buffer = buffer;
        this.repository = repository;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${cache.writeback.flush-interval-ms:1000}")
    @Transactional
    public void flush() {
        WriteBackBuffer.Snapshot snapshot = buffer.snapshotAndClear();
        if (snapshot.entries().isEmpty()) {
            return;
        }

        Map<Long, Item> existingById = new HashMap<>();
        List<Item> existing = repository.findAllById(snapshot.entries().keySet());
        for (Item item : existing) {
            existingById.put(item.getId(), item);
        }

        Instant now = Instant.now();
        List<Item> toSave = new ArrayList<>(snapshot.entries().size());

        for (Map.Entry<Long, String> e : snapshot.entries().entrySet()) {
            Item item = existingById.get(e.getKey());
            if (item == null) {
                toSave.add(new Item(e.getKey(), e.getValue(), now));
            } else {
                item.updateValue(e.getValue(), now);
                toSave.add(item);
            }
        }

        repository.saveAll(toSave);
        metrics.writeBackFlush(toSave.size());
    }
}

