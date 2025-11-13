package com.nwidart.loganalyzer.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.nwidart.loganalyzer.model.ItemId;
import com.nwidart.loganalyzer.model.ItemRepository;
import com.nwidart.loganalyzer.model.LogEntry;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@RecordApplicationEvents
@Transactional
class BagInitLogProcessorTest {

  @Autowired
  ItemRepository itemRepository;
  @Autowired
  private BagInitLogProcessor processor;

  @BeforeEach
  void setUp() {
    // Ensure a clean state per test transaction
    itemRepository.deleteAll();
  }

  @ParameterizedTest
  @MethodSource("bagInitLines")
  void pattern_matches_and_extracts_captured_groups(String line,
      int expectedPageId,
      int expectedSlotId,
      int expectedConfigBaseId,
      int expectedNum) {
    Pattern pattern = Pattern.compile(processor.getPattern());

    Matcher matcher = pattern.matcher(line);
    assertThat(matcher.find()).as("Pattern should match: %s", line).isTrue();

    assertThat(Integer.parseInt(matcher.group(1))).isEqualTo(expectedPageId);
    assertThat(Integer.parseInt(matcher.group(2))).isEqualTo(expectedSlotId);
    assertThat(Integer.parseInt(matcher.group(3))).isEqualTo(expectedConfigBaseId);
    assertThat(Integer.parseInt(matcher.group(4))).isEqualTo(expectedNum);
  }

  static Stream<Arguments> bagInitLines() {
    return Stream.of(
        Arguments.of("[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] BagMgr@:InitBagData PageId = 100 SlotId = 1 ConfigBaseId = 3001 Num = 1", 100, 1, 3001, 1),
        Arguments.of("[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] BagMgr@:InitBagData PageId = 100 SlotId = 5 ConfigBaseId = 261010 Num = 1", 100, 5, 261010, 1),
        Arguments.of("[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] BagMgr@:InitBagData PageId = 100 SlotId = 23 ConfigBaseId = 350007 Num = 2", 100, 23, 350007, 2),
        Arguments.of("[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] BagMgr@:InitBagData PageId = 100 SlotId = 30 ConfigBaseId = 260202 Num = 1", 100, 30, 260202, 1)
    );
  }

  @Test
  void pattern_does_not_match_unrelated_lines() {
    Pattern pattern = Pattern.compile(processor.getPattern());
    String unrelated = "[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] OtherMgr@:InitSomething PageId = 100";

    Matcher matcher = pattern.matcher(unrelated);
    assertThat(matcher.find()).isFalse();
  }

  @Test
  void get_order_is_expected() {
    assertThat(processor.getOrder()).isEqualTo(10);
  }

  @Test
  void process_accepts_matching_log_entry_without_throwing() {
    String line = "[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] BagMgr@:InitBagData PageId = 100 SlotId = 1 ConfigBaseId = 3001 Num = 1";
    LogEntry entry = new LogEntry(line, 1L, Instant.now());

    // No assertions needed; ensure no exception is thrown when processing a matching line.
    processor.process(entry, null);
  }

  @Test
  void process_saves_item() {
    String line = "[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] BagMgr@:InitBagData PageId = 100 SlotId = 1 ConfigBaseId = 3001 Num = 1";
    Pattern pattern = Pattern.compile(processor.getPattern());
    Matcher matcher = pattern.matcher(line);
    assertThat(matcher.find()).isTrue();

    LogEntry entry = new LogEntry(line, 42L, Instant.now());

    // when
    processor.process(entry, matcher);

    // then
    var found = itemRepository.findById(ItemId.of("3001", "100", "1"));
    assertThat(found).isNotNull();
    assertThat(found.getNum()).isEqualTo(1);
  }
}
