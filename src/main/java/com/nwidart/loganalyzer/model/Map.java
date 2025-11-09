package com.nwidart.loganalyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "map")
public class Map {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @Column(name = "map_id")
  private Long id;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  @Nullable
  private Instant endedAt;

  @ManyToMany
  @JoinTable(
      name = "map_item",
      joinColumns = @JoinColumn(name = "map_id"),
      inverseJoinColumns = @JoinColumn(name = "config_base_id")
  )
  private Set<Item> items = new LinkedHashSet<>();

  protected Map() {
  }

  private Map(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public static Map newMap() {
    return new Map(Instant.now());
  }

  public void endMap() {
    this.endedAt = Instant.now();
  }

  public Set<Item> getItems() {
    return items;
  }

  public void setItems(Set<Item> items) {
    this.items = items;
  }

  public void addItem(Item item) {
    if (this.items.add(item)) {
      item.getMaps().add(this);
    }
  }

  public void removeItem(Item item) {
    if (this.items.remove(item)) {
      item.getMaps().remove(this);
    }
  }

  public @Nullable Instant getEndedAt() {
    return endedAt;
  }
}
