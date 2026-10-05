# RabbitMQ interview cheat sheet

RabbitMQ is a message broker centered on exchanges, queues, and routing. Producers publish to exchanges; bindings route messages to queues; consumers receive deliveries and acknowledge them. It is useful for task distribution, work queues, and flexible routing.

## Exchanges, queues, and bindings

```text
producer -> exchange --binding/routing key--> queue -> consumer
```

- **Direct exchange:** exact routing-key match.
- **Topic exchange:** dot-separated wildcard routing (`*` one segment, `#` zero or more).
- **Fanout exchange:** copy to every bound queue.
- **Headers exchange:** route from headers rather than routing key.
- Default exchange has each queue name as a routing key. Queue durability and message persistence are separate choices.

```java
channel.exchangeDeclare("orders", "topic", true);
channel.queueDeclare("billing", true, false, false, null);
channel.queueBind("billing", "orders", "order.created");
```

A durable queue survives broker restart; a persistent message is eligible for disk persistence. Neither alone guarantees end-to-end business durability. Publisher confirms, queue type/replication, consumer acknowledgments, and storage policies matter too.

## Acknowledgements, confirms, and retries

Consumer acknowledgments tell the broker a delivery was processed. `ack` removes it; `nack/reject` can requeue or dead-letter it. Automatic ack before processing risks loss. Manual ack after an idempotent side effect gives at-least-once delivery; crashes can redeliver. Prefetch bounds in-flight deliveries and helps prevent one consumer from buffering unbounded work.

Publisher confirms tell a producer that the broker accepted a publish under its configured semantics. Use mandatory publishing/returns or alternate exchanges to detect unroutable messages; a successful socket write alone does not prove a queue received the message.

Use bounded retries with backoff. Immediate requeue loops can overload the broker. Dead-letter exchanges/queues should retain failure reason and allow controlled replay. TTL can expire messages or implement delayed retry patterns, but queues are not precision schedulers.

## Ordering, scaling, and reliability

Queue FIFO ordering can be affected by multiple consumers, redelivery, priorities, and requeue. A single active consumer gives simpler sequential processing at lower throughput. Competing consumers scale work but do not promise global completion order. Use message IDs/idempotency keys to tolerate redelivery.

Monitor ready/unacknowledged messages, publish/confirm rates, consumer utilization, redelivery, connections/channels, disk alarms, and queue growth. Quorum queues are replicated for stronger availability but have different performance/capacity tradeoffs than classic queues. Apply resource limits and flow control deliberately.

## Spring AMQP example

```java
@RabbitListener(queues = "orders.created")
public void handle(OrderCreatedEvent event) {
  service.applyIdempotently(event.orderId(), event);
}

rabbitTemplate.convertAndSend("orders.exchange", "order.created", event);
```

Declare topology as code and make it consistent across environments. Configure JSON conversion, trusted type handling, schema evolution, retries, and error handlers intentionally. Avoid coupling broker class names to public message contracts.

## Kafka comparison

RabbitMQ routes work to queues and tracks delivery/acknowledgment per message. Kafka retains partitioned logs and consumer offsets, so independent groups can replay history. RabbitMQ often fits task routing and work queues; Kafka often fits high-throughput event history, replay, and stream processing. Both can provide durable messaging and both require idempotent consumers; neither guarantees exactly-once arbitrary external side effects.

## AI/agentic systems and RabbitMQ

Use queues to isolate expensive tool/model calls from request threads. Bound prefetch and consumer concurrency to model quotas; use per-tenant fairness, timeouts, retry classification, and DLQ review. Add a task ID and deduplication store so redelivery does not execute a costly or irreversible tool twice. Do not put credentials or raw sensitive prompts in broadly accessible queues.

## Senior interview questions, answers, and reasoning

1. **What is the difference between publisher confirms and consumer acknowledgements?** Confirms cover broker acceptance of a publish; acknowledgements cover consumer completion of a delivery. They protect different ends of the path, and both can be needed.
2. **How do you prevent poison messages from blocking a queue?** Classify transient/permanent failures; cap retries with delayed backoff; route exhausted/permanent failures to a DLQ with diagnostics; provide safe redrive and idempotency. Avoid infinite immediate requeue.
3. **How does prefetch affect a slow consumer?** It bounds unacknowledged work assigned to a consumer. A large prefetch can improve throughput but harms fairness and increases buffered work/redelivery burst; tune against processing time and memory.
4. **How do you detect a successful publish that was not routed?** Enable mandatory publishing and handle returned messages, or configure an alternate exchange. Publisher confirm success only says the broker accepted the publish, not that the intended queue had a binding.
5. **How do you achieve reliable DB-plus-Rabbit publication?** Persist an outbox row with the domain update in the same SQL transaction; a publisher sends and marks it; consumers deduplicate by event ID. Broker confirms help, but crash windows still require replay-safe behavior.
