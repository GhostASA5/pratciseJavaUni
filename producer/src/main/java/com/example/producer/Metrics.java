package com.example.producer;

import java.util.concurrent.atomic.AtomicLong;

public class Metrics {
    public static AtomicLong received = new AtomicLong();
    public static AtomicLong errors = new AtomicLong();

    public static void print() {
        System.out.println("Received: " + received.get());
        System.out.println("Errors: " + errors.get());
    }
}
