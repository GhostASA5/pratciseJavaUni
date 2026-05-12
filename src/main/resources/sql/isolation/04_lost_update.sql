-- Lost update (потерянное обновление): обе транзакции читают одно значение
-- и записывают результат «своей» логики, перетирая друг друга.
-- Перед опытом: \i 00_schema_and_data.sql  (баланс id=1 равен 100)

-- Корректный суммарный списание 30+30=60 при последовательном выполнении дало бы 40.
-- При lost update часто остаётся 70: обе прочитали 100 и обе записали 100-30=70.

-- ========== СЕАНС 1 ==========
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT balance FROM isolation_wallet WHERE id = 1;
-- Прочитали 100. НЕ COMMIT. Перейдите в сеанс 2.

-- ========== СЕАНС 2 ==========
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT balance FROM isolation_wallet WHERE id = 1;
-- Прочитали 100.
UPDATE isolation_wallet SET balance = 70 WHERE id = 1;
COMMIT;

-- ========== Снова сеанс 1 ==========
UPDATE isolation_wallet SET balance = 70 WHERE id = 1;
COMMIT;

-- ========== Проверка (любой сеанс) ==========
SELECT id, balance FROM isolation_wallet WHERE id = 1;
-- Ожидание: 70 вместо 40 — одно из списаний «потеряно».

-- Восстановление:
-- UPDATE isolation_wallet SET balance = 100 WHERE id = 1;
