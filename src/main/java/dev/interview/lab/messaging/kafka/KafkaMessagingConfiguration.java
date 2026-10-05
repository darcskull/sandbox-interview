package dev.interview.lab.messaging.kafka;

import dev.interview.lab.messaging.MessagingConfiguration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

/** Declares the topics used by the Kafka adapter. */
@Configuration
@Profile("!standalone")
public class KafkaMessagingConfiguration {
  @Bean
  NewTopic demoTopic() {
    return TopicBuilder.name(MessagingConfiguration.DEMO_TOPIC).partitions(1).replicas(1).build();
  }

  @Bean
  NewTopic orderTopic() {
    return TopicBuilder.name(MessagingConfiguration.ORDER_TOPIC).partitions(1).replicas(1).build();
  }
}
