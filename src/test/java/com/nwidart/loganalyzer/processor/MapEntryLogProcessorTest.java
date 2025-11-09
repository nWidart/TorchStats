package com.nwidart.loganalyzer.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.nwidart.loganalyzer.model.LogEntry;
import com.nwidart.loganalyzer.model.MapRepository;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MapEntryLogProcessorTest {

  @Autowired
  private MapEntryLogProcessor processor;

  @Autowired
  private MapRepository mapRepository;

  @BeforeEach
  void setUp() {
    mapRepository.deleteAll();
  }

  @Test
  void pattern_matches_known_log_line() {
    Pattern pattern = Pattern.compile(processor.getPattern());
    String line = "[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] "
        + "PageApplyBase@ _UpdateGameEnd: LastSceneName = World'/Game/Art/Maps/01SD/XZ_YuJinZhiXiBiNanSuo200/"
        + "XZ_YuJinZhiXiBiNanSuo200.XZ_YuJinZhiXiBiNanSuo200' NextSceneName = World'/Game/Art/Maps/02SD/SomeOtherMap/SomeOtherMap.SomeOtherMap'";

    Matcher matcher = pattern.matcher(line);
    assertThat(matcher.find()).isTrue();
  }

  @Test
  void pattern_does_not_match_unrelated_lines() {
    Pattern pattern = Pattern.compile(processor.getPattern());
    String unrelated = "[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] SomeOtherEvent happening";

    Matcher matcher = pattern.matcher(unrelated);
    assertThat(matcher.find()).isFalse();
  }

  @Test
  void get_order_is_expected() {
    assertThat(processor.getOrder()).isEqualTo(15);
  }

  @Test
  void process_persists_a_new_map() {
    long before = mapRepository.count();

    String line = "[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] "
        + "PageApplyBase@ _UpdateGameEnd: LastSceneName = World'/Game/Art/Maps/01SD/XZ_YuJinZhiXiBiNanSuo200/"
        + "XZ_YuJinZhiXiBiNanSuo200.XZ_YuJinZhiXiBiNanSuo200' NextSceneName = World'/Game/Art/Maps/02SD/SomeOtherMap/SomeOtherMap.SomeOtherMap'";
    LogEntry entry = new LogEntry(line, 123L, Instant.now());

    // When
    processor.process(entry, null);

    // Then
    assertThat(mapRepository.count()).isEqualTo(before + 1);
  }
}
