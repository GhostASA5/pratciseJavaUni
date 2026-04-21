package com.example.producer;

import java.util.concurrent.ThreadLocalRandom;

public class PayloadGenerator {
    public static byte[] generate(int size) {
        byte[] data = new byte[size];
        ThreadLocalRandom.current().nextBytes(data);
        return data;
    }
}
