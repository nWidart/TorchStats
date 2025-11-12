package com.nwidart.loganalyzer;

import com.nwidart.loganalyzer.model.Item;
import com.nwidart.loganalyzer.model.Map;
import com.nwidart.loganalyzer.model.MapRepository;
import com.nwidart.fulltable.FullTableService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class StatsServiceQuantityTest {

  @PersistenceContext
  private EntityManager em;

  @Autowired
  private MapRepository mapRepository;

  @Autowired
  private StatsService statsService;

  @MockitoBean
  private FullTableService fullTableService;

  @Test
  void currentMapRevenue_includes_quantity_multiplier() {
    // Given
    Item a = Item.of("p1", "s1", "A", 3, 3);
    Item b = Item.of("p1", "s2", "B", 2, 2);
    em.persist(a);
    em.persist(b);
    Map map = Map.newMap();
    map.addItem(a);
    map.addItem(b);
    mapRepository.saveAndFlush(map);

    when(fullTableService.getPriceForItem(argThat(i -> i != null && "A".equals(i.getConfigBaseId()))))
        .thenReturn(10f);
    when(fullTableService.getPriceForItem(argThat(i -> i != null && "B".equals(i.getConfigBaseId()))))
        .thenReturn(5f);

    // When
    Float revenue = statsService.currentMapRevenue();

    // Then (10 * 3) + (5 * 2) = 40
    assertThat(revenue).isEqualTo(40.0f);
  }

  @Test
  void sessionRevenue_sums_price_times_quantity_across_all_maps() {
    // Given
    Item a = Item.of("p1", "s1", "A", 1, 1);
    Item b = Item.of("p1", "s2", "B", 4, 4);
    em.persist(a);
    em.persist(b);

    Map map1 = Map.newMap();
    map1.addItem(a);
    mapRepository.save(map1);

    Map map2 = Map.newMap();
    map2.addItem(b);
    mapRepository.saveAndFlush(map2);

    when(fullTableService.getPriceForItem(argThat(i -> i != null && "A".equals(i.getConfigBaseId()))))
        .thenReturn(10f);
    when(fullTableService.getPriceForItem(argThat(i -> i != null && "B".equals(i.getConfigBaseId()))))
        .thenReturn(5f);

    // When
    Float revenue = statsService.getSessionRevenue();

    // Then 10*1 + 5*4 = 30
    assertThat(revenue).isEqualTo(30.0f);
  }

  @Test
  void currentMapRevenue_zero_when_no_active_map() {
    // Given: create and end a map so no active map exists
    Map map = Map.newMap();
    map.endMap();
    mapRepository.saveAndFlush(map);

    // When
    Float revenue = statsService.currentMapRevenue();

    // Then
    assertThat(revenue).isEqualTo(0.0f);
  }

  @Test
  void items_with_zero_quantity_do_not_increase_revenue() {
    // Given
    Item a = Item.of("p1", "s1", "A", 0, 0);
    em.persist(a);
    Map map = Map.newMap();
    map.addItem(a);
    mapRepository.saveAndFlush(map);

    when(fullTableService.getPriceForItem(argThat(i -> i != null && "A".equals(i.getConfigBaseId()))))
        .thenReturn(123f);

    // When
    Float revenue = statsService.currentMapRevenue();

    // Then 123 * 0 = 0
    assertThat(revenue).isEqualTo(0.0f);
  }
}
