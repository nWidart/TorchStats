package com.nwidart.loganalyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.proxy.HibernateProxy;

@Entity
@Table(name = "item")
public class Item {

  @EmbeddedId
  private ItemId id;
  @Column(name = "num", nullable = false)
  private Integer num;
  @Column(name = "total", nullable = false)
  private Integer total;

  @ManyToMany(mappedBy = "items")
  private Set<Map> maps = new LinkedHashSet<>();

  protected Item() {
  }

  private Item(String pageId, String slotId, String configBaseId, Integer num, Integer total) {
    this.id = new ItemId(configBaseId, pageId, slotId);
    this.num = num;
    this.total = total;
  }

  public static Item of(String pageId, String slotId, String configBaseId, Integer num, Integer total) {
    return new Item(pageId, slotId, configBaseId, num, total);
  }

  public ItemId getId() {
    return id;
  }

  public void setId(ItemId id) {
    this.id = id;
  }

  public String getConfigBaseId() {
    return id != null ? id.getConfigBaseId() : null;
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
    return getId() != null && Objects.equals(getId(), item.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
  }

  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }
}
