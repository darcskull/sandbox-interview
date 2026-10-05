package dev.interview.lab.messaging.rabbitmq;

import dev.interview.lab.messaging.DemoMessageEvent;
import dev.interview.lab.messaging.MessagingConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Sends dashboard messages to the durable RabbitMQ demo queue. */
@Component
@Profile("!standalone")
public class RabbitMqDemoMessagePublisher {
  private final RabbitTemplate rabbit;

  public RabbitMqDemoMessagePublisher(RabbitTemplate rabbit) {
    this.rabbit = rabbit;
  }

  public void publish(DemoMessageEvent event) {
    rabbit.convertAndSend(MessagingConfiguration.DEMO_QUEUE, event);
  }
}
