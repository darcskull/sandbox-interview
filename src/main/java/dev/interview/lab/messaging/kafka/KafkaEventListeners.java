package dev.interview.lab.messaging.kafka;

import dev.interview.lab.audit.DemoMessageDocument;
import dev.interview.lab.audit.DemoMessageRepository;
import dev.interview.lab.messaging.DemoMessageEvent;
import dev.interview.lab.messaging.MessagingConfiguration;
import dev.interview.lab.messaging.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes the Kafka topics and mirrors dashboard messages into MongoDB. */
@Component
@Profile("!standalone")
public class KafkaEventListeners {
  private static final Logger log = LoggerFactory.getLogger(KafkaEventListeners.class);
  private final DemoMessageRepository demoMessages;

  public KafkaEventListeners(DemoMessageRepository demoMessages) {
    this.demoMessages = demoMessages;
  }

  @KafkaListener(topics = MessagingConfiguration.ORDER_TOPIC, groupId = "interview-lab")
  public void onOrderCreated(OrderCreatedEvent event) {
    log.info("Kafka consumed order {}", event.orderId());
  }

  @KafkaListener(topics = MessagingConfiguration.DEMO_TOPIC, groupId = "interview-demo-messages")
  public void onDemoMessage(DemoMessageEvent event) {
    demoMessages.save(
        new DemoMessageDocument(
            null, event.id(), event.channel(), event.message(), event.publishedAt()));
    log.info("Kafka consumed demo message {}", event.id());
  }
}
