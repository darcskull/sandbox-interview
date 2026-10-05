# MongoDB interview cheat sheet

MongoDB is a document database. A collection stores BSON documents; documents can have nested objects and arrays. A good document model follows read/write boundaries and lifecycle, not a mechanical one-table-to-one-collection conversion.

## CRUD and document shapes

```javascript
db.customers.insertOne({
  _id: "cust-42",
  email: "ada@example.test",
  preferences: { locale: "en", alerts: true },
  tags: ["early-access"],
  createdAt: new Date()
});

db.customers.find({ "preferences.locale": "en" }, { email: 1, tags: 1 });
db.customers.updateOne(
  { _id: "cust-42" },
  { $set: { "preferences.alerts": false }, $addToSet: { tags: "beta" } }
);
db.customers.deleteOne({ _id: "cust-42" });
```

`_id` is the document key and is indexed uniquely by default. Dot notation addresses nested values. Update operators such as `$set`, `$inc`, `$push`, and `$addToSet` perform targeted modifications; replacement updates replace most of the document, so use them carefully. Use schema validation for required fields/types even when the schema is flexible.

## Modeling and consistency

- Embed bounded child data when it is read/updated with the parent, has a manageable size, and shares its lifecycle. Embedding enables one-document atomic updates and one round trip.
- Reference independently queried, shared, large, or unbounded entities. Arrays that grow without bound risk document limits and expensive rewrites.
- Denormalized copies are a consistency decision: identify the source of truth, update path, repair/rebuild procedure, and acceptable staleness.
- Single-document operations are atomic. Multi-document transactions exist for supported deployments but cost more; prefer modeling around atomic document boundaries when that fits the domain.
- BSON has a document-size limit; don't treat a document as an unlimited blob.

## Indexes, aggregation, and explain

```javascript
db.orders.createIndex({ customerId: 1, createdAt: -1 });
db.orders.createIndex({ expiresAt: 1 }, { expireAfterSeconds: 0 }); // TTL

db.orders.aggregate([
  { $match: { status: "PAID", createdAt: { $gte: ISODate("2026-01-01") } } },
  { $group: { _id: "$customerId", revenue: { $sum: "$total" } } },
  { $sort: { revenue: -1 } },
  { $limit: 20 }
]);

db.orders.find({ customerId: "cust-42" }).sort({ createdAt: -1 }).explain("executionStats");
```

Compound index order matters; equality predicates generally precede sort/range fields. Multikey indexes support array fields but have restrictions, especially for compound arrays. Unique and partial indexes encode useful invariants. TTL cleanup is asynchronous, not an exact-time scheduler. Aggregation pipelines transform documents stage by stage; filter early when it preserves semantics and reduces work. Explain output shows examined versus returned documents and index usage.

## Replication, sharding, and durability

Replica sets maintain redundant copies and elect a primary. Read preference controls eligible members; secondary reads can be stale. Write concern controls acknowledgment/durability expectations; read concern controls consistency of observed data. Tune them to business semantics, not blindly to maximum speed.

Sharding distributes data and load by a shard key. A poor shard key can create hotspots or scatter-gather queries. Choose a key with cardinality and write/read distribution in mind; validate resharding and operational costs before production. Sharding is not an automatic speed switch.

Change streams provide resumable change notifications for replica sets/sharded clusters. Consumers must handle resume tokens, duplicate processing, outages, and backpressure. A change stream is useful for propagation but is not a replacement for a durable domain event contract in every architecture.

## SQL and MongoDB comparison

Use relational SQL when joins, strong cross-row constraints, multi-entity transactions, and ad-hoc relational reporting dominate. Use MongoDB when aggregate/document access patterns, flexible evolving records, and horizontal distribution fit better. Both support indexes, replication, transactions (with different costs/semantics), and schema design. NoSQL does not mean “no schema”; it moves more schema discipline into validation and application design.

## AI/agentic systems and MongoDB

MongoDB can store conversations, tool traces, metadata, and vector-searchable chunks. Keep authorization/tenant filters in trusted server code; never allow model-produced filters to expand a query scope. Validate document shape and maximum payload size. For vector retrieval, version embeddings and chunk metadata and measure retrieval quality; vector similarity alone is not authorization.

## Senior interview questions, answers, and reasoning

1. **Embed or reference an array of child records?** Ask how the data is read, updated, shared, bounded, and retained. Embed bounded owned children used atomically with the parent; reference independently queried/shared or unbounded data. Explain write amplification and consistency for denormalized copies.
2. **A query scans many documents despite an index. Why?** Inspect `explain("executionStats")`, predicate selectivity, compound key order, sort support, collation/type mismatches, multikey behavior, and actual returned versus examined counts. A low-selectivity predicate or mismatched sort can make a scan reasonable.
3. **How do you prevent duplicate side effects from a change stream?** Persist a resume position and process with an idempotency key/unique constraint; coordinate checkpointing with the side effect or use an outbox/dedup store. Expect replay after restart and design for at-least-once delivery.
4. **When would you use a transaction?** When a true invariant spans multiple documents and cannot be represented by a single-document atomic operation or workflow. Keep transactions short, use suitable read/write concerns, and account for retries and deployment topology.
5. **How would you choose a shard key?** Use actual query targeting, cardinality, monotonicity, write distribution, and growth expectations. Validate with production-like traffic; avoid hot partitions and keys that force broad scatter-gather reads.
