package com.example.cachepractice.strategy;

import com.example.cachepractice.cache.RedisJsonCache;
import com.example.cachepractice.item.ItemDbService;
import com.example.cachepractice.metrics.AppMetrics;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CacheProps.class)
public class StrategyConfiguration {

    @Bean
    public ItemCachingStrategy itemCachingStrategy(
            CacheProps props,
            ItemDbService db,
            RedisJsonCache cache,
            WriteBackBuffer writeBackBuffer,
            AppMetrics metrics
    ) {
        return switch (props.strategy()) {
            case ASIDE -> new CacheAsideStrategy(props, db, cache, metrics);
            case THROUGH -> new WriteThroughStrategy(props, db, cache, metrics);
            case BACK -> new WriteBackStrategy(props, db, cache, writeBackBuffer, metrics);
        };
    }
}

