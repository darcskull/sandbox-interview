package dev.interview.lab.model.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 120)
  private String customerName;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrderStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  protected OrderEntity() {}

  public OrderEntity(String customerName, BigDecimal total) {
    this.customerName = customerName;
    this.total = total;
    this.status = OrderStatus.CREATED;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getCustomerName() {
    return customerName;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
