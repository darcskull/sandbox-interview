package dev.interview.lab.messaging;

import java.time.Instant;
import java.util.UUID;

/** Payload sent through either demo message broker. */
public record DemoMessageEvent(UUID id, String channel, String message, Instant publishedAt) {}
