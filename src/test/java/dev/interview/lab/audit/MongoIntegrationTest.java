package dev.interview.lab.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/** Verifies Mongo persistence against a disposable MongoDB instance. */
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:mongo_integration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.autoconfigure.exclude=org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration,org.springframework.boot.amqp.autoconfigure.AmqpAutoConfiguration"
    })
@Testcontainers(disabledWithoutDocker = true)
class MongoIntegrationTest {
  @Container
  static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:8.0"));

  @Autowired DemoMessageRepository repository;

  @DynamicPropertySource
  static void configureMongo(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
  }

  @Test
  void savesAndFindsMessagesByChannel() {
    Instant publishedAt = Instant.now();
    DemoMessageDocument message =
        new DemoMessageDocument(null, UUID.randomUUID(), "MONGO_TEST", "persisted", publishedAt);

    DemoMessageDocument saved = repository.save(message);

    assertThat(saved.documentId()).isNotBlank();
    assertThat(repository.findTop50ByChannelOrderByPublishedAtDesc("MONGO_TEST")).contains(saved);
  }
}
