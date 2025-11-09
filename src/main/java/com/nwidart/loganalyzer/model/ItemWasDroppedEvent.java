package com.nwidart.loganalyzer.model;

import org.springframework.context.ApplicationEvent;

public class ItemWasDroppedEvent extends ApplicationEvent {

  private final Item item;
  private final int countDelta;

  public ItemWasDroppedEvent(Object source, Item item, int countDelta) {
    super(source);
    this.item = item;
    this.countDelta = countDelta;
  }

  public Item getItem() {
    return item;
  }

  public int getCountDelta() {
    return countDelta;
  }
}
