package com.example.cachepractice.sqlisolation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Два независимых JDBC-соединения к PostgreSQL для воспроизведения аномалий изоляции.
 * По умолчанию не выполняется, чтобы {@code mvn test} не требовал БД.
 * Поднимите {@code docker compose up -d postgres}, затем:
 * {@code mvn -DRUN_ISOLATION_IT=true -Dtest=IsolationAnomaliesJdbcIT test}
 */
@EnabledIfSystemProperty(named = "RUN_ISOLATION_IT", matches = "true")
class IsolationAnomaliesJdbcIT {

    private static final String URL =
            System.getProperty("isolation.jdbc.url", "jdbc:postgresql://localhost:5462/cache_practice");
    private static final String USER = System.getProperty("isolation.jdbc.user", "postgres");
    private static final String PASSWORD = System.getProperty("isolation.jdbc.password", "postgres");

    private ExecutorService executor;

    @BeforeEach
    void setUp() throws Exception {
        executor = Executors.newFixedThreadPool(2);
        ensureSchemaAndSeed();
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void ensureSchemaAndSeed() throws Exception {
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
                Statement st = c.createStatement()) {
            st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS isolation_wallet (
                        id INTEGER PRIMARY KEY,
                        balance INTEGER NOT NULL CHECK (balance >= 0)
                    )""");
            st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS isolation_order (
                        id BIGSERIAL PRIMARY KEY,
                        amount INTEGER NOT NULL
                    )""");
            st.execute("TRUNCATE isolation_order RESTART IDENTITY");
            st.execute("DELETE FROM isolation_wallet");
            st.execute("INSERT INTO isolation_wallet (id, balance) VALUES (1, 100)");
        }
    }

    @Test
    void postgresqlDoesNotAllowDirtyRead() throws Exception {
        CountDownLatch updatedNotCommitted = new CountDownLatch(1);
        CountDownLatch readerDone = new CountDownLatch(1);

        Future<?> writer =
                executor.submit(
                        () -> {
                            try (Connection c2 = DriverManager.getConnection(URL, USER, PASSWORD)) {
                                c2.setAutoCommit(false);
                                exec(c2, "BEGIN");
                                exec(c2, "UPDATE isolation_wallet SET balance = 999 WHERE id = 1");
                                log("SESSION2", "UPDATE 999 без COMMIT");
                                updatedNotCommitted.countDown();
                                readerDone.await(10, TimeUnit.SECONDS);
                                exec(c2, "ROLLBACK");
                                log("SESSION2", "ROLLBACK");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

        try (Connection c1 = DriverManager.getConnection(URL, USER, PASSWORD)) {
            c1.setAutoCommit(false);
            updatedNotCommitted.await(10, TimeUnit.SECONDS);
            exec(c1, "BEGIN");
            exec(c1, "SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED");
            int balance = queryInt(c1, "SELECT balance FROM isolation_wallet WHERE id = 1");
            log("SESSION1", "SELECT balance=" + balance + " (ожидаем 100, не 999)");
            Assertions.assertEquals(100, balance);
            exec(c1, "COMMIT");
        }
        readerDone.countDown();
        writer.get(15, TimeUnit.SECONDS);
    }

    @Test
    void nonRepeatableRead() throws Exception {
        CountDownLatch firstReadDone = new CountDownLatch(1);
        CountDownLatch writerDone = new CountDownLatch(1);

        Future<?> writer =
                executor.submit(
                        () -> {
                            try (Connection c2 = DriverManager.getConnection(URL, USER, PASSWORD)) {
                                c2.setAutoCommit(false);
                                firstReadDone.await(10, TimeUnit.SECONDS);
                                exec(c2, "BEGIN");
                                exec(c2, "SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
                                exec(c2, "UPDATE isolation_wallet SET balance = 200 WHERE id = 1");
                                exec(c2, "COMMIT");
                                log("SESSION2", "UPDATE 200 COMMIT");
                                writerDone.countDown();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

        try (Connection c1 = DriverManager.getConnection(URL, USER, PASSWORD)) {
            c1.setAutoCommit(false);
            exec(c1, "BEGIN");
            exec(c1, "SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
            int first = queryInt(c1, "SELECT balance FROM isolation_wallet WHERE id = 1");
            log("SESSION1", "первый SELECT balance=" + first);
            Assertions.assertEquals(100, first);
            firstReadDone.countDown();
            writerDone.await(10, TimeUnit.SECONDS);
            int second = queryInt(c1, "SELECT balance FROM isolation_wallet WHERE id = 1");
            log("SESSION1", "второй SELECT balance=" + second);
            Assertions.assertEquals(200, second);
            exec(c1, "COMMIT");
        }
        writer.get(15, TimeUnit.SECONDS);
    }

    @Test
    void phantomRead() throws Exception {
        CountDownLatch firstSelectDone = new CountDownLatch(1);
        CountDownLatch insertDone = new CountDownLatch(1);

        Future<?> inserter =
                executor.submit(
                        () -> {
                            try (Connection c2 = DriverManager.getConnection(URL, USER, PASSWORD)) {
                                c2.setAutoCommit(false);
                                firstSelectDone.await(10, TimeUnit.SECONDS);
                                exec(c2, "BEGIN");
                                exec(c2, "SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
                                exec(c2, "INSERT INTO isolation_order (amount) VALUES (150)");
                                exec(c2, "COMMIT");
                                log("SESSION2", "INSERT amount=150 COMMIT");
                                insertDone.countDown();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

        try (Connection c1 = DriverManager.getConnection(URL, USER, PASSWORD)) {
            c1.setAutoCommit(false);
            exec(c1, "BEGIN");
            exec(c1, "SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
            int count1 = queryCount(c1, "SELECT COUNT(*) FROM isolation_order WHERE amount > 100");
            log("SESSION1", "первый COUNT (amount > 100)=" + count1);
            Assertions.assertEquals(0, count1);
            firstSelectDone.countDown();
            insertDone.await(10, TimeUnit.SECONDS);
            int count2 = queryCount(c1, "SELECT COUNT(*) FROM isolation_order WHERE amount > 100");
            log("SESSION1", "второй COUNT=" + count2);
            Assertions.assertEquals(1, count2);
            exec(c1, "COMMIT");
        }
        inserter.get(15, TimeUnit.SECONDS);
    }

    @Test
    void lostUpdate() throws Exception {
        CountDownLatch session1ReadDone = new CountDownLatch(1);
        CountDownLatch session2Done = new CountDownLatch(1);

        Future<?> session2 =
                executor.submit(
                        () -> {
                            try (Connection c2 = DriverManager.getConnection(URL, USER, PASSWORD)) {
                                c2.setAutoCommit(false);
                                session1ReadDone.await(10, TimeUnit.SECONDS);
                                exec(c2, "BEGIN");
                                exec(c2, "SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
                                int b = queryInt(c2, "SELECT balance FROM isolation_wallet WHERE id = 1");
                                log("SESSION2", "прочитал balance=" + b);
                                exec(c2, "UPDATE isolation_wallet SET balance = 70 WHERE id = 1");
                                exec(c2, "COMMIT");
                                log("SESSION2", "UPDATE 70 COMMIT");
                                session2Done.countDown();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

        try (Connection c1 = DriverManager.getConnection(URL, USER, PASSWORD)) {
            c1.setAutoCommit(false);
            exec(c1, "BEGIN");
            exec(c1, "SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
            int b = queryInt(c1, "SELECT balance FROM isolation_wallet WHERE id = 1");
            log("SESSION1", "прочитал balance=" + b);
            Assertions.assertEquals(100, b);
            session1ReadDone.countDown();
            session2Done.await(10, TimeUnit.SECONDS);
            exec(c1, "UPDATE isolation_wallet SET balance = 70 WHERE id = 1");
            exec(c1, "COMMIT");
            log("SESSION1", "UPDATE 70 COMMIT");
        }

        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD);
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT balance FROM isolation_wallet WHERE id = 1")) {
            rs.next();
            int finalBalance = rs.getInt(1);
            log("CHECK", "итоговый balance=" + finalBalance + " (ожидаем 70 — потеря одного списания)");
            Assertions.assertEquals(70, finalBalance);
        }
        session2.get(15, TimeUnit.SECONDS);
    }

    private static void exec(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    private static int queryInt(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int queryCount(Connection c, String sql) throws SQLException {
        return queryInt(c, sql);
    }

    private static void log(String session, String msg) {
        System.out.println("[" + session + "] " + msg);
    }
}
