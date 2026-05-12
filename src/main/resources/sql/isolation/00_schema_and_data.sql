-- Схема и тестовые данные для практики аномалий изоляции (PostgreSQL).
-- Запуск: psql -h localhost -p 5462 -U postgres -d cache_practice -f 00_schema_and_data.sql

CREATE TABLE IF NOT EXISTS isolation_wallet (
    id     INTEGER PRIMARY KEY,
    balance INTEGER NOT NULL CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS isolation_order (
    id     BIGSERIAL PRIMARY KEY,
    amount INTEGER NOT NULL
);

-- Воспроизводимое начальное состояние
TRUNCATE isolation_order RESTART IDENTITY;
DELETE FROM isolation_wallet;
INSERT INTO isolation_wallet (id, balance) VALUES (1, 100);
