# Отчёт: практика аномалий изоляции SQL

**Дата прогона автотестов:** 2026-05-12 (локально: `docker compose up -d postgres`, затем `mvn -DRUN_ISOLATION_IT=true -Dtest=IsolationAnomaliesJdbcIT test`, все 4 теста — успешно).

## Выбранные аномалии

| Аномалия | СУБД / примечание |
|----------|-------------------|
| Dirty read (грязное чтение) | PostgreSQL — демонстрация **отсутствия** грязного чтения; MySQL — классическое грязное чтение при `READ UNCOMMITTED` |
| Non-repeatable read (неповторяемое чтение) | PostgreSQL, `READ COMMITTED` |
| Phantom read (фантомное чтение) | PostgreSQL, `READ COMMITTED` |
| Lost update (потерянное обновление) | PostgreSQL, `READ COMMITTED` |

## Подготовка окружения

1. Поднять PostgreSQL: из корня репозитория выполнить `docker compose up -d postgres`.
2. Применить схему и данные:

   ```bash
   psql -h localhost -p 5462 -U postgres -d cache_practice -f src/main/resources/sql/isolation/00_schema_and_data.sql
   ```

3. (Опционально для dirty read в MySQL) Поднять MySQL: `docker compose up -d mysql`, затем скрипт из `src/main/resources/sql/isolation/mysql/00_dirty_read.sql` — см. раздел ниже.

---

## 1. Dirty read

### Шаги воспроизведения (PostgreSQL)

Следуйте файлам [01_dirty_read.md](src/main/resources/sql/isolation/01_dirty_read.md) и [01_dirty_read.sql](src/main/resources/sql/isolation/01_dirty_read.sql): два сеанса `psql`, в сеансе A — `UPDATE` без `COMMIT`, в сеансе B — `SELECT` при `READ UNCOMMITTED`.

### Полученный результат


**Прогон JDBC-теста `postgresqlDoesNotAllowDirtyRead` (лог консоли):**

```text
[SESSION2] UPDATE 999 без COMMIT
[SESSION1] SELECT balance=100 (ожидаем 100, не 999)
[SESSION2] ROLLBACK
```

Вывод: при незафиксированном изменении во втором соединении первое соединение по-прежнему читает зафиксированное значение **100** — грязного чтения в PostgreSQL нет.

### Как избежать

В PostgreSQL грязное чтение и так исключено на уровне движка. В СУБД, где доступен `READ UNCOMMITTED`, не использовать его для бизнес-логики; держать уровень не ниже `READ COMMITTED`.


---

## 2. Non-repeatable read

### Шаги воспроизведения

Файл [02_non_repeatable_read.sql](src/main/resources/sql/isolation/02_non_repeatable_read.sql): сеанс 1 — `BEGIN`, два `SELECT` одной строки с паузой; между ними сеанс 2 — `UPDATE` и `COMMIT`.

### Полученный результат

Ожидаем 100, потом 200

**Прогон JDBC-теста `nonRepeatableRead`:**

```text
[SESSION1] первый SELECT balance=100
[SESSION2] UPDATE 200 COMMIT
[SESSION1] второй SELECT balance=200
```

В одной транзакции `READ COMMITTED` повторное чтение той же строки после коммита другой транзакции даёт новое значение — неповторяемое чтение.

### Как избежать

Повысить уровень изоляции до `REPEATABLE READ` или `SERIALIZABLE` (с учётом особенностей СУБД); при необходимости — `SELECT ... FOR SHARE` / `FOR UPDATE` для стабильного чтения строки в рамках транзакции.

---

## 3. Phantom read

### Шаги воспроизведения

Файл [03_phantom_read.sql](src/main/resources/sql/isolation/03_phantom_read.sql): сеанс 1 — два одинаковых `SELECT` по предикату `amount > 100`; между ними сеанс 2 — `INSERT` строки, попадающей под предикат.

### Полученный результат

После первого запрос 0 строк, после второго появилось значение

**Прогон JDBC-теста `phantomRead`:**

```text
[SESSION1] первый COUNT (amount > 100)=0
[SESSION2] INSERT amount=150 COMMIT
[SESSION1] второй COUNT=1
```

В той же транзакции `READ COMMITTED` повторный подсчёт по предикату видит вставленную другой транзакцией строку — фантом.

### Как избежать

`SERIALIZABLE` / стратегии сериализации в конкретной СУБД; согласованные блокировки диапазона там, где поддерживается; денормализация или материализованные снимки для отчётов без конкурентных вставок в тот же предикат.

---

## 4. Lost update

### Шаги воспроизведения

Файл [04_lost_update.sql](src/main/resources/sql/isolation/04_lost_update.sql): оба сеанса читают баланс и выполняют `UPDATE` до «прочитанное минус 30» без учёта параллельного изменения.

### Полученный результат

`SELECT` — баланс **70** вместо ожидаемых **40** после двух списаний по 30.

**Прогон JDBC-теста `lostUpdate`:**

```text
[SESSION1] прочитал balance=100
[SESSION2] прочитал balance=100
[SESSION2] UPDATE 70 COMMIT
[SESSION1] UPDATE 70 COMMIT
[CHECK] итоговый balance=70 (ожидаем 70 — потеря одного списания)
```

Обе транзакции опирались на прочитанное **100** и записали **70**; одно списание потеряно.

### Как избежать

Атомарное выражение `UPDATE isolation_wallet SET balance = balance - 30 WHERE id = 1`; блокировка строки `SELECT ... FOR UPDATE`; оптимистичные блокировки по версии (`WHERE id = 1 AND version = ?`).

---

## Автоматизация (логи для отчёта)

Интеграционный тест с двумя JDBC-соединениями: [IsolationAnomaliesJdbcIT.java](src/test/java/com/example/cachepractice/sqlisolation/IsolationAnomaliesJdbcIT.java). Тесты выполняются только при явном флаге JVM (чтобы обычный `mvn test` не требовал PostgreSQL для этого класса):

```bash
docker compose up -d postgres
mvn -DRUN_ISOLATION_IT=true -Dtest=IsolationAnomaliesJdbcIT test
```

Полный вывод консоли Surefire (объединённый лог всех четырёх сценариев, порядок строк может отличаться от порядка тестов):

```text
[SESSION1] прочитал balance=100
[SESSION2] прочитал balance=100
[SESSION2] UPDATE 70 COMMIT
[SESSION1] UPDATE 70 COMMIT
[CHECK] итоговый balance=70 (ожидаем 70 — потеря одного списания)
[SESSION1] первый SELECT balance=100
[SESSION2] UPDATE 200 COMMIT
[SESSION1] второй SELECT balance=200
[SESSION2] UPDATE 999 без COMMIT
[SESSION1] SELECT balance=100 (ожидаем 100, не 999)
[SESSION2] ROLLBACK
[SESSION1] первый COUNT (amount > 100)=0
[SESSION2] INSERT amount=150 COMMIT
[SESSION1] второй COUNT=1
```

При необходимости для зачёта добавьте скриншот окна терминала с этим выводом или скриншот отчёта Surefire (`target/surefire-reports/`).
