package dev.interview.lab.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import dev.interview.lab.messaging.kafka.KafkaDemoMessagePublisher;
import dev.interview.lab.messaging.rabbitmq.RabbitMqDemoMessagePublisher;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for routing demo messages to the selected broker adapter. */
@ExtendWith(MockitoExtension.class)
class DemoMessagePublisherTest {
  @Mock KafkaDemoMessagePublisher kafkaPublisher;
  @Mock RabbitMqDemoMessagePublisher rabbitPublisher;

  private DemoMessagePublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new DemoMessagePublisher(kafkaPublisher, rabbitPublisher);
  }

  @Test
  void routesKafkaMessageOnlyToKafkaAdapter() {
    DemoMessageEvent event = message("KAFKA");

    publisher.publish(event);

    verify(kafkaPublisher).publish(event);
    verifyNoInteractions(rabbitPublisher);
  }

  @Test
  void routesRabbitMessageOnlyToRabbitAdapter() {
    DemoMessageEvent event = message("RABBITMQ");

    publisher.publish(event);

    verify(rabbitPublisher).publish(event);
    verifyNoInteractions(kafkaPublisher);
  }

  private DemoMessageEvent message(String channel) {
    return new DemoMessageEvent(UUID.randomUUID(), channel, "unit test", Instant.now());
  }
}
