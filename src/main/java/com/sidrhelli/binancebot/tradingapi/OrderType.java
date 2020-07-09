package com.sidrhelli.binancebot.tradingapi;

/**
 * Defines the different order types for sending to the exchange.
 *
 * @author gazbert
 * @since 1.0
 */
public enum OrderType {

  /**
   * Buy order.
   */
  BUY("Buy"),

  /**
   * Sell order.
   */
  SELL("Sell");

  private final String type;

  OrderType(String type) {
    this.type = type;
  }

  public String getStringValue() {
    return type;
  }
}
