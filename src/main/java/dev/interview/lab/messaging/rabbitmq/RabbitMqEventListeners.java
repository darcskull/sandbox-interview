package dev.interview.lab.messaging.rabbitmq;

import dev.interview.lab.audit.DemoMessageDocument;
import dev.interview.lab.audit.DemoMessageRepository;
import dev.interview.lab.messaging.DemoMessageEvent;
import dev.interview.lab.messaging.MessagingConfiguration;
import dev.interview.lab.messaging.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumes RabbitMQ queues and mirrors dashboard messages into MongoDB. */
@Component
public class RabbitMqEventListeners {
  private static final Logger log = LoggerFactory.getLogger(RabbitMqEventListeners.class);
  private final DemoMessageRepository demoMessages;

  public RabbitMqEventListeners(DemoMessageRepository demoMessages) {
    this.demoMessages = demoMessages;
  }

  @RabbitListener(queues = MessagingConfiguration.ORDER_QUEUE)
  public void onOrderCreated(OrderCreatedEvent event) {
    log.info("RabbitMQ consumed order {}", event.orderId());
  }

  @RabbitListener(queues = MessagingConfiguration.DEMO_QUEUE)
  public void onDemoMessage(DemoMessageEvent event) {
    demoMessages.save(
        new DemoMessageDocument(
            null, event.id(), event.channel(), event.message(), event.publishedAt()));
    log.info("RabbitMQ consumed demo message {}", event.id());
  }
}
