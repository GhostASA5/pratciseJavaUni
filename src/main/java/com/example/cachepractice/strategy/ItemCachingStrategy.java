package com.example.cachepractice.strategy;

import com.example.cachepractice.item.ItemDto;

public interface ItemCachingStrategy {
    ItemDto get(long id);

    ItemDto put(long id, String value);
}

