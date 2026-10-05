package dev.interview.lab.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, String customerName, BigDecimal total) {}
