package com.nwidart.loganalyzer.event;

import com.nwidart.loganalyzer.model.Item;
import org.springframework.context.ApplicationEvent;

public class BagSlotInitialized extends ApplicationEvent {

  private final Item item;

  public BagSlotInitialized(Object source, Item item) {
    super(source);
    this.item = item;
  }

  public Item getItem() {
    return item;
  }
}
