# spring-kafka-transactional-pitfalls

Практический репозиторий по эволюции решений в `Spring + Kafka`:

1. `bad`: dual write (`DB + Kafka`) в одном `@Transactional` методе.
2. `better`: `transactional outbox` на producer-стороне.
3. `ideal`: `outbox + idempotent consumer` для exactly-once эффекта в бизнес-операциях.

## Что внутри

- Spring Boot приложение с REST API для ручного прогона сценариев.
- Два consumer-а на одном топике:
  - `naive` (без дедупликации) - показывает дубли.
  - `idempotent` (с таблицей `processed_events`) - подавляет дубли.
- Интеграционные тесты, которые фиксируют типичные сбои.

## Быстрый старт

```bash
docker compose up -d kafka
mvn spring-boot:run
```

Проверить текущее состояние:

```bash
curl localhost:8080/api/state
```

Сбросить состояние лабораторки:

```bash
curl -X POST localhost:8080/api/state/reset
```

## Лаба 1: Плохой dual write

Цель: показать "фантомное" событие в Kafka, когда DB транзакция откатилась.

```bash
curl -i -X POST localhost:8080/api/bad/transfer \
  -H 'Content-Type: application/json' \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 100.0,
    "failAfterKafkaSend": true
  }'
```

Ожидание:

- HTTP `409` (мы эмулируем падение после отправки в Kafka).
- Балансы аккаунтов не изменились (DB rollback).
- В `naive_ledger_entries` уже есть запись (событие в Kafka всё же ушло).

Проверка:

```bash
curl localhost:8080/api/state
```

## Лаба 2: Outbox

Цель: связать изменения бизнес-данных и создание события в одной DB-транзакции.

1) Падение до commit:

```bash
curl -i -X POST localhost:8080/api/outbox/transfer \
  -H 'Content-Type: application/json' \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 50.00,
    "failAfterDbWrite": true
  }'
```

Ожидание: ни балансы, ни outbox не изменены.

2) Успешный transfer в outbox:

```bash
curl -X POST localhost:8080/api/outbox/transfer \
  -H 'Content-Type: application/json' \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 70.00,
    "failAfterDbWrite": false
  }'
```

3) Эмулируем падение relay после отправки в Kafka, но до `status=SENT`:

```bash
curl -i -X POST 'localhost:8080/api/outbox/relay?batchSize=10&failAfterFirstSend=true'
```

4) Повторный запуск relay:

```bash
curl -X POST 'localhost:8080/api/outbox/relay?batchSize=10&failAfterFirstSend=false'
```

Ожидание:

- Одно и то же событие отправилось повторно.
- `naive` consumer применит его 2+ раза.
- `idempotent` consumer применит его ровно 1 раз.

Проверка:

```bash
curl localhost:8080/api/state
```

## Почему это "идеал" для практики

- В Kafka нельзя сделать "магический" distributed transaction между DB и брокером без побочных эффектов.
- Надёжный путь для сервисов с БД:
  - producer: `transactional outbox`;
  - consumer: идемпотентная обработка (`processed_events` / unique key).
- Такой подход даёт exactly-once **эффект** на уровне бизнес-операции даже при at-least-once доставке.

## Профиль PostgreSQL

Поднять БД:

```bash
docker compose up -d postgres
```

Запуск приложения:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Тесты

```bash
mvn test
```

Покрыто:

- фантомное событие при dual write;
- atomic commit бизнес-данных + outbox;
- защита от дублей idempotent consumer-ом.
