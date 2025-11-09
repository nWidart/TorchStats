package com.nwidart.loganalyzer;

import com.nwidart.loganalyzer.model.Item;
import com.nwidart.loganalyzer.model.Map;
import com.nwidart.loganalyzer.model.MapRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

  private final MapRepository mapRepository;

  public StatsService(MapRepository mapRepository) {
    this.mapRepository = mapRepository;
  }

  @Transactional
  public Float getSessionRevenue() {
    return this.mapRepository.findAll().stream()
        .flatMap(m -> m.getItems().stream())
        .map(Item::getPrice)
        .reduce(0f, Float::sum);
  }

  public Float currentMapRevenue() {
    Map activeMap = this.mapRepository.findActiveMap();
    if (activeMap == null) {
      return 0f;
    }
    return activeMap.getItems().stream()
        .map(Item::getPrice)
        .reduce(0f, Float::sum);
  }

  public Float mapsCompleted() {
    return (float) this.mapRepository.count();
  }

  public Duration timeInActiveMap() {
    Map activeMap = this.mapRepository.findActiveMap();
    if (activeMap == null) {
      return Duration.ZERO;
    }
    Instant now = Instant.now();

    return Duration.between(activeMap.getStartedAt(), now);
  }
}
