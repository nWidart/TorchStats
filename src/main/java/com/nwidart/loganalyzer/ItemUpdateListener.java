package com.nwidart.loganalyzer;

import com.nwidart.loganalyzer.model.Item;
import com.nwidart.loganalyzer.model.ItemWasDroppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ItemUpdateListener {

  private static final Logger log = LoggerFactory.getLogger(ItemUpdateListener.class);

  @EventListener
  public void onItemUpdate(ItemWasDroppedEvent event) {
    Item item = event.getItem();
    int delta = event.getCountDelta();

    log.info("Item updated: {} by {}", item.getConfigBaseId(), delta);
    // add item to the currently active map
    //
  }
}
