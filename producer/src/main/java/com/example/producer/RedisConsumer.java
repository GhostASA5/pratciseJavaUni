package com.example.producer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisConsumer {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisConsumer(StringRedisTemplate r) {
        this.redis = r;
    }

    @PostConstruct
    @SneakyThrows
    public void listen() {
        new Thread(() -> {
            while (true) {
                var msgs = redis.opsForStream()
                        .read(org.springframework.data.redis.connection.stream.StreamOffset.fromStart("stream"));
                if (msgs != null) {
                    msgs.forEach(m -> {
                        try {
                            objectMapper.readValue((JsonParser) m, MessageDto.class);
                            BenchmarkMetrics.received.incrementAndGet();
                        } catch (Exception e) {
                            BenchmarkMetrics.errors.incrementAndGet();
                        }
                    });
                }
            }
        }).start();
    }
}
