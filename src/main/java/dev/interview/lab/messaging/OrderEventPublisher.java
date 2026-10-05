package dev.interview.lab.messaging;

public interface OrderEventPublisher {
  void publish(OrderCreatedEvent event);
}
