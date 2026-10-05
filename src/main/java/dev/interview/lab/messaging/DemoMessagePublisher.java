package dev.interview.lab.messaging;

import dev.interview.lab.messaging.kafka.KafkaDemoMessagePublisher;
import dev.interview.lab.messaging.rabbitmq.RabbitMqDemoMessagePublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Publishes demo messages to one selected local broker. */
@Component
@Profile("!standalone")
public class DemoMessagePublisher {
  private final KafkaDemoMessagePublisher kafkaPublisher;
  private final RabbitMqDemoMessagePublisher rabbitPublisher;

  public DemoMessagePublisher(
      KafkaDemoMessagePublisher kafkaPublisher, RabbitMqDemoMessagePublisher rabbitPublisher) {
    this.kafkaPublisher = kafkaPublisher;
    this.rabbitPublisher = rabbitPublisher;
  }

  public void publish(DemoMessageEvent event) {
    if ("KAFKA".equals(event.channel())) {
      kafkaPublisher.publish(event);
    } else {
      rabbitPublisher.publish(event);
    }
  }
}
