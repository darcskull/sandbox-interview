package dev.interview.lab.audit;

import dev.interview.lab.messaging.OrderCreatedEvent;
import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderAuditListener {
  private final AuditRepository repository;

  public OrderAuditListener(AuditRepository repository) {
    this.repository = repository;
  }

  @KafkaListener(topics = "orders.created", groupId = "interview-audit")
  public void record(OrderCreatedEvent event) {
    repository.save(
        new AuditDocument(
            null,
            "ORDER_CREATED",
            "Order %s created for %s (total %s)"
                .formatted(event.orderId(), event.customerName(), event.total()),
            Instant.now()));
  }
}
