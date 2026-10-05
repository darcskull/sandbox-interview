# Redis and caching interview cheat sheet

Redis is an in-memory data structure server used for caches, counters, coordination, streams, and other workloads. Treat each use according to its durability and consistency requirements; “in memory” does not automatically mean disposable or persistent.

## Common data structures and commands

```text
SET user:42:profile '{"name":"Ada"}' EX 300 NX
GET user:42:profile
INCR rate:user:42
HSET session:abc userId 42 role reader
HGETALL session:abc
LPUSH jobs job-1
XADD events * kind order.created orderId 42
```

- Strings: values, counters, compact serialized objects.
- Hashes: field/value records; Sets: uniqueness/membership; Sorted sets: ranking/time ordering.
- Lists: simple queues; Streams: append-only records with consumer groups and pending entries.
- Use atomic server commands or Lua scripts for compound operations. Keep scripts bounded.

## Cache-aside and expiry

```text
read(key): cache lookup -> on miss read source of truth -> populate with TTL -> return
write(data): update source of truth -> invalidate/update cache according to policy
```

Cache-aside is simple, but concurrent misses can stampede the database. Mitigate with request coalescing, jittered TTLs, stale-while-revalidate, short-lived locks, or prewarming where justified. TTL introduces staleness bounds, not immediate consistency. Use explicit invalidation/versioning where requirements demand it.

- **Cache stampede:** many misses recompute the same hot key.
- **Cache penetration:** repeated requests for missing keys; use short negative caching where safe.
- **Cache avalanche:** many entries expire together; add TTL jitter and staggered refresh.
- **Hot key:** one key receives disproportionate traffic; replicate/read-scale or redesign access.

Avoid caching authorization decisions longer than the revocation model permits. Namespace keys by tenant/environment; validate serialized values and use size limits. Avoid unbounded key growth.

## Expiration, eviction, and memory

Set TTL deliberately. Redis eviction policy applies when memory reaches its configured maximum; policies may evict all keys or only keys with expiration. Monitor memory fragmentation, evictions, hit rate, latency, blocked clients, key cardinality, and persistence/replication lag. A high hit rate can hide stale or incorrect cache behavior.

Persistence options (RDB snapshots/AOF) change recovery and write costs. Replication improves availability/read capacity but does not itself make every acknowledged write durable. Back up and test restore when Redis stores authoritative state.

## Distributed locks and limits

`SET lock:key token NX PX 10000` is a common single-instance lease pattern. Release only if the stored token still matches; otherwise one owner may release another owner's lock after lease expiry. Lease expiry can allow overlapping workers when a process pauses. For correctness-critical coordination, use fencing tokens or a system with appropriate consensus/transaction semantics. A Redis lock is not automatically a safe distributed mutex.

Rate limiting uses atomic counters with expiration, sorted sets, or Lua for token/leaky buckets. Clarify fixed-window boundary bursts, clock source, tenant keying, and what happens if the cache is unavailable.

## AI/agentic systems and Redis

Use Redis for short-lived session state, rate limits, deduplication windows, and ephemeral job coordination, with TTLs and recovery semantics. It can support semantic/vector indexes where available, but choose a durable source for canonical conversations or audit. Scope keys by tenant, cap context size, and do not treat cache presence as authorization.

## Senior interview questions, answers, and reasoning

1. **How do you choose a cache consistency strategy?** Start from tolerated staleness and read/write shape. Define source of truth, invalidation/update path, TTL fallback, race behavior, and cache outage behavior. Then test concurrency and measure hit ratio and stale-read incidents.
2. **How would you prevent a hot-key stampede?** Coalesce concurrent loads, add TTL jitter, serve bounded stale data while refresh occurs, and protect origin with concurrency limits. A lock may coordinate refresh but must have leases/tokens and failure handling.
3. **Is Redis appropriate as the source of truth?** It can be, if data-loss/recovery, persistence, replication, eviction, and transactional semantics meet the business requirement. Configure max memory/eviction so authoritative data is not silently evicted; test failover/restore.
4. **What is unsafe about a simple distributed lock?** Lease expiration can occur while the original holder is paused, so a second holder enters concurrently. Use unique tokens to release safely and fencing tokens to reject stale owners at the resource.
5. **What does cache hit rate tell you?** It says how often reads found entries, not whether values are correct, fresh, or worth caching. Track latency, origin load, stale/miss patterns, eviction, and business correctness too.
