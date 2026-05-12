-- Non-repeatable read (неповторяемое чтение) в READ COMMITTED.
-- Два терминала psql. Сначала: \i 00_schema_and_data.sql

-- ========== СЕАНС 1 ==========
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT id, balance FROM isolation_wallet WHERE id = 1;
-- Запомните balance (100). НЕ COMMIT. Перейдите в сеанс 2.

-- ========== СЕАНС 2 ==========
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
UPDATE isolation_wallet SET balance = 200 WHERE id = 1;
COMMIT;

-- ========== Снова сеанс 1 ==========
SELECT id, balance FROM isolation_wallet WHERE id = 1;
-- Ожидание: 200 — другое значение при повторном SELECT в той же транзакции.
COMMIT;

-- Восстановление данных для следующих опытов:
-- UPDATE isolation_wallet SET balance = 100 WHERE id = 1;
