package com.nwidart.loganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import com.nwidart.loganalyzer.model.Item;
import com.nwidart.loganalyzer.model.ItemRepository;
import com.nwidart.loganalyzer.model.Map;
import com.nwidart.loganalyzer.model.MapRepository;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LogServiceIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(LogServiceIntegrationTest.class);

  @Autowired
  private LogService logService;

  @Autowired
  private ItemRepository itemRepository;

  @Autowired
  private MapRepository mapRepository;

  @BeforeEach
  void cleanDbBefore() {
    // Ensure a clean state for each test
    itemRepository.deleteAll();
    mapRepository.deleteAll();

    // Safety: ensure no previous tailer is running
    if (logService.isRunning()) {
      logService.stopTailing();
    }
  }

  @AfterEach
  void stopTailer() {
    if (logService.isRunning()) {
      logService.stopTailing();
    }
  }

  @Test
  @DisplayName("happy path: Multiple BagInit -> Map entry -> Multiple BagMgr@:Modfy -> Map exit")
  void logService_processes_happy_path_log() throws Exception {
    Path logFile = resourcePath("logs/happy_path.log");
    long expectedLines = countLines(logFile);

    // When
    logService.startTailing(logFile);

    // Wait until all lines have been processed
    awaitUntil(() -> logService.getCurrentLineNumber() >= expectedLines, Duration.ofSeconds(5));
    // small settling delay to ensure repository flush in the tailer thread
    sleep(150);

    // Then
    assertThat(mapRepository.count()).as("Map should be created at map entry").isEqualTo(1);

    // Items created or updated
    Item i3001 = itemRepository.findByConfigBaseId("3001");
    Item i261010 = itemRepository.findByConfigBaseId("261010");
    Item i5555 = itemRepository.findByConfigBaseId("5555");

    assertThat(i3001).isNotNull();
    assertThat(i261010).isNotNull();
    assertThat(i5555).isNotNull();

    assertThat(i3001.getNum()).isEqualTo(3); // 1 -> 3
    assertThat(i261010.getNum()).isEqualTo(0); // 1 -> 0
    assertThat(i5555.getNum()).isEqualTo(10); // created by modify

    assertThat(itemRepository.count()).isEqualTo(3);
  }

  @Test
  @DisplayName("multi scenario: Multiple items with successive modifies and map lifecycle")
  void logService_processes_multi_scenario_log() throws Exception {
    Path logFile = resourcePath("logs/multi_scenario.log");
    long expectedLines = countLines(logFile);

    // When
    logService.startTailing(logFile);

    // Wait until all lines have been processed
    awaitUntil(() -> logService.getCurrentLineNumber() >= expectedLines, Duration.ofSeconds(5));
    sleep(150);

    // Then: map created
    assertThat(mapRepository.count()).isEqualTo(1);
    List<Map> maps = mapRepository.findAll(Sort.by(Sort.Direction.DESC, "endedAt"));
    assertThat(maps).hasSize(1);
    var lastMap = maps.getFirst();
    assertThat(lastMap.getEndedAt()).isNotNull();
    assertThat(lastMap.getItems()).hasSize(3);

    // Items and their final counts
    Item i7001 = itemRepository.findByConfigBaseId("7001");
    Item i7002 = itemRepository.findByConfigBaseId("7002");
    Item i8000 = itemRepository.findByConfigBaseId("8000");

    assertThat(i7001).isNotNull();
    assertThat(i7002).isNotNull();
    assertThat(i8000).isNotNull();

    assertThat(i7001.getNum()).isEqualTo(9);  // 5 -> 8 -> 9
    assertThat(i7002.getNum()).isEqualTo(0);  // 1 -> 0
    assertThat(i8000.getNum()).isEqualTo(2);  // created by modify

    assertThat(itemRepository.count()).isEqualTo(3);
  }

  // ---------- helpers ----------

  private Path resourcePath(String classpathLocation) throws IOException {
    ClassPathResource res = new ClassPathResource(classpathLocation);
    if (!res.exists()) {
      throw new IllegalArgumentException("Test resource not found: " + classpathLocation);
    }
    try {
      URL url = res.getURL();
      return Path.of(url.toURI());
    } catch (URISyntaxException e) {
      throw new IOException("Could not resolve resource URI: " + classpathLocation, e);
    }
  }

  private long countLines(Path file) throws IOException {
    try (var lines = Files.lines(file)) {
      return lines.count();
    }
  }

  private void awaitUntil(BooleanSupplier condition, Duration timeout) throws InterruptedException {
    long deadline = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      sleep(25);
    }
    // one last check
    if (!condition.getAsBoolean()) {
      throw new AssertionError("Condition not met within timeout " + timeout);
    }
  }

  @FunctionalInterface
  private interface BooleanSupplier {
    boolean getAsBoolean();
  }

  private void sleep(long millis) throws InterruptedException {
    TimeUnit.MILLISECONDS.sleep(millis);
  }
}
