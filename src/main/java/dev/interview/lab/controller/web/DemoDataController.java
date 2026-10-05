package dev.interview.lab.controller.web;

import dev.interview.lab.audit.AuditDocument;
import dev.interview.lab.audit.AuditRepository;
import dev.interview.lab.audit.DemoMessageDocument;
import dev.interview.lab.audit.DemoMessageRepository;
import dev.interview.lab.messaging.DemoMessageEvent;
import dev.interview.lab.messaging.DemoMessagePublisher;
import dev.interview.lab.model.order.OrderRequest;
import dev.interview.lab.model.order.OrderResponse;
import dev.interview.lab.service.order.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Small JSON endpoints used by the interview dashboard forms. */
@RestController
@RequestMapping("/api/demo")
public class DemoDataController {
  private final OrderService orderService;
  private final ObjectProvider<AuditRepository> auditRepository;
  private final ObjectProvider<DemoMessageRepository> messageRepository;
  private final ObjectProvider<DemoMessagePublisher> messagePublisher;

  public DemoDataController(
      OrderService orderService,
      ObjectProvider<AuditRepository> auditRepository,
      ObjectProvider<DemoMessageRepository> messageRepository,
      ObjectProvider<DemoMessagePublisher> messagePublisher) {
    this.orderService = orderService;
    this.auditRepository = auditRepository;
    this.messageRepository = messageRepository;
    this.messagePublisher = messagePublisher;
  }

  @GetMapping("/orders")
  public List<OrderResponse> orders() {
    return orderService.findAll();
  }

  @PostMapping("/orders")
  public OrderResponse createOrder(@Valid @RequestBody OrderRequest request) {
    return orderService.create(request);
  }

  @GetMapping("/mongo")
  public List<?> mongoRecords() {
    return requireAuditRepository().findAll();
  }

  @PostMapping("/mongo")
  public Object createMongoRecord(@Valid @RequestBody MongoRecordRequest request) {
    return requireAuditRepository()
        .save(new AuditDocument(null, request.type(), request.detail(), Instant.now()));
  }

  @GetMapping("/messages")
  public List<DemoMessageDocument> messages(@RequestParam String channel) {
    return requireMessageRepository()
        .findTop50ByChannelOrderByPublishedAtDesc(channel.toUpperCase());
  }

  @PostMapping("/messages")
  public Map<String, String> publishMessage(@Valid @RequestBody MessageRequest request) {
    String channel = request.channel().toUpperCase();
    if (!List.of("KAFKA", "RABBITMQ").contains(channel)) {
      throw new IllegalArgumentException("channel must be KAFKA or RABBITMQ");
    }
    UUID id = UUID.randomUUID();
    DemoMessagePublisher publisher = messagePublisher.getIfAvailable();
    if (publisher == null) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Kafka and RabbitMQ publishing is disabled in standalone mode.");
    }
    publisher.publish(new DemoMessageEvent(id, channel, request.message(), Instant.now()));
    return Map.of("id", id.toString(), "status", "published");
  }

  public record MongoRecordRequest(@NotBlank String type, @NotBlank String detail) {}

  public record MessageRequest(@NotBlank String channel, @NotBlank String message) {}

  private AuditRepository requireAuditRepository() {
    AuditRepository repository = auditRepository.getIfAvailable();
    if (repository == null) {
      throw standaloneServiceUnavailable("MongoDB");
    }
    return repository;
  }

  private DemoMessageRepository requireMessageRepository() {
    DemoMessageRepository repository = messageRepository.getIfAvailable();
    if (repository == null) {
      throw standaloneServiceUnavailable("Message brokers");
    }
    return repository;
  }

  private ResponseStatusException standaloneServiceUnavailable(String component) {
    return new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, component + " are disabled in standalone mode.");
  }
}
