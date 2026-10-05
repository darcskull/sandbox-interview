package dev.interview.lab.messaging.kafka;

import dev.interview.lab.messaging.DemoMessageEvent;
import dev.interview.lab.messaging.MessagingConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Sends dashboard messages to Kafka with a stable message key. */
@Component
@Profile("!standalone")
public class KafkaDemoMessagePublisher {
  private final KafkaTemplate<String, DemoMessageEvent> kafka;

  public KafkaDemoMessagePublisher(KafkaTemplate<String, DemoMessageEvent> kafka) {
    this.kafka = kafka;
  }

  public void publish(DemoMessageEvent event) {
    kafka.send(MessagingConfiguration.DEMO_TOPIC, event.id().toString(), event);
  }
}
