package com.nwidart.loganalyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

@Embeddable
@Immutable
public class ItemId implements Serializable {

  @Column(name = "config_base_id")
  private String configBaseId;

  @Column(name = "page_id")
  private String pageId;

  @Column(name = "slot_id")
  private String slotId;

  protected ItemId() {
  }

  public ItemId(String configBaseId, String pageId, String slotId) {
    this.configBaseId = configBaseId;
    this.pageId = pageId;
    this.slotId = slotId;
  }

  public static ItemId of(String configBaseId, String pageId, String slotId) {
    return new ItemId(configBaseId, pageId, slotId);
  }

  public String getConfigBaseId() {
    return configBaseId;
  }

  public String getPageId() {
    return pageId;
  }

  public String getSlotId() {
    return slotId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ItemId itemId = (ItemId) o;
    return Objects.equals(configBaseId, itemId.configBaseId) &&
        Objects.equals(pageId, itemId.pageId) &&
        Objects.equals(slotId, itemId.slotId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configBaseId, pageId, slotId);
  }
}
