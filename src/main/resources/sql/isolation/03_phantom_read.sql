-- Phantom read (фантомное чтение) в READ COMMITTED.
-- Перед опытом таблица заказов пуста (после 00_schema_and_data.sql).

-- ========== СЕАНС 1 ==========
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT id, amount FROM isolation_order WHERE amount > 100;
-- Ожидание: 0 строк. НЕ COMMIT. Перейдите в сеанс 2.

-- ========== СЕАНС 2 ==========
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
INSERT INTO isolation_order (amount) VALUES (150);
COMMIT;

-- ========== Снова сеанс 1 ==========
SELECT id, amount FROM isolation_order WHERE amount > 100;
-- Ожидание: появилась новая строка (фантом) относительно первого SELECT.
COMMIT;

-- Сброс для повторного прогона:
-- TRUNCATE isolation_order RESTART IDENTITY;
