package com.example.cachepractice.metrics;

import com.example.cachepractice.strategy.CacheProps;
import com.example.cachepractice.strategy.WriteBackBuffer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final CacheProps cacheProps;
    private final AppMetrics metrics;
    private final WriteBackBuffer writeBackBuffer;

    public MetricsController(CacheProps cacheProps, AppMetrics metrics, WriteBackBuffer writeBackBuffer) {
        this.cacheProps = cacheProps;
        this.metrics = metrics;
        this.writeBackBuffer = writeBackBuffer;
    }

    @GetMapping
    public Map<String, Object> metrics() {
        AppMetrics.Snapshot s = metrics.snapshot();
        long cacheReads = s.cacheHits() + s.cacheMisses();
        double hitRate = cacheReads == 0 ? 0.0 : (double) s.cacheHits() / cacheReads;

        return Map.of(
                "cacheStrategy", cacheProps.strategy(),
                "dbReads", s.dbReads(),
                "dbWrites", s.dbWrites(),
                "cacheHits", s.cacheHits(),
                "cacheMisses", s.cacheMisses(),
                "cacheHitRate", hitRate,
                "writeBackBufferSize", writeBackBuffer.size(),
                "writeBackDropped", writeBackBuffer.dropped(),
                "writeBackFlushes", s.writeBackFlushes(),
                "writeBackFlushedEntries", s.writeBackFlushedEntries()
        );
    }
}

