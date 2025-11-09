package com.nwidart.loganalyzer.model;

import org.springframework.context.ApplicationEvent;

public class ItemWasDroppedEvent extends ApplicationEvent {

  private final Item item;
  private final Map map;

  public ItemWasDroppedEvent(Object source, Item item, Map map) {
    super(source);
    this.item = item;
    this.map = map;
  }

  public Item getItem() {
    return item;
  }

  public Map getMap() {
    return map;
  }
}
