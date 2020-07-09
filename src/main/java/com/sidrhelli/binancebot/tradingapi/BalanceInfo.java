package com.sidrhelli.binancebot.tradingapi;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Encapsulates wallet balance info held on the exchange.
 *
 * @author gazbert
 * @since 1.0
 */
public interface BalanceInfo {

  /**
   * Returns map of available balances.
   *
   * <p>The key is the currency id in UPPERCASE, e.g. LTC, BTC, USD
   *
   * @return map of available balances.
   */
  Map<String, BigDecimal> getBalancesAvailable();

  /**
   * Returns map of balances on hold.
   *
   * <p>Some exchanges do not provide this information and the returned map will be empty.
   *
   * <p>The key is the currency id in UPPERCASE, e.g. LTC, BTC, USD
   *
   * @return map of balances on hold.
   */
  Map<String, BigDecimal> getBalancesOnHold();
}
