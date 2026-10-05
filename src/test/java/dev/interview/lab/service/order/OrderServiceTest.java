package dev.interview.lab.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.interview.lab.messaging.OrderCreatedEvent;
import dev.interview.lab.messaging.OrderEventPublisher;
import dev.interview.lab.model.order.OrderEntity;
import dev.interview.lab.model.order.OrderNotFoundException;
import dev.interview.lab.model.order.OrderRequest;
import dev.interview.lab.model.order.OrderResponse;
import dev.interview.lab.model.order.OrderStatus;
import dev.interview.lab.repository.order.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for order mapping, persistence calls, and event publication. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
  @Mock OrderRepository repository;
  @Mock OrderEventPublisher publisher;

  @InjectMocks OrderService service;

  private UUID orderId;
  private OrderEntity savedOrder;

  @BeforeEach
  void setUp() {
    orderId = UUID.randomUUID();
    savedOrder = new OrderEntity("Ada Lovelace", new BigDecimal("42.50"));
    ReflectionTestUtils.setField(savedOrder, "id", orderId);
  }

  @Test
  void createsOrderAndPublishesMatchingEvent() {
    when(repository.save(any(OrderEntity.class))).thenReturn(savedOrder);

    OrderResponse response =
        service.create(new OrderRequest("Ada Lovelace", new BigDecimal("42.50")));

    assertThat(response.id()).isEqualTo(orderId);
    assertThat(response.customerName()).isEqualTo("Ada Lovelace");
    assertThat(response.total()).isEqualByComparingTo("42.50");
    assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
    verify(publisher)
        .publish(new OrderCreatedEvent(orderId, "Ada Lovelace", new BigDecimal("42.50")));
  }

  @Test
  void mapsAllSavedOrdersToResponses() {
    when(repository.findAll()).thenReturn(List.of(savedOrder));

    List<OrderResponse> responses = service.findAll();

    assertThat(responses)
        .singleElement()
        .satisfies(
            response -> {
              assertThat(response.id()).isEqualTo(orderId);
              assertThat(response.customerName()).isEqualTo("Ada Lovelace");
              assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
            });
  }

  @Test
  void findsExistingOrder() {
    when(repository.findById(orderId)).thenReturn(Optional.of(savedOrder));

    OrderResponse response = service.find(orderId);

    assertThat(response.id()).isEqualTo(orderId);
    assertThat(response.customerName()).isEqualTo("Ada Lovelace");
  }

  @Test
  void throwsDomainExceptionWhenOrderDoesNotExist() {
    when(repository.findById(orderId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.find(orderId)).isInstanceOf(OrderNotFoundException.class);
  }
}
