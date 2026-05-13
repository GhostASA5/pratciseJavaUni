# Dirty read (грязное чтение) — PostgreSQL

## Важно про СУБД

В **PostgreSQL** настоящее грязное чтение **невозможно**: уровень `READ UNCOMMITTED` обрабатывается как `READ COMMITTED`, незафиксированные изменения других транзакций не видны. См. [документацию PostgreSQL — уровни изоляции](https://www.postgresql.org/docs/current/transaction-iso.html).

Ниже — сценарий «как в учебнике», чтобы показать, что второй сеанс **не** увидит незафиксированное значение. Для **классического** dirty read с `READ UNCOMMITTED` используйте скрипт под MySQL: `mysql/00_dirty_read.sql` (сервис `mysql` в `docker-compose.yml`).

## Подготовка

```bash
psql -h localhost -p 5462 -U postgres -d cache_practice -f src/main/resources/sql/isolation/00_schema_and_data.sql
```

Автопрогон с логами (нужен запущенный PostgreSQL из `docker compose up -d postgres`):

```bash
mvn -DRUN_ISOLATION_IT=true -Dtest=IsolationAnomaliesJdbcIT test
```

Откройте **два** терминала с `psql` к той же БД.

## Сеанс A (писатель)

Выполняйте по порядку:

1. `BEGIN;`
2. `UPDATE isolation_wallet SET balance = 999 WHERE id = 1;`
3. **Не делайте** `COMMIT` и **не делайте** `ROLLBACK` — оставьте транзакцию открытой.
4. Перейдите в сеанс B, выполните шаги там, затем вернитесь.
5. `ROLLBACK;` (или `COMMIT;` — для демонстрации PG достаточно `ROLLBACK`, чтобы не портить данные).

## Сеанс B (читатель)

Пока в сеансе A транзакция открыта после `UPDATE`:

1. `BEGIN;`
2. `SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;` — в PG это всё равно ведёт себя как read committed.
3. `SELECT id, balance FROM isolation_wallet WHERE id = 1;`
4. Ожидаемо увидите **100** (старое зафиксированное значение), а не **999**.
5. `COMMIT;`

## Результат для отчёта

- В сеансе B: баланс остаётся 100 до `COMMIT`/`ROLLBACK` в A.
- В отчёте: «грязное чтение в PostgreSQL не воспроизводится»

## Как избежать грязного чтения (в СУБД, где оно возможно)

Использовать не ниже `READ COMMITTED` (поведение по умолчанию в PostgreSQL и многих СУБД). Не читать из долгоживущих транзакций без необходимости; короткие транзакции и фиксация изменений.
