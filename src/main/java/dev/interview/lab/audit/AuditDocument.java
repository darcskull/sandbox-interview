package dev.interview.lab.audit;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("audit_events")
public record AuditDocument(@Id String id, String type, String detail, Instant occurredAt) {}
