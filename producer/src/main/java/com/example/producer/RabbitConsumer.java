package com.example.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "queue")
    @SneakyThrows
    public void consume(String msg) {
        try {
            objectMapper.readValue(msg, MessageDto.class);
            BenchmarkMetrics.received.incrementAndGet();
        } catch (Exception e) {
            BenchmarkMetrics.errors.incrementAndGet();
        }
    }
}
