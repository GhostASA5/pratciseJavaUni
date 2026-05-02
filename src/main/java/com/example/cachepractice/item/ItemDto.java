package com.example.cachepractice.item;

import java.time.Instant;

public record ItemDto(
        long id,
        String value,
        Instant updatedAt
) {
    public static ItemDto from(Item item) {
        return new ItemDto(item.getId(), item.getValue(), item.getUpdatedAt());
    }
}

