package dev.interview.lab.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Keeps H2 order practice usable when the local message brokers are not running. */
@Component
@Profile("standalone")
public class StandaloneOrderEventPublisher implements OrderEventPublisher {
  private static final Logger log = LoggerFactory.getLogger(StandaloneOrderEventPublisher.class);

  @Override
  public void publish(OrderCreatedEvent event) {
    log.info("Standalone profile: skipped broker publish for order {}", event.orderId());
  }
}
