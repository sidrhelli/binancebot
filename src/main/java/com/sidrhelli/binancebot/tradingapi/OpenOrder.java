package com.sidrhelli.binancebot.tradingapi;

import java.math.BigDecimal;
import java.util.Date;


/**
 * Represents an Open Order (active order) on the exchange.
 *
 * @author gazbert
 * @since 1.0
 */
public interface OpenOrder {

  /**
   * Returns the ID for this order.
   *
   * @return the ID of the order.
   */
  String getId();

  /**
   * Returns the exchange date/time the order was created.
   *
   * @return The exchange date/time.
   */
  Date getCreationDate();

  /**
   * Returns the id of the market this order was placed on.
   *
   * @return the id of the market.
   */
  String getMarketId();

  /**
   * Returns the type of order. Value will be {@link OrderType#BUY} or {@link OrderType#SELL}.
   *
   * @return the type of order.
   */
  OrderType getType();

  /**
   * Returns the price per unit for this order. This is usually in BTC or USD.
   *
   * @return the price per unit for this order.
   */
  BigDecimal getPrice();

  /**
   * Returns the Quantity remaining for this order. This is usually the amount of the other currency
   * you want to trade for BTC/USD.
   *
   * @return the Quantity remaining for this order.
   */
  BigDecimal getQuantity();

  /**
   * Returns the Original total order quantity. If the Exchange does not provide this information,
   * the value will be null. This is usually the amount of the other currency you want to trade for
   * BTC/USD.
   *
   * @return the Original total order quantity if the Exchange provides this information, null
   *     otherwise.
   */
  BigDecimal getOriginalQuantity();

  /**
   * Returns the Total value of order (price * quantity). This is usually in BTC or USD.
   *
   * @return the Total value of order (price * quantity).
   */
  BigDecimal getTotal();
}

