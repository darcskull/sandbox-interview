package dev.interview.lab.messaging;

/** Shared destinations used by both broker adapters. */
public final class MessagingConfiguration {
  public static final String ORDER_QUEUE = "orders.created";
  public static final String DEMO_QUEUE = "demo.messages";
  public static final String ORDER_TOPIC = "orders.created";
  public static final String DEMO_TOPIC = "demo.messages";

  private MessagingConfiguration() {
    // Utility class.
  }
}
