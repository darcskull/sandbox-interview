# Relational SQL interview cheat sheet

Use examples as portable SQL unless a database is named; PostgreSQL-specific examples are labeled. A schema is an executable contract for shape, constraints, and relationships.

## Table design and constraints

```sql
CREATE TABLE customer (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email        VARCHAR(320) NOT NULL UNIQUE,
  display_name VARCHAR(120) NOT NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_order (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  customer_id BIGINT NOT NULL REFERENCES customer(id),
  status      VARCHAR(24) NOT NULL CHECK (status IN ('NEW', 'PAID', 'CANCELLED')),
  total       DECIMAL(12, 2) NOT NULL CHECK (total >= 0)
);
```

- Primary key: stable row identity, unique and non-null. Natural keys (email, external ID) may change or have awkward length; a surrogate key does not remove the need for a uniqueness constraint on the natural key.
- Foreign key: enforces referential integrity. Decide deliberately whether deletion should be restricted, cascaded, or set null.
- `NOT NULL`, `UNIQUE`, and `CHECK` enforce invariants for every writer, not only one service.
- Use exact decimal types for money; floating point is approximate. Store currency separately where multiple currencies are possible.
- Normalize to avoid update anomalies and duplicated facts. Denormalize only for a measured access pattern, and define how the copies stay consistent.

## Query patterns

```sql
SELECT c.id, c.email, COUNT(o.id) AS order_count
FROM customer c
LEFT JOIN purchase_order o ON o.customer_id = c.id
WHERE c.created_at >= :since
GROUP BY c.id, c.email
HAVING COUNT(o.id) >= :minimum_orders
ORDER BY order_count DESC, c.id
LIMIT :page_size;

WITH monthly_sales AS (
  SELECT date_trunc('month', created_at) AS month, SUM(total) AS total
  FROM purchase_order
  WHERE status = 'PAID'
  GROUP BY 1
)
SELECT month, total, SUM(total) OVER (ORDER BY month) AS running_total
FROM monthly_sales;
```

- `INNER JOIN` keeps matching rows; `LEFT JOIN` keeps every left row and fills missing right columns with `NULL`. A right-side condition in `WHERE` can accidentally turn a left join into an inner join; put optional-side filtering in `ON` when appropriate.
- `WHERE` filters rows before grouping; `HAVING` filters groups after aggregation.
- A CTE (`WITH`) names an intermediate query. A window function computes across related rows without collapsing them into one grouped row.
- Bound parameters prevent injection and improve plan reuse. Do not concatenate untrusted values into SQL; identifiers generally cannot be supplied as value parameters, so allowlist dynamic column names.
- Prefer keyset pagination for deep, changing result sets: `WHERE (created_at, id) < (:cursor_time, :cursor_id) ORDER BY created_at DESC, id DESC LIMIT :n`. Offset pagination can scan/skip many rows and shift under concurrent writes.

## Transactions, isolation, and locking

```sql
BEGIN;
UPDATE account SET balance = balance - 25 WHERE id = :from_id;
UPDATE account SET balance = balance + 25 WHERE id = :to_id;
COMMIT;
```

A transaction groups changes atomically. ACID means atomicity, consistency, isolation, durability. Isolation levels trade anomalies against concurrency; exact behavior varies by engine. Common anomalies include dirty reads, non-repeatable reads, and phantoms. Serializable isolation may abort transactions that must be retried. Optimistic locking uses a version column (`UPDATE ... WHERE id = ? AND version = ?`); pessimistic locking holds locks such as `SELECT ... FOR UPDATE`.

- Keep transactions short; do not wait on a remote API while holding database locks.
- Retry only transient, retryable failures, with bounded backoff and idempotent operations.
- Deadlocks can occur even when each transaction is correct. Lock rows in a consistent order, keep transactions small, inspect deadlock graphs, and retry the victim safely.
- A transaction is normally local to one database. It does not atomically include Kafka, RabbitMQ, email, or an HTTP call; use an outbox/saga pattern for cross-system workflows.

## Indexes and query plans

```sql
CREATE INDEX ix_order_customer_created
    ON purchase_order(customer_id, created_at DESC);
CREATE UNIQUE INDEX ux_customer_email_lower
    ON customer (lower(email)); -- PostgreSQL expression index
```

An index speeds selective reads and joins but costs disk, write work, and maintenance. Composite index order matters: a B-tree on `(customer_id, created_at)` naturally helps filters beginning with `customer_id`, not a filter on `created_at` alone. Include columns or covering-index features only after measuring. Index foreign-key lookup columns used for joins/deletes. Avoid indexing every column or low-selectivity flags by reflex.

Use `EXPLAIN` and (where safe) `EXPLAIN ANALYZE` to compare estimated and actual rows, scans, joins, sorts, and buffers. A sequential scan can be optimal for a small table or a query returning much of the table. Investigate stale statistics, skew, implicit casts, leading wildcard searches, and N+1 ORM access before adding indexes.

## Migrations and operations

Use versioned migrations (Liquibase/Flyway) committed with the application. Prefer expand-and-contract changes: add nullable/backward-compatible structures, deploy readers/writers compatible with both forms, backfill, switch usage, then remove old structures in a later release. Large backfills should be restartable and bounded. DDL locking and transactional behavior vary by database; review production migration plans and backups.

Backups require restore drills. Monitor connection pools, slow queries, lock waits, replication lag, disk, and vacuum/statistics health. Pool size is a database concurrency limit; more connections can lower throughput through contention.

## AI/agentic systems and SQL

For text-to-SQL, expose a narrowly scoped read-only identity, a small allowlisted schema, query timeouts and row limits, and inspect/approve generated SQL before execution. Treat schema and rows as untrusted input. Do not let a model choose credentials, bypass authorization, or perform arbitrary DDL/DML. For agents that trigger writes, validate business invariants in ordinary application code and make the operation idempotent.

## Senior interview questions, answers, and reasoning

1. **A query is slow after the table grows. What do you do?** Capture the exact parameterized query and plan; compare estimated/actual rows, identify expensive scans, joins, sorts, or repeated queries; check stats and skew; test a suitable index or query rewrite against representative data; compare write cost and production impact. Guessing an index without the plan can make writes slower without fixing the cause.
2. **How do you safely deploy a non-null column used by a rolling deployment?** Add it as nullable/default-compatible; deploy code that can handle both old and new records; backfill in batches; verify; add validation/constraint in a safe engine-specific phase; deploy code that relies on it; remove compatibility later. Old and new application versions coexist during rollout.
3. **When is `SERIALIZABLE` appropriate?** When a business invariant spans reads/writes that weaker isolation cannot protect and the database supports it with acceptable contention. Handle serialization failures with bounded retries and idempotent transactions. A lock or unique constraint may be simpler and more precise.
4. **Why can a foreign key or index hurt a migration?** Adding/validating constraints and building indexes can scan or lock large tables and increase write/replication load. Use the database's online/concurrent facilities when appropriate, stage validation, and rehearse on representative data.
5. **How do you keep a database update and event publication reliable?** Commit the business row and outbox row in one local transaction. A publisher reads and publishes outbox records with stable IDs; consumers deduplicate/idempotently apply them. This avoids pretending a normal SQL transaction includes a broker.
