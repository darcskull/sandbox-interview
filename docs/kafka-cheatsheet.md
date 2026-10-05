# Apache Kafka interview cheat sheet

Kafka is a distributed append-only event log. Producers append records to topic partitions; consumers track offsets. It is well suited to durable event streams, replay, and fan-out across independent consumer groups.

## Topics, partitions, keys, and records

```text
record = key + value + headers + timestamp
topic  = ordered partitions
partition = append-only log with offset 0, 1, 2, ...
```

Ordering is guaranteed within a partition, not across a topic. A key is hashed to a partition so records with the same key normally stay ordered together. Null keys can be distributed among partitions. More partitions enable parallelism but increase broker/client overhead and change the partition mapping if partition count changes.

```java
kafkaTemplate.send("orders.created", order.id().toString(), event);
```

Partition count should reflect expected throughput and consumer parallelism. Within one consumer group, each partition is assigned to at most one active consumer at a time; adding consumers beyond partition count does not add read parallelism.

## Producer reliability

Use explicit serializers, stable keys, schema/version compatibility, and bounded delivery timeouts. `acks=all` plus appropriate replication and `min.insync.replicas` reduces acknowledged data loss risk. Idempotent producers deduplicate producer retries within Kafka's defined scope; transactions can atomically write to partitions and commit consumed offsets. They do not atomically commit an external SQL transaction.

Producer `send` is asynchronous. Observe completion/errors when the business contract requires confirmation; do not block every request without capacity/latency analysis. Retries need idempotency because application-level retries can duplicate business actions. Use an outbox to bridge SQL commit and publish.

## Consumers, groups, and offsets

```properties
group.id=order-projection-v2
auto.offset.reset=earliest
enable.auto.commit=false
```

Offsets represent consumer progress. `earliest` applies when no valid committed offset exists; it is not a request to replay on every restart. Commit after processing for at-least-once delivery; a crash between side effect and commit can replay. Commit before processing risks losing work. Exactly-once processing requires a carefully bounded Kafka transaction and compatible read-process-write path; arbitrary external side effects still need idempotency/deduplication.

Rebalances reassign partitions when membership or metadata changes. Long processing and unbounded polls can exceed liveness/poll intervals. Use bounded batches, pause/resume under backpressure, tune poll/heartbeat settings with care, and monitor lag and rebalance rate. A dead-letter topic should preserve original context, failure metadata, and replay tooling.

## Retention, compaction, and schemas

Time/size retention deletes old log segments; consumers cannot replay past retained data. Log compaction keeps the latest value per key over time and is useful for state snapshots, not a complete event history. Tombstones mark deletes for compacted topics.

Use a schema registry or another governed format for long-lived event contracts. Prefer additive compatible changes, defaults for new fields, and consumer tolerance of unknown fields. Schema evolution rules should match deployment order and replay horizon.

## Operations and commands

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic orders.created
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group order-projection
```

Monitor under-replicated partitions, ISR changes, disk, request latency, producer errors, consumer lag, fetch rate, and rebalances. Capacity includes network/disk throughput, replication factor, retention, and partition count. Secure TLS/SASL/ACL access and separate producer/consumer identities.

## AI/agentic systems and Kafka

Publish durable lifecycle events such as `DocumentIngested` or `InferenceRequested`; route slow model work to an isolated consumer group with bounded concurrency, retry policy, and DLQ. Include correlation, tenant, causation, and idempotency IDs, but avoid secrets and sensitive prompts in broadly retained topics. Version prompt/model metadata and outputs where auditability requires it. Consumer lag is a product-level delay signal.

## Senior interview questions, answers, and reasoning

1. **How do you preserve per-order ordering while scaling consumers?** Key records by order ID and keep a partition-stable keying scheme. Parallelize across partitions; one partition's order is sequential for its group. Repartitioning can change mapping and requires migration planning.
2. **What does Kafka “exactly once” guarantee?** Idempotent producer plus transactions can atomically write Kafka records and consumed offsets in Kafka. It does not make email, SQL, or arbitrary HTTP side effects exactly once. Those need idempotent operations, deduplication, or an outbox/inbox design.
3. **Why is consumer lag rising?** Determine whether production increased, consumers slowed, partitions are unbalanced, downstream dependencies are bottlenecked, rebalances occur, or errors cause retries. Scale only if partition count and dependency capacity permit; otherwise fix work per record/backpressure.
4. **When use compaction rather than time retention?** Compaction retains the latest value per key and is useful for reconstructing current keyed state; time retention keeps a bounded event history. Compaction is asynchronous and does not provide a precise delete-time guarantee.
5. **How do you evolve an event safely?** Make changes compatible with all concurrently deployed readers and retained historical records; add fields with defaults, tolerate unknowns, version only for genuine incompatible changes, and test old/new reader-writer combinations before rollout.
