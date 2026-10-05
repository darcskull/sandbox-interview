package dev.interview.lab.messaging;

import dev.interview.lab.messaging.kafka.KafkaOrderEventPublisher;
import dev.interview.lab.messaging.rabbitmq.RabbitMqOrderEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!standalone")
public class CompositeOrderEventPublisher implements OrderEventPublisher {
  private final KafkaOrderEventPublisher kafkaPublisher;
  private final RabbitMqOrderEventPublisher rabbitPublisher;

  public CompositeOrderEventPublisher(
      KafkaOrderEventPublisher kafkaPublisher, RabbitMqOrderEventPublisher rabbitPublisher) {
    this.kafkaPublisher = kafkaPublisher;
    this.rabbitPublisher = rabbitPublisher;
  }

  @Override
  public void publish(OrderCreatedEvent event) {
    kafkaPublisher.publish(event);
    rabbitPublisher.publish(event);
  }
}
