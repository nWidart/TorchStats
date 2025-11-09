package com.nwidart.loganalyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.proxy.HibernateProxy;

@Entity
@Table(name = "item")
public class Item {

  @Id
  @Column(name = "config_base_id")
  private String configBaseId;
  @Column(name = "page_id", nullable = false)
  private String pageId;
  @Column(name = "slot_id", nullable = false)
  private String slotId;
  @Column(name = "num", nullable = false)
  private Integer num;

  @ManyToMany(mappedBy = "items")
  private Set<Map> maps = new LinkedHashSet<>();

  protected Item() {
  }

  private Item(String pageId, String slotId, String configBaseId, Integer num) {
    this.pageId = pageId;
    this.slotId = slotId;
    this.configBaseId = configBaseId;
    this.num = num;
  }

  public static Item of(String pageId, String slotId, String configBaseId, Integer num) {
    return new Item(pageId, slotId, configBaseId, num);
  }

  public String getPageId() {
    return pageId;
  }

  public void setPageId(String pageId) {
    this.pageId = pageId;
  }

  public String getSlotId() {
    return slotId;
  }

  public void setSlotId(String slotId) {
    this.slotId = slotId;
  }

  public String getConfigBaseId() {
    return configBaseId;
  }

  public void setConfigBaseId(String configBaseId) {
    this.configBaseId = configBaseId;
  }

  public Integer getNum() {
    return num;
  }

  public void setNum(Integer num) {
    this.num = num;
  }

  public Set<Map> getMaps() {
    return maps;
  }

  public void setMaps(Set<Map> maps) {
    this.maps = maps;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    Class<?> oEffectiveClass =
        o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
    Class<?> thisEffectiveClass =
        this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) {
      return false;
    }
    Item item = (Item) o;
    return getConfigBaseId() != null && Objects.equals(getConfigBaseId(), item.getConfigBaseId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
  }
}
