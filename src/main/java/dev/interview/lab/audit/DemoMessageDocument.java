package dev.interview.lab.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Mongo mirror of messages successfully received by a broker consumer. */
@Document("demo_messages")
public record DemoMessageDocument(
    @Id String documentId, UUID messageId, String channel, String message, Instant publishedAt) {}
