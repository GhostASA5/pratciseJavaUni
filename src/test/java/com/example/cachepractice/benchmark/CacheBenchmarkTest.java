package com.example.cachepractice.benchmark;

import com.example.cachepractice.CacheComparisonApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class CacheBenchmarkTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    enum Mix {
        READ_HEAVY(0.80),
        BALANCED(0.50),
        WRITE_HEAVY(0.20);

        final double readRatio;

        Mix(double readRatio) {
            this.readRatio = readRatio;
        }
    }

    @Test
    void runAll() throws Exception {
        int durationSeconds = 30;
        int ratePerSecond = 200;
        int datasetSize = 10_000;

        for (String strategy : List.of("ASIDE", "THROUGH", "BACK")) {
            for (Mix mix : Mix.values()) {
                runOne(strategy, mix, durationSeconds, ratePerSecond, datasetSize);
            }
        }
    }

    private void runOne(String strategy, Mix mix, int durationSeconds, int ratePerSecond, int datasetSize) throws Exception {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(CacheComparisonApplication.class)
                .run(
                        "--server.port=0",
                        "--cache.strategy=" + strategy,
                        "--cache.ttl-seconds=300",
                        "--seed.enabled=true",
                        "--seed.items=" + datasetSize
                );

        try {
            String port = ctx.getEnvironment().getProperty("local.server.port");
            if (port == null) {
                throw new IllegalStateException("local.server.port is null");
            }

            URI base = URI.create("http://localhost:" + port);

            BenchmarkResult result = new BenchmarkRunner(base, datasetSize)
                    .run(strategy, mix, durationSeconds, ratePerSecond);

            JsonNode appMetrics = fetchJson(base.resolve("/metrics"));

            print(strategy, mix, ratePerSecond, durationSeconds, result, appMetrics);
            saveCsv(strategy, mix, ratePerSecond, durationSeconds, result, appMetrics);
        } finally {
            ctx.close();
        }
    }

    private static void print(
            String strategy,
            Mix mix,
            int ratePerSecond,
            int durationSeconds,
            BenchmarkResult r,
            JsonNode m
    ) {
        double throughput = (double) r.totalRequests / durationSeconds;
        System.out.println("==== BENCHMARK RESULT ====");
        System.out.println("Strategy: " + strategy);
        System.out.println("Mix: " + mix);
        System.out.println("Rate/sec: " + ratePerSecond);
        System.out.println("Duration/sec: " + durationSeconds);
        System.out.println("Total requests: " + r.totalRequests);
        System.out.println("Errors: " + r.errors);
        System.out.println("Throughput req/sec: " + String.format(Locale.US, "%.2f", throughput));
        System.out.println("Avg latency ms: " + String.format(Locale.US, "%.2f", r.avgLatencyMs));
        System.out.println("p95 latency ms: " + r.p95LatencyMs);
        System.out.println("App metrics: " + m.toString());
    }

    private static void saveCsv(
            String strategy,
            Mix mix,
            int ratePerSecond,
            int durationSeconds,
            BenchmarkResult r,
            JsonNode m
    ) throws IOException {
        String filename = String.format("result-%s-%s-%s.csv", strategy, mix, ratePerSecond);

        double throughput = (double) r.totalRequests / durationSeconds;

        String header = "strategy,mix,ratePerSecond,durationSeconds,totalRequests,errors,throughput,avgLatencyMs,p95LatencyMs,dbReads,dbWrites,cacheHits,cacheMisses,cacheHitRate,writeBackBufferSize,writeBackDropped,writeBackFlushes,writeBackFlushedEntries\n";
        String line = String.format(Locale.US,
                "%s,%s,%d,%d,%d,%d,%.2f,%.2f,%d,%d,%d,%d,%d,%.6f,%d,%d,%d,%d\n",
                strategy,
                mix,
                ratePerSecond,
                durationSeconds,
                r.totalRequests,
                r.errors,
                throughput,
                r.avgLatencyMs,
                r.p95LatencyMs,
                m.path("dbReads").asLong(),
                m.path("dbWrites").asLong(),
                m.path("cacheHits").asLong(),
                m.path("cacheMisses").asLong(),
                m.path("cacheHitRate").asDouble(),
                m.path("writeBackBufferSize").asLong(),
                m.path("writeBackDropped").asLong(),
                m.path("writeBackFlushes").asLong(),
                m.path("writeBackFlushedEntries").asLong()
        );

        Path out = Path.of(filename);
        if (!Files.exists(out)) {
            Files.writeString(out, header, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        Files.writeString(out, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static JsonNode fetchJson(URI uri) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(response.body());
    }

    static final class BenchmarkRunner {
        private final HttpClient client;
        private final URI base;
        private final int datasetSize;
        private final Random random;

        BenchmarkRunner(URI base, int datasetSize) {
            this.client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            this.base = base;
            this.datasetSize = datasetSize;
            this.random = new Random(42);
        }

        BenchmarkResult run(String strategy, Mix mix, int durationSeconds, int ratePerSecond) throws InterruptedException {
            ExecutorService pool = Executors.newFixedThreadPool(8);
            try {
                long endTime = System.currentTimeMillis() + durationSeconds * 1000L;
                List<Long> latencies = new CopyOnWriteArrayList<>();
                AtomicInteger errors = new AtomicInteger();
                AtomicInteger total = new AtomicInteger();

                while (System.currentTimeMillis() < endTime) {
                    CountDownLatch latch = new CountDownLatch(ratePerSecond);

                    for (int i = 0; i < ratePerSecond; i++) {
                        pool.submit(() -> {
                            long start = System.nanoTime();
                            try {
                                boolean read = random.nextDouble() < mix.readRatio;
                                long id = 1 + random.nextInt(datasetSize);

                                if (read) {
                                    getItem(id);
                                } else {
                                    putItem(id, "value-" + strategy + "-" + System.nanoTime());
                                }
                                total.incrementAndGet();
                            } catch (Exception e) {
                                errors.incrementAndGet();
                            } finally {
                                long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                                latencies.add(tookMs);
                                latch.countDown();
                            }
                        });
                    }

                    latch.await();
                }

                return BenchmarkResult.from(latencies, errors.get(), total.get());
            } finally {
                pool.shutdown();
                pool.awaitTermination(5, TimeUnit.SECONDS);
            }
        }

        private void getItem(long id) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(base.resolve("/items/" + id))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("GET failed: " + response.statusCode());
            }
        }

        private void putItem(long id, String value) throws IOException, InterruptedException {
            String body = MAPPER.createObjectNode().put("value", value).toString();
            HttpRequest request = HttpRequest.newBuilder(base.resolve("/items/" + id))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("PUT failed: " + response.statusCode());
            }
        }
    }

    static final class BenchmarkResult {
        final double avgLatencyMs;
        final long p95LatencyMs;
        final int errors;
        final int totalRequests;

        BenchmarkResult(double avgLatencyMs, long p95LatencyMs, int errors, int totalRequests) {
            this.avgLatencyMs = avgLatencyMs;
            this.p95LatencyMs = p95LatencyMs;
            this.errors = errors;
            this.totalRequests = totalRequests;
        }

        static BenchmarkResult from(List<Long> latenciesMs, int errors, int totalRequests) {
            latenciesMs.sort(Long::compareTo);

            long p95 = latenciesMs.isEmpty()
                    ? 0
                    : latenciesMs.get((int) Math.floor(latenciesMs.size() * 0.95));

            double avg = latenciesMs.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            return new BenchmarkResult(avg, p95, errors, totalRequests);
        }
    }
}

