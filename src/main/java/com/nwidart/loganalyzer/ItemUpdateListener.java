package com.nwidart.loganalyzer;

import com.nwidart.fulltable.FullTableService;
import com.nwidart.loganalyzer.event.DropEventBroadcaster;
import com.nwidart.loganalyzer.model.Item;
import com.nwidart.loganalyzer.model.ItemWasDroppedEvent;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ItemUpdateListener {

  private static final Logger log = LoggerFactory.getLogger(ItemUpdateListener.class);
  private final DropEventBroadcaster broadcaster;
  private final FullTableService fullTableService;

  public ItemUpdateListener(DropEventBroadcaster broadcaster, FullTableService fullTableService) {
    this.broadcaster = broadcaster;
    this.fullTableService = fullTableService;
  }

  @EventListener
  public void onItemUpdate(ItemWasDroppedEvent event) {
    Item item = event.getItem();

    log.info("Item updated: {} (delta: {}, total: {})", item.getConfigBaseId(), item.getNum(), item.getTotal());

    var map = event.getMap();
    var dto = new DropEventBroadcaster.DropEvent(
        Instant.now(),
        this.fullTableService.getNameForItem(item) + " (" + item.getConfigBaseId() + ")",
        item.getNum() == null ? 0 : item.getNum(),
        item.getTotal() == null ? 0 : item.getTotal(),
        this.fullTableService.getPriceForItem(item),
        map == null ? null : map.getStartedAt()
    );

    broadcaster.broadcast(dto);
  }
}
