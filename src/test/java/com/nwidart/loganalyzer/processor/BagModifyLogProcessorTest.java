package com.nwidart.loganalyzer.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.nwidart.loganalyzer.model.Item;
import com.nwidart.loganalyzer.model.ItemRepository;
import com.nwidart.loganalyzer.model.ItemWasDroppedEvent;
import com.nwidart.loganalyzer.model.LogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@RecordApplicationEvents
@Transactional
class BagModifyLogProcessorTest {

  @Autowired
  private BagModifyLogProcessor processor;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private ApplicationEvents applicationEvents;
  @PersistenceContext
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    entityManager.createQuery("delete from Map m join m.items i").executeUpdate(); // JPQL doesn't support delete with join
    entityManager.createNativeQuery("delete from map_item").executeUpdate();
    entityManager.createQuery("delete from Item").executeUpdate();
    entityManager.createQuery("delete from Map").executeUpdate();
  }

  @ParameterizedTest
  @MethodSource("bagModifyLines")
  void pattern_matches_and_extracts_captured_groups(String line,
      int expectedPageId,
      int expectedSlotId,
      int expectedConfigBaseId,
      int expectedNum) {
    Pattern pattern = Pattern.compile(processor.getPattern());

    Matcher matcher = pattern.matcher(line);
    assertThat(matcher.find()).as("Pattern should match: %s", line).isTrue();

    assertThat(Integer.parseInt(matcher.group("pageId"))).isEqualTo(expectedPageId);
    assertThat(Integer.parseInt(matcher.group("slotId"))).isEqualTo(expectedSlotId);
    assertThat(Integer.parseInt(matcher.group("configBaseId"))).isEqualTo(expectedConfigBaseId);
    assertThat(Integer.parseInt(matcher.group("num"))).isEqualTo(expectedNum);
  }

  static Stream<Arguments> bagModifyLines() {
    return Stream.of(
        Arguments.of("[2025.11.04-19.23.40:547][  5]GameLog: Display: [Game] BagMgr@:Modfy BagItem PageId = 103 SlotId = 60 ConfigBaseId = 10042 Num = 34", 103, 60, 10042, 34),
        Arguments.of("[2025.11.04-19.23.40:547][  5]GameLog: Display: [Game] BagMgr@:Modfy BagItem PageId = 103 SlotId = 49 ConfigBaseId = 10180 Num = 44", 103, 49, 10180, 44),
        Arguments.of("[2025.11.04-19.23.40:547][  5]GameLog: Display: [Game] BagMgr@:Modfy BagItem PageId = 103 SlotId = 6 ConfigBaseId = 400033 Num = 4", 103, 6, 400033, 4),
        Arguments.of("[2025.11.04-19.23.40:547][  5]GameLog: Display: [Game] BagMgr@:Modfy BagItem PageId = 103 SlotId = 62 ConfigBaseId = 10022 Num = 61", 103, 62, 10022, 61),
        Arguments.of("[2025.11.04-19.23.40:547][  5]GameLog: Display: [Game] BagMgr@:Modfy BagItem PageId = 103 SlotId = 28 ConfigBaseId = 10161 Num = 72", 103, 28, 10161, 72),
        Arguments.of("[2025.11.04-19.23.40:548][  5]GameLog: Display: [Game] BagMgr@:Modfy BagItem PageId = 102 SlotId = 11 ConfigBaseId = 5028 Num = 617", 102, 11, 5028, 617)
    );
  }

  @Test
  void pattern_does_not_match_unrelated_lines() {
    Pattern pattern = Pattern.compile(processor.getPattern());
    String unrelated = "[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] OtherMgr@:SomethingElse PageId = 100";

    Matcher matcher = pattern.matcher(unrelated);
    assertThat(matcher.find()).isFalse();
  }

  @Test
  void get_order_is_expected() {
    assertThat(processor.getOrder()).isEqualTo(20);
  }

  @Test
  void process_updates_existing_item_and_publishes_event_with_delta() {
    // Given an existing item in the repository
    Item existing = Item.of("102", "11", "5028", 600);
    itemRepository.save(existing);

    String line = "[2025.11.04-19.23.40:548][  5]GameLog: Display: [Game] BagMgr@:Modfy BagItem PageId = 102 SlotId = 11 ConfigBaseId = 5028 Num = 617";
    Pattern pattern = Pattern.compile(processor.getPattern());
    Matcher matcher = pattern.matcher(line);
    assertThat(matcher.find()).isTrue();

    LogEntry entry = new LogEntry(line, 42L, Instant.now());

    // When
    processor.process(entry, matcher);

    // Then: repository is updated
    Item updated = itemRepository.findById("5028").orElseThrow();
    assertThat(updated.getNum()).isEqualTo(17);

    // And an ItemUpdateEvent was published with the correct delta
    List<ItemWasDroppedEvent> events = applicationEvents.stream(ItemWasDroppedEvent.class).toList();
    assertThat(events).hasSize(1);
    ItemWasDroppedEvent event = events.getFirst();
    assertThat(event.getItem().getConfigBaseId()).isEqualTo("5028");
    assertThat(event.getItem().getNum()).isEqualTo(17); // 617 - 600
  }

  @Test
  void process_creates_new_item_and_publishes_event_with_delta() {
    String line = "[2025.11.04-19.23.40:548][  5]GameLog: Display: [Game] BagMgr@:Modfy BagItem PageId = 102 SlotId = 11 ConfigBaseId = 5028 Num = 617";
    Pattern pattern = Pattern.compile(processor.getPattern());
    Matcher matcher = pattern.matcher(line);
    assertThat(matcher.find()).isTrue();

    LogEntry entry = new LogEntry(line, 42L, Instant.now());

    // When
    processor.process(entry, matcher);

    // Then: repository is updated
    Item updated = itemRepository.findById("5028").orElseThrow();
    assertThat(updated.getNum()).isEqualTo(617);

    // And an ItemUpdateEvent was published with the correct delta
    List<ItemWasDroppedEvent> events = applicationEvents.stream(ItemWasDroppedEvent.class).toList();
    assertThat(events).hasSize(1);
    ItemWasDroppedEvent event = events.getFirst();
    assertThat(event.getItem().getConfigBaseId()).isEqualTo("5028");
    assertThat(event.getItem().getNum()).isEqualTo(617); // new item, so full count is considered
  }
}
