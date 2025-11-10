package com.nwidart.loganalyzer.model;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MapRepository extends JpaRepository<Map, Long> {

  Map findFirstByEndedAtIsNullOrderByStartedAtDesc();

  @Query("select distinct m from Map m left join fetch m.items order by m.endedAt desc")
  List<Map> fetchMapsWithItemsSortedByEndedAtDesc();

  default Map findActiveMap() {
    return findFirstByEndedAtIsNullOrderByStartedAtDesc();
  }
}
