package dev.interview.lab.service.order;

import dev.interview.lab.messaging.OrderCreatedEvent;
import dev.interview.lab.messaging.OrderEventPublisher;
import dev.interview.lab.model.order.OrderEntity;
import dev.interview.lab.model.order.OrderNotFoundException;
import dev.interview.lab.model.order.OrderRequest;
import dev.interview.lab.model.order.OrderResponse;
import dev.interview.lab.repository.order.OrderRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private final OrderRepository repository;
  private final OrderEventPublisher publisher;

  public OrderService(OrderRepository repository, OrderEventPublisher publisher) {
    this.repository = repository;
    this.publisher = publisher;
  }

  @Transactional
  public OrderResponse create(OrderRequest request) {
    OrderEntity saved = repository.save(new OrderEntity(request.customerName(), request.total()));
    publisher.publish(
        new OrderCreatedEvent(saved.getId(), saved.getCustomerName(), saved.getTotal()));
    return OrderResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> findAll() {
    return repository.findAll().stream().map(OrderResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public OrderResponse find(UUID id) {
    return repository
        .findById(id)
        .map(OrderResponse::from)
        .orElseThrow(() -> new OrderNotFoundException(id));
  }
}
