package com.nwidart.loganalyzer.model;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MapRepositoryTest {

  @Autowired
  private MapRepository mapRepository;

  @Test
  void mapRepository_can_find_active_map() {
    Map map = Map.newMap();
    mapRepository.save(map);
    map.endMap();
    mapRepository.save(map);
    Map map2 = Map.newMap();
    mapRepository.save(map2);

    Map found = mapRepository.findActiveMap();
    assertThat(found).isNotNull();
    assertThat(found.getEndedAt()).isNull();
  }
}
