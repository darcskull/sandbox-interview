# Data and messaging architecture comparisons

These are tradeoffs, not universal rankings. Select around correctness, access patterns, delivery semantics, operations, and team expertise.

## Relational SQL vs document/NoSQL databases

| Concern | Relational SQL | Document / NoSQL (example: MongoDB) |
|---|---|---|
| Primary model | Tables, rows, relationships | Documents, aggregates, keys/values, or other model by product |
| Query strength | Joins, constraints, ad-hoc relational queries | Fast access around modeled document/key patterns; product-specific query abilities |
| Integrity | Foreign keys, unique/check constraints, transactions | Validation and atomicity vary by product; MongoDB validates documents and is atomic per document |
| Schema | Explicit schema and migrations | Flexible shape, but still needs validation/versioning and migration discipline |
| Scaling | Vertical and distributed options vary; relational semantics are strong | Many products emphasize horizontal distribution; partition/shard-key design is consequential |
| Good fit | Financial/order systems, relational reporting, multi-row invariants | Aggregate/document workloads, evolving records, bounded embedded data |
| Main risk | Poor indexes/normalization/connection pressure; expensive cross-service coupling | Unbounded documents, duplicated inconsistent facts, poor shard/query modeling |

Ask: Are relationships and cross-record invariants central? Are reads naturally aggregate-shaped? What consistency/staleness is acceptable? What does the reporting path need? What are backup, restore, migration, and operational skills? “NoSQL scales” and “SQL cannot scale” are both false generalizations.

## Kafka vs RabbitMQ

| Concern | Kafka | RabbitMQ |
|---|---|---|
| Core abstraction | Retained partitioned log | Exchanges route messages into queues |
| Consumer progress | Offset per partition and consumer group | Delivery and ack per message/queue |
| Replay | Native while records remain retained | Usually requeue/republish; not a general retained event log |
| Ordering | Within one partition | Queue ordering with caveats from multiple consumers, redelivery, priorities |
| Routing | Topics/partitions/keys, consumer groups | Direct/topic/fanout/headers exchange and bindings |
| Typical use | Event history, replay, high-throughput streams, stream processing | Work queues, task routing, request/reply patterns |
| Backpressure | Consumer lag accumulates in retained log | Queue depth/unacked deliveries; broker flow control and memory/disk alarms |
| Failure design | Offset commits, retries, DLQ topics | Ack/nack, confirms/returns, retry queues and DLQs |

Both can provide durable delivery when correctly configured. Both commonly give at-least-once effects at application boundaries; consumers should be idempotent. Neither makes an arbitrary database write exactly once. Use an outbox for reliable publication from a database transaction.

Decision examples: choose Kafka if several teams need independent replayable views of the same event stream. Choose RabbitMQ if routing jobs to appropriate workers and acknowledging each delivery are central. Consider operational fit, ordering, replay horizon, throughput, message size, latency, and required topology. A system can use both, but avoid dual-publishing without an explicit consistency strategy.

## Storage, cache, and event log are different jobs

- SQL/Mongo hold queryable application state and enforce their own data model constraints.
- Redis usually accelerates or coordinates access; decide if it is disposable cache or durable state.
- Kafka stores a retained event log and consumer progress, not a relational query database.
- RabbitMQ holds routed pending work, not normally the canonical history of all business events.

Keeping these roles clear makes recovery possible: identify source of truth, replay boundary, cache rebuild path, and message redrive strategy.

## AI/agentic workload placement

| Need | Useful building block | Design caution |
|---|---|---|
| Durable business record | SQL or document DB | Store model/prompt versions and provenance; enforce tenant boundaries |
| Search over unstructured content | Search/vector index plus source store | Rebuildable index; retrieval quality and authorization filters |
| Long-running inference/tool task | Kafka or RabbitMQ worker | Idempotency, quotas, deadline, bounded retries, DLQ |
| Session/rate-limit/dedup state | Redis | TTL and eviction semantics; never use cache state as authority by accident |
| Audit/replay | Append-only events plus immutable/object storage as needed | Retention, sensitive-data controls, schema evolution |

## Senior interview questions, answers, and reasoning

1. **How would you choose MongoDB over SQL for a new service?** Compare access patterns, consistency invariants, transaction boundaries, evolving shape, reporting, and operations using realistic reads/writes. Choose a model that naturally represents aggregates; a fashionable technology is not a requirement.
2. **How do you choose Kafka or RabbitMQ for asynchronous work?** Establish replay needs, subscriber independence, routing, ordering, delivery/ack semantics, backlog behavior, and operational familiarity. A queue/work-routing answer often favors RabbitMQ; retained streams and replay often favor Kafka, but validate volume and failure behavior.
3. **Can Kafka replace a database?** It can be the durable event source for a design built around logs and projections, but it does not provide general transactional relational queries or arbitrary indexed lookups. Add projections/state stores and define replay/schema/retention strategy.
4. **Why not use Redis as a cache, queue, database, and lock manager for everything?** Its data structures are versatile, but each role imposes different durability, consistency, eviction, ordering, and failover requirements. Explicitly configure and test those guarantees rather than assuming one server setting covers all roles.
5. **How do you keep several stores consistent?** Avoid distributed dual writes. Use a local transaction plus outbox, idempotent consumers, versioned events, reconciliation, and observable lag. State the consistency window and repair process.
