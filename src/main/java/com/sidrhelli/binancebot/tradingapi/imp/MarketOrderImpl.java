package com.sidrhelli.binancebot.tradingapi.imp;

import java.math.BigDecimal;
import com.sidrhelli.binancebot.tradingapi.MarketOrder;
import com.sidrhelli.binancebot.tradingapi.OrderType;


public final class MarketOrderImpl implements MarketOrder {

  private OrderType type;
  private BigDecimal price;
  private BigDecimal quantity;
  private BigDecimal total;

  /** Creates a new Market Order. */
  public MarketOrderImpl(OrderType type, BigDecimal price, BigDecimal quantity, BigDecimal total) {
    this.type = type;
    this.price = price;
    this.quantity = quantity;
    this.total = total;
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

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  @Override
  public String toString() {
    return "MarketOrderImpl [price=" + price + ", quantity=" + quantity + ", total=" + total + "]";
  }


}

