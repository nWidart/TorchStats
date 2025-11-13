package com.nwidart.loganalyzer.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DropEventBroadcasterTest {

  @Test
  void stores_backlog_and_notifies_listeners() {
    var broadcaster = new DropEventBroadcaster(3);

    AtomicInteger calls = new AtomicInteger();
    var reg = broadcaster.register(e -> calls.incrementAndGet());

    broadcaster.broadcast(new DropEventBroadcaster.DropEvent(Instant.now(), "A", 1, 1, 10.0, Instant.now()));
    broadcaster.broadcast(new DropEventBroadcaster.DropEvent(Instant.now(), "B", 2, 3, 5.0, Instant.now()));

    assertThat(calls.get()).isEqualTo(2);
    assertThat(broadcaster.getBacklog()).hasSize(2);

    // Exceed capacity to test eviction
    broadcaster.broadcast(new DropEventBroadcaster.DropEvent(Instant.now(), "C", 1, 4, 1.0, Instant.now()));
    broadcaster.broadcast(new DropEventBroadcaster.DropEvent(Instant.now(), "D", 1, 5, 1.0, Instant.now()));

    assertThat(broadcaster.getBacklog()).hasSize(3);
    assertThat(broadcaster.getBacklog().get(0).configBaseId()).isEqualTo("B");

    reg.remove();

    int before = calls.get();
    broadcaster.broadcast(new DropEventBroadcaster.DropEvent(Instant.now(), "E", 1, 6, 1.0, Instant.now()));
    assertThat(calls.get()).isEqualTo(before); // no more notifications
  }
}
