package com.nwidart.loganalyzer.model;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapRepository extends JpaRepository<Map, Long> {

  @EntityGraph(attributePaths = "items")
  Map findFirstByEndedAtIsNullOrderByStartedAtDesc();

  default Map findActiveMap() {
    return findFirstByEndedAtIsNullOrderByStartedAtDesc();
  }
}
