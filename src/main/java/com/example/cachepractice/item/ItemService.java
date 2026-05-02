package com.example.cachepractice.item;

import com.example.cachepractice.strategy.ItemCachingStrategy;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

    private final ItemCachingStrategy strategy;

    public ItemService(ItemCachingStrategy strategy) {
        this.strategy = strategy;
    }

    public ItemDto get(long id) {
        return strategy.get(id);
    }

    public ItemDto put(long id, String value) {
        return strategy.put(id, value);
    }
}

