package com.example.producer;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ProducerService {

    private final RabbitTemplate rabbit;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProducerService(RabbitTemplate r, StringRedisTemplate rt) {
        this.rabbit = r;
        this.redis = rt;
    }

    public void send(String broker, int size) throws Exception {
        MessageDto msg = new MessageDto();
        msg.id = UUID.randomUUID().toString();
        msg.timestamp = System.currentTimeMillis();
        msg.payload = PayloadGenerator.generate(size);

        String json = mapper.writeValueAsString(msg);

        if ("rabbit".equals(broker)) {
            rabbit.convertAndSend("queue", json);
        } else {
            redis.opsForStream().add("stream", Map.of("data", json));
        }
    }
}
