package com.nwidart.fulltable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nwidart.loganalyzer.model.Item;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class FullTableService implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(FullTableService.class);

  private final ObjectMapper objectMapper;

  private volatile Map<String, FullTableItem> itemsById = Collections.emptyMap();

  public FullTableService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void run(ApplicationArguments args) {
    load();
  }

  /**
   * Loads the JSON from classpath into memory. Throws a runtime exception if loading fails, preventing the app from starting silently with empty data.
   */
  public synchronized void load() {
    ClassPathResource resource = new ClassPathResource("full_table.json");
    if (!resource.exists()) {
      throw new IllegalStateException("Resource 'full_table.json' not found on classpath");
    }
    try (InputStream is = resource.getInputStream()) {
      Map<String, FullTableItem> parsed = objectMapper.readValue(is, new TypeReference<Map<String, FullTableItem>>() {
      });
      this.itemsById = Collections.unmodifiableMap(parsed);
      log.info("Loaded full_table.json with {} entries", parsed.size());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load/parse full_table.json", e);
    }
  }

  public Optional<FullTableItem> findById(String id) {
    return Optional.ofNullable(itemsById.get(id));
  }

  public Map<String, FullTableItem> all() {
    return itemsById;
  }

  public int size() {
    return itemsById.size();
  }

  public Float getPriceForItem(Item item) {
    if (item.getConfigBaseId() == null) {
      return 0f;
    }

    return findById(item.getConfigBaseId())
        .map(FullTableItem::price)
        .map(Double::floatValue)
        .orElse(0f);
  }
}
