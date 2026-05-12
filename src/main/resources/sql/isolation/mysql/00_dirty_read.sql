-- Классическое грязное чтение (READ UNCOMMITTED) в MySQL / InnoDB.
-- Подготовка: docker compose up -d mysql
-- Клиент 1: mysql -h 127.0.0.1 -P 3307 -u root -proot isolation_practice
-- Клиент 2: второе окно с теми же параметрами.

CREATE TABLE IF NOT EXISTS isolation_wallet (
    id INT NOT NULL PRIMARY KEY,
    balance INT NOT NULL
) ENGINE=InnoDB;

INSERT INTO isolation_wallet (id, balance) VALUES (1, 100)
    ON DUPLICATE KEY UPDATE balance = 100;

-- ========== СЕАНС A (писатель) ==========
START TRANSACTION;
UPDATE isolation_wallet SET balance = 999 WHERE id = 1;
-- НЕ COMMIT. Переключитесь в сеанс B.

-- ========== СЕАНС B (читатель, грязное чтение) ==========
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SELECT id, balance FROM isolation_wallet WHERE id = 1;
-- Часто увидите 999 (незафиксированное значение) — это dirty read.
COMMIT;

-- ========== Снова сеанс A ==========
ROLLBACK;
-- После отката в B повторный SELECT при RC снова покажет 100.
