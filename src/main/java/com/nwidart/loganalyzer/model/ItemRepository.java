package com.nwidart.loganalyzer.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, String> {

  Item findById(ItemId id);

}
