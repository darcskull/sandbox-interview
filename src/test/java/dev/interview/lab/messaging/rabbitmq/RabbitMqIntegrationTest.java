package dev.interview.lab.messaging.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import dev.interview.lab.messaging.DemoMessageEvent;
import dev.interview.lab.messaging.DemoMessagePublisher;
import dev.interview.lab.messaging.MessagingConfiguration;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/** Verifies RabbitTemplate send and receive against disposable RabbitMQ. */
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:rabbit_integration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.rabbitmq.dynamic=false",
      "spring.rabbitmq.listener.simple.auto-startup=false",
      "spring.rabbitmq.listener.direct.auto-startup=false"
    })
@ActiveProfiles("h2")
@Testcontainers(disabledWithoutDocker = true)
class RabbitMqIntegrationTest {
  @Container
  static final RabbitMQContainer RABBIT =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management-alpine"));

  @Autowired RabbitTemplate rabbitTemplate;
  @Autowired DemoMessagePublisher publisher;

  @DynamicPropertySource
  static void configureRabbit(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.host", RABBIT::getHost);
    registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
    registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
  }

  @Test
  void publishesAndReceivesMessage() {
    String queue = MessagingConfiguration.DEMO_QUEUE;
    DemoMessageEvent expected =
        new DemoMessageEvent(
            UUID.randomUUID(), "RABBITMQ", "RabbitMQ integration message", Instant.now());
    rabbitTemplate.execute(channel -> channel.queueDeclare(queue, false, false, true, null));

    publisher.publish(expected);

    Message received = rabbitTemplate.receive(queue, 10_000L);
    assertThat(received).isNotNull();
    assertThat(new String(received.getBody(), StandardCharsets.UTF_8)).contains(expected.message());
  }
}
