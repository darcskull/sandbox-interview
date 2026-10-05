package dev.interview.lab.messaging.rabbitmq;

import dev.interview.lab.messaging.MessagingConfiguration;
import dev.interview.lab.messaging.OrderCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Publishes order events to the durable RabbitMQ orders queue. */
@Component
@Profile("!standalone")
public class RabbitMqOrderEventPublisher {
  private final RabbitTemplate rabbit;

  public RabbitMqOrderEventPublisher(RabbitTemplate rabbit) {
    this.rabbit = rabbit;
  }

  public void publish(OrderCreatedEvent event) {
    rabbit.convertAndSend(MessagingConfiguration.ORDER_QUEUE, event);
  }
}
