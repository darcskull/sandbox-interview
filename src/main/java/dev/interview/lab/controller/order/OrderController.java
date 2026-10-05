package dev.interview.lab.controller.order;

import dev.interview.lab.model.order.OrderNotFoundException;
import dev.interview.lab.model.order.OrderRequest;
import dev.interview.lab.model.order.OrderResponse;
import dev.interview.lab.service.order.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse create(@Valid @RequestBody OrderRequest request) {
    return service.create(request);
  }

  @GetMapping
  public List<OrderResponse> findAll() {
    return service.findAll();
  }

  @GetMapping("/{id}")
  public OrderResponse find(@PathVariable UUID id) {
    return service.find(id);
  }

  @ExceptionHandler(OrderNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse notFound(OrderNotFoundException exception) {
    return new ErrorResponse(exception.getMessage());
  }

  public record ErrorResponse(String message) {}
}
