package dev.interview.lab.messaging.kafka;

import dev.interview.lab.messaging.MessagingConfiguration;
import dev.interview.lab.messaging.OrderCreatedEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes order events to the Kafka topic using the order ID as the key. */
@Component
@Profile("!standalone")
public class KafkaOrderEventPublisher {
  private final KafkaTemplate<String, OrderCreatedEvent> kafka;

  public KafkaOrderEventPublisher(KafkaTemplate<String, OrderCreatedEvent> kafka) {
    this.kafka = kafka;
  }

  public void publish(OrderCreatedEvent event) {
    kafka.send(MessagingConfiguration.ORDER_TOPIC, event.orderId().toString(), event);
  }
}
