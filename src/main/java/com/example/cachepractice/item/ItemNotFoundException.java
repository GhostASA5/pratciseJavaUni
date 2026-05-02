package com.example.cachepractice.item;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(long id) {
        super("Item not found: " + id);
    }
}

