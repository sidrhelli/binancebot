package com.sidrhelli.binancebot.config.service.domain.objects;



/*
 **
 * Domain object representing a Market config.
 *
 * @author gazbert
 */
public class MarketConfig {

  private String id;
  private String name;
  private String baseCurrency;
  private String counterCurrency;


  // required for Jackson
  public MarketConfig() {}

  /** Creates a MarketConfig from an existing one. */
  public MarketConfig(MarketConfig other) {
    this.id = other.id;
    this.name = other.name;
    this.baseCurrency = other.baseCurrency;
    this.counterCurrency = other.counterCurrency;

  }

  /** Creates a new MarketConfig. */
  public MarketConfig(String id, String name, String baseCurrency, String counterCurrency,
      boolean enabled, String tradingStrategyId) {

    this.id = id;
    this.name = name;
    this.baseCurrency = baseCurrency;
    this.counterCurrency = counterCurrency;

  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBaseCurrency() {
    return baseCurrency;
  }

  public void setBaseCurrency(String baseCurrency) {
    this.baseCurrency = baseCurrency;
  }

  public String getCounterCurrency() {
    return counterCurrency;
  }

  public void setCounterCurrency(String counterCurrency) {
    this.counterCurrency = counterCurrency;
  }

  @Override
  public String toString() {
    return "MarketConfig [id=" + id + ", name=" + name + ", baseCurrency=" + baseCurrency
        + ", counterCurrency=" + counterCurrency + "]";
  }



}
