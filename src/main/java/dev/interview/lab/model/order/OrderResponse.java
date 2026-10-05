package dev.interview.lab.model.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id, String customerName, BigDecimal total, OrderStatus status, Instant createdAt) {
  public static OrderResponse from(OrderEntity order) {
    return new OrderResponse(
        order.getId(),
        order.getCustomerName(),
        order.getTotal(),
        order.getStatus(),
        order.getCreatedAt());
  }
}
