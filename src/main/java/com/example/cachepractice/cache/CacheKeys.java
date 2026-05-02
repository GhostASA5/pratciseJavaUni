package com.example.cachepractice.cache;

public final class CacheKeys {
    private CacheKeys() {}

    public static String item(long id) {
        return "item:" + id;
    }
}

