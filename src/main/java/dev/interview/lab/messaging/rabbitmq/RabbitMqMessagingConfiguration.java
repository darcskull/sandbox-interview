package dev.interview.lab.messaging.rabbitmq;

import dev.interview.lab.messaging.MessagingConfiguration;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Declares durable RabbitMQ queues and the JSON message converter. */
@Configuration
@Profile("!standalone")
public class RabbitMqMessagingConfiguration {
  @Bean
  Queue orderQueue() {
    return QueueBuilder.durable(MessagingConfiguration.ORDER_QUEUE).build();
  }

  @Bean
  Queue demoQueue() {
    return QueueBuilder.durable(MessagingConfiguration.DEMO_QUEUE).build();
  }

  @Bean
  JacksonJsonMessageConverter amqpMessageConverter() {
    return new JacksonJsonMessageConverter();
  }
}
