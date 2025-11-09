package com.nwidart.loganalyzer.processor;

import com.nwidart.loganalyzer.model.Item;
import com.nwidart.loganalyzer.model.ItemRepository;
import com.nwidart.loganalyzer.model.ItemWasDroppedEvent;
import com.nwidart.loganalyzer.model.LogEntry;
import com.nwidart.loganalyzer.model.Map;
import com.nwidart.loganalyzer.model.MapRepository;
import java.util.regex.Matcher;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BagModifyLogProcessor implements LogProcessor {

  private static final Logger log = LoggerFactory.getLogger(BagModifyLogProcessor.class);
  private final ItemRepository itemRepository;
  private final MapRepository mapRepository;
  private final ApplicationEventPublisher eventPublisher;

  private static final String PREFIX = "\\[.*?\\]GameLog: Display: \\[Game\\]\\s*";
  private static final String PATTERN =
      PREFIX + "BagMgr@:Modfy BagItem PageId = (?<pageId>\\d+) SlotId = (?<slotId>\\d+) ConfigBaseId = (?<configBaseId>\\d+) Num = (?<num>\\d+)";

  public BagModifyLogProcessor(ItemRepository itemRepository, MapRepository mapRepository, ApplicationEventPublisher eventPublisher) {
    this.itemRepository = itemRepository;
    this.mapRepository = mapRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional
  public void process(LogEntry logEntry, @Nullable Matcher matcher) {
    if (matcher == null) {
      return;
    }
    assert matcher.group("pageId") != null;
    assert matcher.group("slotId") != null;
    assert matcher.group("configBaseId") != null;
    assert matcher.group("num") != null;

    var item = this.getOrCreateItem(matcher);
    Integer currentAmount = item.getNum();
    Integer newAmount = Integer.valueOf(matcher.group("num"));
    item.setNum(newAmount);
    var savedItem = this.itemRepository.save(item);

    Map activeMap = this.mapRepository.findActiveMap();
    if (activeMap != null) {
      activeMap.addItem(item);
      this.mapRepository.save(activeMap);
    } else {
      log.debug("No active map found; skipping map association for item {}", savedItem.getConfigBaseId());
    }

    // trigger event to update Map and Session statistics
    this.eventPublisher.publishEvent(new ItemWasDroppedEvent(this, savedItem, newAmount - currentAmount));
  }

  private Item getOrCreateItem(Matcher matcher) {
    var item = this.itemRepository.findByConfigBaseId(matcher.group("configBaseId"));
    if (item != null) {
      return item;
    }
    return Item.of(matcher.group("pageId"), matcher.group("slotId"), matcher.group("configBaseId"), 0);
  }

  @Override
  public String getPattern() {
    return PATTERN;
  }

  @Override
  public int getOrder() {
    return 20;
  }
}
