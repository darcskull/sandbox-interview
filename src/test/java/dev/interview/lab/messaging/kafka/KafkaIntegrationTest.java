package dev.interview.lab.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import dev.interview.lab.messaging.DemoMessageEvent;
import dev.interview.lab.messaging.DemoMessagePublisher;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

/** Verifies the configured Kafka producer against an in-process broker. */
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:kafka_integration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.kafka.listener.auto-startup=false",
      "spring.kafka.admin.auto-create=false",
      "spring.rabbitmq.dynamic=false",
      "spring.rabbitmq.listener.simple.auto-startup=false"
    })
@ActiveProfiles("h2")
@EmbeddedKafka(
    partitions = 1,
    topics = "demo.messages",
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class KafkaIntegrationTest {
  @Autowired DemoMessagePublisher publisher;
  @Autowired EmbeddedKafkaBroker broker;

  @Test
  void publishesAndConsumesJsonEvent() {
    String topic = "demo.messages";
    DemoMessageEvent expected =
        new DemoMessageEvent(UUID.randomUUID(), "KAFKA", "embedded broker", Instant.now());
    Map<String, Object> consumerProperties = new HashMap<>();
    consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
    consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "integration-reader");
    consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerProperties.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
    consumerProperties.put(
        JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, DemoMessageEvent.class.getName());
    consumerProperties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "dev.interview.lab.messaging");
    consumerProperties.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);

    try (Consumer<String, DemoMessageEvent> consumer =
        new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProperties)) {
      consumer.subscribe(List.of(topic));
      publisher.publish(expected);

      ConsumerRecord<String, DemoMessageEvent> received =
          KafkaTestUtils.getSingleRecord(consumer, topic, Duration.ofSeconds(10));
      assertThat(received.value()).isEqualTo(expected);
    }
  }
}
