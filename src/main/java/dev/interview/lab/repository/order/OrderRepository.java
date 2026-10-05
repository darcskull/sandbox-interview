package dev.interview.lab.repository.order;

import dev.interview.lab.model.order.OrderEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {}
