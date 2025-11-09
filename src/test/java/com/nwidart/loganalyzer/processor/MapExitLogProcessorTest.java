package com.nwidart.loganalyzer.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.nwidart.loganalyzer.model.LogEntry;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MapExitLogProcessorTest {

  @Autowired
  private MapExitLogProcessor processor;

  @Test
  void pattern_matches_known_log_line() {
    Pattern pattern = Pattern.compile(processor.getPattern());
    String line = "[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] "
        + "PageApplyBase@ _UpdateGameEnd: LastSceneName = World'/Game/Art/Maps/01SD/XZ_YuJinZhiXiBiNanSuo200/"
        + "XZ_YuJinZhiXiBiNanSuo200.XZ_YuJinZhiXiBiNanSuo200' NextSceneName = World'/Game/Art/Maps/01SD/XZ_YuJinZhiXiBiNanSuo200/XZ_YuJinZhiXiBiNanSuo200.XZ_YuJinZhiXiBiNanSuo200'";

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
  void process_accepts_matching_log_entry_without_throwing() {
    String line = "[2025.11.04-19.20.47:543][562]GameLog: Display: [Game] NextSceneName = World'/Game/Art/Maps/01SD/XZ_YuJinZhiXiBiNanSuo200/XZ_YuJinZhiXiBiNanSuo200.XZ_YuJinZhiXiBiNanSuo200'";
    LogEntry entry = new LogEntry(line, 777L, Instant.now());

    // no exception expected
    processor.process(entry, null);
  }
}
