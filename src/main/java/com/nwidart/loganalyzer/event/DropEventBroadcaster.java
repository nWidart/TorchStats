package com.nwidart.loganalyzer.event;

import com.vaadin.flow.shared.Registration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * A lightweight in-memory broadcaster for item drop events that can be used
 * by Vaadin UIs to receive server-pushed updates. It also keeps a bounded
 * backlog so newly opened views can show recent events instantly.
 */
@Component
public class DropEventBroadcaster {

  private static final int DEFAULT_BACKLOG_SIZE = 200;

  private final CopyOnWriteArraySet<Consumer<DropEvent>> listeners = new CopyOnWriteArraySet<>();
  private final Deque<DropEvent> backlog = new ArrayDeque<>();
  private final int maxBacklogSize;

  public DropEventBroadcaster() {
    this(DEFAULT_BACKLOG_SIZE);
  }

  public DropEventBroadcaster(int maxBacklogSize) {
    this.maxBacklogSize = Math.max(1, maxBacklogSize);
  }

  /** Register a listener. Returns a {@link Registration} to unsubscribe. */
  public Registration register(Consumer<DropEvent> listener) {
    Objects.requireNonNull(listener, "listener");
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  /** Returns a snapshot copy of the current backlog (oldest first). */
  public List<DropEvent> getBacklog() {
    synchronized (backlog) {
      return new ArrayList<>(backlog);
    }
  }

  /** Broadcasts an event to all listeners and stores it in the backlog. */
  public void broadcast(DropEvent event) {
    if (event == null) return;
    synchronized (backlog) {
      backlog.addLast(event);
      while (backlog.size() > maxBacklogSize) {
        backlog.removeFirst();
      }
    }
    for (var l : listeners) {
      try {
        l.accept(event);
      } catch (RuntimeException ignored) {
        // isolate listener exceptions
      }
    }
  }

  /**
   * View model for the UI, derived from domain event.
   */
  public record DropEvent(
      Instant time,
      String configBaseId,
      int delta,
      int total,
      double price,
      Instant mapStartedAt
  ) { }
}
