package com.nwidart.loganalyzer.processor;

import com.nwidart.loganalyzer.model.ItemRepository;
import com.nwidart.loganalyzer.model.Item;
import com.nwidart.loganalyzer.model.LogEntry;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

//@Component
public class BagInitLogProcessor implements LogProcessor {

  private final ItemRepository itemRepository;

  private static final Logger log = LoggerFactory.getLogger(BagInitLogProcessor.class);

  private static final String PREFIX = "\\[.*?\\]GameLog: Display: \\[Game\\]\\s*";
  private static final String PATTERN =
      PREFIX + "BagMgr@:InitBagData\\s+PageId = (?<pageId>\\d+)\\s+SlotId = (?<slotId>\\d+)\\s+ConfigBaseId = (?<configBaseId>\\d+)\\s+Num = (?<num>\\d+)";

  public BagInitLogProcessor(ItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  @Override
  public void process(LogEntry logEntry, Matcher matcher) {
    if (matcher == null) {
      return;
    }
    assert matcher.group("pageId") != null;
    assert matcher.group("slotId") != null;
    assert matcher.group("configBaseId") != null;
    assert matcher.group("num") != null;

    log.info("Matched error log at line {}: {}", logEntry.lineNumber(), logEntry.rawLine());
    log.info("PageId = {}", matcher.group("pageId"));
    log.info("SlotId = {}", matcher.group("slotId"));
    log.info("ConfigBaseId = {}", matcher.group("configBaseId"));
    log.info("Num = {}", matcher.group("num"));

    var item = Item.of(
        matcher.group("pageId"),
        matcher.group("slotId"),
        matcher.group("configBaseId"),
        Integer.valueOf(matcher.group("num")),
        0);
    this.itemRepository.save(item);
  }

  @Override
  public String getPattern() {
    return PATTERN;
  }

  @Override
  public int getOrder() {
    return 10; // Higher priority
  }
}
