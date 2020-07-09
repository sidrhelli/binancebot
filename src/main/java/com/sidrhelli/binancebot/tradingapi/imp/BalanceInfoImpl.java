package com.sidrhelli.binancebot.tradingapi.imp;

import java.math.BigDecimal;
import java.util.Map;
import com.sidrhelli.binancebot.tradingapi.BalanceInfo;

public final class BalanceInfoImpl implements BalanceInfo {

  private Map<String, BigDecimal> balancesAvailable;
  private Map<String, BigDecimal> balancesOnHold;

  public BalanceInfoImpl(Map<String, BigDecimal> balancesAvailable,
      Map<String, BigDecimal> balancesOnHold) {
    this.balancesAvailable = balancesAvailable;
    this.balancesOnHold = balancesOnHold;
  }

  public Map<String, BigDecimal> getBalancesAvailable() {
    return balancesAvailable;
  }

  public void setBalancesAvailable(Map<String, BigDecimal> balancesAvailable) {
    this.balancesAvailable = balancesAvailable;
  }

  public Map<String, BigDecimal> getBalancesOnHold() {
    return balancesOnHold;
  }

  public void setBalancesOnHold(Map<String, BigDecimal> balancesOnHold) {
    this.balancesOnHold = balancesOnHold;
  }

  @Override
  public String toString() {
    return "BalanceInfoImpl [balancesAvailable=" + balancesAvailable + ", balancesOnHold="
        + balancesOnHold + "]";
  }


}
