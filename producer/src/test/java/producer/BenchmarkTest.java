package producer;

import com.example.producer.BenchmarkMetrics;
import com.example.producer.BenchmarkResult;
import com.example.producer.ProducerApplication;
import com.example.producer.ProducerService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest(classes = ProducerApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BenchmarkTest {

    @Autowired
    private ProducerService producer;

    @Autowired
    private BenchmarkMetrics metrics;

    @Value("${benchmark.duration-seconds}")
    private int duration;

    @Value("${benchmark.rate}")
    private int rate;

    @Value("${benchmark.payload-size}")
    private int size;

    @Value("${benchmark.broker}")
    private String broker;

    private ExecutorService pool;

    @BeforeAll
    void setup() {
        pool = Executors.newFixedThreadPool(4);
    }

    @AfterAll
    void teardown() {
        pool.shutdown();
    }

    @Test
    void runBenchmark() throws Exception {

        long endTime = System.currentTimeMillis() + duration * 1000L;

        while (System.currentTimeMillis() < endTime) {

            CountDownLatch latch = new CountDownLatch(rate);

            for (int i = 0; i < rate; i++) {
                pool.submit(() -> {
                    try {
                        long start = System.currentTimeMillis();

                        producer.send(broker, size);
                        metrics.sent();

                        long latency = System.currentTimeMillis() - start;
                        metrics.received(latency);

                    } catch (Exception e) {
                        metrics.error();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        BenchmarkResult result = metrics.snapshot();

        print(result);
        save(result);
    }

    private void print(BenchmarkResult r) {
        System.out.println("==== RESULT ====");
        System.out.println("Sent: " + r.sent());
        System.out.println("Received: " + r.received());
        System.out.println("Errors: " + r.errors());
        System.out.println("Avg latency: " + r.avgLatency());
        System.out.println("p95 latency: " + r.p95Latency());
    }

    private void save(BenchmarkResult r) throws Exception {
        String line = String.format(
                "%d,%d,%d,%.2f,%d\n",
                r.sent(),
                r.received(),
                r.errors(),
                r.avgLatency(),
                r.p95Latency()
        );

        Files.writeString(
                Path.of(String.format("result-%s-%s-%s.csv", broker, rate, size)),
                line,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }
}