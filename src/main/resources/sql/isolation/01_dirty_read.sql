-- Dirty read в PostgreSQL: демонстрация ОТСУТСТВИЯ незафиксированного чтения.
-- Перед запуском: \i 00_schema_and_data.sql  (или выполните из psql -f)

-- ========== СЕАНС A (писатель) ==========
BEGIN;
UPDATE isolation_wallet SET balance = 999 WHERE id = 1;
-- STOP: не COMMIT — переключитесь в сеанс B

-- ========== СЕАНС B (читатель) ==========
BEGIN;
SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SELECT id, balance FROM isolation_wallet WHERE id = 1;
-- Ожидание: balance = 100 (не 999)
COMMIT;

-- ========== Снова сеанс A ==========
ROLLBACK;
-- После ROLLBACK баланс снова 100 из последнего COMMIT в БД; при необходимости пересоздайте seed:
-- DELETE FROM isolation_wallet; INSERT INTO isolation_wallet VALUES (1, 100);
