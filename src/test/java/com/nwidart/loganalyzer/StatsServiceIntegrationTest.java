package com.nwidart.loganalyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.nwidart.fulltable.FullTableItem;
import com.nwidart.fulltable.FullTableService;
import com.nwidart.loganalyzer.model.Item;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

  @MockitoBean
  private FullTableService fullTableService;

  @Test
  void statServiceGeneratesProperData() throws Exception {
    mockItemId("100001", 10f);
    mockItemId("10001", 10f);
    mockItemId("1001", 10f);
    mockItemId("10003", 10f);

    Path logFile = resourcePath("logs/multi_map_scenario.log");
    long expectedLines = countLines(logFile);

    // When
    logService.startTailing(logFile);

    // Wait until all lines have been processed
    awaitUntil(() -> logService.getCurrentLineNumber() >= expectedLines, Duration.ofSeconds(10));
    sleep(250);

    // Then: check the stats service
    assertThat(statsService.mapsCompleted()).isEqualTo(2);
    assertThat(statsService.getSessionRevenue()).isEqualTo(710.0f);
  }

  private void mockItemId(String id, Float price) {
    when(fullTableService.getPriceForItem(argThat(item ->
        item != null && id.equals(item.getConfigBaseId())
    ))).thenReturn(price);
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
