package com.nwidart.loganalyzer.model;

import java.util.ArrayList;
import java.util.List;

public class Bag {

  private List<Item> items = new ArrayList<>();

  public void addItem(String pageId, String slotId, String configBaseId, Integer num) {
    var item = Item.of(pageId, slotId, configBaseId, num);

    this.items.add(item);
  }
}
