package com.sidrhelli.binancebot.dao.entitities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;

@Entity
@Table(name = "Orders")
public class NewBinanceOrder {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String clientOrderId;
  private String symbol;
  private Long orderId;
  private Long transactTime;
  private String price;
  private String origQty;
  private String executedQty;
  private String cummulativeQuoteQty;
  private OrderStatus status;
  private TimeInForce timeInForce;
  private OrderType type;
  private OrderSide side;

  public NewBinanceOrder() {

  }


  public NewBinanceOrder(String symbol) {
    this.symbol = symbol;
  }

  public NewBinanceOrder(String clientOrderId, String symbol, Long orderId, Long transactTime,
      String price, String origQty, String executedQty, String cummulativeQuoteQty,
      OrderStatus status, TimeInForce timeInForce, OrderType type, OrderSide side) {
    super();
    this.clientOrderId = clientOrderId;
    this.symbol = symbol;
    this.orderId = orderId;
    this.transactTime = transactTime;
    this.price = price;
    this.origQty = origQty;
    this.executedQty = executedQty;
    this.cummulativeQuoteQty = cummulativeQuoteQty;
    this.status = status;
    this.timeInForce = timeInForce;
    this.type = type;
    this.side = side;
  }



  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }


  public String getClientOrderId() {
    return clientOrderId;
  }

  public void setClientOrderId(String clientOrderId) {
    this.clientOrderId = clientOrderId;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }


  public Long getTransactTime() {
    return transactTime;
  }

  public void setTransactTime(Long transactTime) {
    this.transactTime = transactTime;
  }

  public String getPrice() {
    return price;
  }

  public void setPrice(String price) {
    this.price = price;
  }

  public String getOrigQty() {
    return origQty;
  }

  public void setOrigQty(String origQty) {
    this.origQty = origQty;
  }

  public String getExecutedQty() {
    return executedQty;
  }

  public void setExecutedQty(String executedQty) {
    this.executedQty = executedQty;
  }

  public String getCummulativeQuoteQty() {
    return cummulativeQuoteQty;
  }

  public void setCummulativeQuoteQty(String cummulativeQuoteQty) {
    this.cummulativeQuoteQty = cummulativeQuoteQty;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  public TimeInForce getTimeInForce() {
    return timeInForce;
  }

  public void setTimeInForce(TimeInForce timeInForce) {
    this.timeInForce = timeInForce;
  }

  public OrderType getType() {
    return type;
  }

  public void setType(OrderType type) {
    this.type = type;
  }

  public OrderSide getSide() {
    return side;
  }

  public void setSide(OrderSide side) {
    this.side = side;
  }

  @Override
  public String toString() {
    return "NewBinanceOrder [id=" + id + ", clientOrderId=" + clientOrderId + ", symbol=" + symbol
        + ", orderId=" + orderId + "transactTime=" + transactTime + ", price=" + price
        + ", origQty=" + origQty + ", executedQty=" + executedQty + ", cummulativeQuoteQty="
        + cummulativeQuoteQty + ", status=" + status + ", timeInForce=" + timeInForce + ", type="
        + type + ", side=" + side + "]";
  }



}
