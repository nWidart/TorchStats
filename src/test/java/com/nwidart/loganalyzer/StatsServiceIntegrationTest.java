package com.nwidart.loganalyzer;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class StatsServiceIntegrationTest extends AbstractResourceTest {

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private PlatformTransactionManager txManager;

  @Autowired
  private LogService logService;

  @Autowired
  private StatsService statsService;

  @Test
  void statServiceGeneratesProperData() throws Exception {
    Path logFile = resourcePath("logs/multi_map_scenario.log");
    long expectedLines = countLines(logFile);

    // When
    logService.startTailing(logFile);

    // Wait until all lines have been processed
    awaitUntil(() -> logService.getCurrentLineNumber() >= expectedLines, Duration.ofSeconds(10));
    sleep(250);

    // Then: check the stats service
    assertThat(statsService.mapsCompleted()).isEqualTo(2);
    assertThat(statsService.getSessionRevenue()).isEqualTo(108.5f);
  }

  @BeforeEach
  void cleanDbBefore() {
    // Safety: ensure no previous tailer is running
    if (logService.isRunning()) {
      logService.stopTailing();
    }

    // Ensure a clean state for each test (wrap in a transaction)
    new TransactionTemplate(txManager)
        .execute(status -> {
          entityManager.createNativeQuery("delete from map_item").executeUpdate();
          entityManager.createQuery("delete from Item").executeUpdate();
          entityManager.createQuery("delete from Map").executeUpdate();
          entityManager.flush();
          return null;
        });
  }

  @AfterEach
  void stopTailer() {
    if (logService.isRunning()) {
      logService.stopTailing();
    }
  }
}
