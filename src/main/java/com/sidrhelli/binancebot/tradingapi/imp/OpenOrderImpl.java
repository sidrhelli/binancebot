package com.sidrhelli.binancebot.tradingapi.imp;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import com.sidrhelli.binancebot.tradingapi.OpenOrder;
import com.sidrhelli.binancebot.tradingapi.OrderType;

public final class OpenOrderImpl implements OpenOrder {

  private String id;
  private Date creationDate;
  private String marketId;
  private OrderType type;
  private BigDecimal price;
  private BigDecimal quantity;
  private BigDecimal originalQuantity;
  private BigDecimal total;

  /** Creates a new Open Order. */
  public OpenOrderImpl(String id, Date creationDate, String marketId, OrderType type,
      BigDecimal price, BigDecimal quantity, BigDecimal originalQuantity, BigDecimal total) {

    this.id = id;
    if (creationDate != null) {
      this.creationDate = new Date(creationDate.getTime());
    }
    this.marketId = marketId;
    this.type = type;
    this.price = price;
    this.quantity = quantity;
    this.originalQuantity = originalQuantity;
    this.total = total;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /** Returns the Order creation date. */
  public Date getCreationDate() {
    if (creationDate != null) {
      return new Date(creationDate.getTime());
    }
    return null;
  }

  void setCreationDate(Date creationDate) {
    if (creationDate != null) {
      this.creationDate = new Date(creationDate.getTime());
    }
  }

  public String getMarketId() {
    return marketId;
  }

  public void setMarketId(String marketId) {
    this.marketId = marketId;
  }

  public OrderType getType() {
    return type;
  }

  public void setType(OrderType type) {
    this.type = type;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getOriginalQuantity() {
    return originalQuantity;
  }

  void setOriginalQuantity(BigDecimal originalQuantity) {
    this.originalQuantity = originalQuantity;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OpenOrderImpl openOrder = (OpenOrderImpl) o;
    return Objects.equals(id, openOrder.id) && Objects.equals(marketId, openOrder.marketId)
        && type == openOrder.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, marketId, type);
  }

  @Override
  public String toString() {
    return "OpenOrderImpl [id=" + id + ", creationDate=" + creationDate + ", marketId=" + marketId
        + ", type=" + type + ", price=" + price + ", quantity=" + quantity + ", originalQuantity="
        + originalQuantity + ", total=" + total + "]";
  }


}
