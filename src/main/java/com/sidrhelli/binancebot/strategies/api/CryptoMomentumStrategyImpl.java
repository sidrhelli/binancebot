package com.sidrhelli.binancebot.strategies.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.CMOIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.StopGainRule;
import org.ta4j.core.trading.rules.StopLossRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.market.Candlestick;
import com.sidrhelli.binancebot.apiservice.ApiService;
import com.sidrhelli.binancebot.config.service.ConfigService;
import com.sidrhelli.binancebot.config.service.domain.objects.MarketConfig;
import com.sidrhelli.binancebot.exceptions.StrategyException;
import com.sidrhelli.binancebot.exceptions.TradingApiException;


@Component
public class CryptoMomentumStrategyImpl implements TradingStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(CryptoMomentumStrategyImpl.class);
  // TODO: put buy amount in configuration file
  private static final String AMOUNT_OF_OUNTER_CURRENCY_TO_BUY = "0.00015303";
  /** The decimal format for the logs. */
  private static final String DECIMAL_FORMAT = "#.########";
  private static final Num ZERO_DOT_ZERO_FIVE = PrecisionNum.valueOf(0.05);
  // TODO: add interval to configuration file
  private static final long DURATION_OF_BAR = 1L;
  private ApiService apiService;
  @Autowired
  private ConfigService configService;
  private MarketConfig marketConfig;
  private BigDecimal counterCurrencyBuyOrderAmount;
  private BarSeries series;
  private Order lastOrder;
  /* e.g. BNB/BTC. This is used to print out to the log file and the console */
  private String marketName;
  private String marketSymbol;
  private Strategy strategy;
  private TradingRecord tradingRecord;
  private int endIndex;
  private BigDecimal amountOfBaseCurrencyToBuy;



  public String getStrategyName() {
    return "Crypto momentum strategy";
  }

  @Override
  public void execute() throws StrategyException {

    LOG.info(marketName + " Checking order status..");

    final BigDecimal bestBidPrice = apiService.getBestBid().getKey();
    final BigDecimal bestAskPrice = apiService.getBestAsk().getKey();

    /*
     * Is this the first time the Strategy has been called? If yes, we initialise the OrderState so
     * we can keep track of orders during later trace cycles.
     */
    if (lastOrder == null) {
      LOG.debug(
          marketName + " First time Strategy has been called - creating new empty Order object.");
      lastOrder = new Order();
    }

    LOG.info("AccountDetails: <<< Free amount of BTC on account to trade with: "
        + apiService.getAccountBalanceCache().get("BTC").getFree() + " >>> \n");
    LOG.info("Best BID price=" + new DecimalFormat(DECIMAL_FORMAT).format(bestBidPrice));
    LOG.info("Best ASK price=" + new DecimalFormat(DECIMAL_FORMAT).format(bestAskPrice));


    // Execute the appropriate algorithm based on the last order type.
    if (lastOrder.getSide() == OrderSide.BUY) {
      executeAlgoForWhenLastOrderWasBuy(bestBidPrice);

    } else if (lastOrder.getSide() == OrderSide.SELL) {
      executeAlgoForWhenLastOrderWasSell(bestBidPrice, bestAskPrice);

    } else if (lastOrder.getSide() == null) {
      executeAlgoForWhenLastOrderWasNone(bestBidPrice);
    }

  }

  @Override
  public void initStrategy(ApiService apiService, MarketConfig marketConfig) {
    System.out.println("Initialising Strategy..");
    this.apiService = apiService;
    this.marketConfig = marketConfig;
    this.marketName = configService.getMarketConfig().getName();
    this.counterCurrencyBuyOrderAmount = new BigDecimal(AMOUNT_OF_OUNTER_CURRENCY_TO_BUY);
    this.marketSymbol = configService.getMarketConfig().getId();
    apiService.initializeCandlestickCache(marketSymbol);
    apiService.startCandlestickEventStreaming(marketSymbol);
    this.series = setupBarsSeries();
    this.strategy = buildStrategy(series);
    this.tradingRecord = new BaseTradingRecord();
  }

  /*
   * Builds the buy and sell rules for this strategy
   * 
   * @parameter Barseries series
   * 
   * @return Strategy
   */
  public Strategy buildStrategy(BarSeries series) {

    ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
    CMOIndicator cmo = new CMOIndicator(closePrice, 9);

    // EMA
    EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
    EMAIndicator longEma = new EMAIndicator(closePrice, 26);

    // Stochastic
    StochasticOscillatorKIndicator stochasticOscillK =
        new StochasticOscillatorKIndicator(series, 14);

    // MACD
    MACDIndicator macd = new MACDIndicator(closePrice);
    EMAIndicator emaMacd = new EMAIndicator(macd, 18);

    // SMA
    SMAIndicator shortSma = new SMAIndicator(closePrice, 41);
    SMAIndicator longSma = new SMAIndicator(closePrice, 14);

    // RSI
    RSIIndicator rsi = new RSIIndicator(closePrice, 2);

    // Check if trend is going up
    Rule momentumEntry = new OverIndicatorRule(shortSma, longSma)
        .and(new CrossedDownIndicatorRule(cmo, PrecisionNum.valueOf(0)))
        .and(new OverIndicatorRule(shortEma, closePrice));

    // Buy when the trend is up and shorts SMA crosses long SMA or when the price dropped with a
    // certain percentage and MACD is going up. And stochasticOscillator crossed down the value of
    // 20 and short EMA is already over long EMA
    Rule buy =
        new CrossedUpIndicatorRule(shortSma, longSma).or(new CrossedDownIndicatorRule(closePrice,
            getClosePriceMinPercentage(closePrice, series, ZERO_DOT_ZERO_FIVE))
                .and(new OverIndicatorRule(macd, emaMacd))
                .and(new CrossedDownIndicatorRule(stochasticOscillK, PrecisionNum.valueOf(20)))
                .and(new OverIndicatorRule(shortEma, longEma)).and(momentumEntry));

    // Check if trend is goin down
    Rule momentumExit = new UnderIndicatorRule(shortSma, longSma)
        .and(new CrossedUpIndicatorRule(cmo, PrecisionNum.valueOf(0)))
        .and(new UnderIndicatorRule(shortSma, closePrice));

    // Sell if short EMA is going under long EMA, Signal 1. And check if MACD is going
    // down or when the loss is 3% or when a profit of 2% is made.
    Rule sell = new UnderIndicatorRule(shortEma, longEma)
        // Signal 1
        .and(new CrossedUpIndicatorRule(stochasticOscillK, PrecisionNum.valueOf(80)))
        // Signal 2
        .and(new UnderIndicatorRule(macd, emaMacd)).or(momentumExit)
        // Protect against severe losses
        .or(new StopLossRule(closePrice, PrecisionNum.valueOf(3.0)))
        // Take profits and run
        .or(new StopGainRule(closePrice, PrecisionNum.valueOf(2.0))
            .and(new UnderIndicatorRule(shortSma, closePrice))
            .or(new CrossedUpIndicatorRule(rsi, 95)));

    return new BaseStrategy("Crypto Momentum Strategy ", buy, sell);
  }


  /**
   * Algo for executing when the Trading Strategy is invoked for the first time. We start off with a
   * buy order at current BID price.
   *
   * @param currentBidPrice the current market BID price.
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *         Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   */
  private void executeAlgoForWhenLastOrderWasNone(BigDecimal currentBidPrice)
      throws StrategyException {

    try {

      if (strategy.shouldEnter(endIndex)) {

        LOG.info(getStrategyName() + " Sending initial BUY order to exchange --->");

        LOG.info(marketName + " OrderType is NONE - looking for new BUY order at ["
            + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice) + "]");

        amountOfBaseCurrencyToBuy = getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
            counterCurrencyBuyOrderAmount);

        // Add entry to trading Record
        // TODO: persist
        boolean entered = tradingRecord.enter(endIndex, PrecisionNum.valueOf(currentBidPrice),
            PrecisionNum.valueOf(amountOfBaseCurrencyToBuy));
        if (entered) {
          LOG.info(getStrategyName() + " should ENTER on " + endIndex);

          NewOrderResponse newOrder = apiService.createRealMarketBuyOrder(marketSymbol,
              amountOfBaseCurrencyToBuy.toPlainString());

          // Update last order details
          lastOrder.setOrderId(newOrder.getOrderId());
          LOG.info(getStrategyName()
              + " <<<<< ***** Yes, we've made a trade! Lets wait and see.. ****** \n Initial BUY Order sent successfully. ID: "
              + lastOrder.getOrderId() + "********>>>>>");
          lastOrder.setPrice(newOrder.getPrice());
          lastOrder.setSide(newOrder.getSide());
          lastOrder.setOrigQty(newOrder.getExecutedQty());
        }


      }


    } catch (Exception e) {
      // Your error handling code could go here...
      // We are just going to re-throw as StrategyException for engine to deal with - it will
      // shutdown the bot.
      LOG.error(

          marketName
              + " Initial order to BUY base currency failed because Exchange threw TradingApi "
              + "exception. Telling Trading Engine to shutdown bot!",
          e);
      throw new StrategyException(e);
    }

  }

  /**
   * Algo for executing when last order we placed on the exchanges was a BUY.
   *
   * <p>
   * If last buy order filled, we try and sell at a profit.
   *
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *         Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   */
  private void executeAlgoForWhenLastOrderWasBuy(BigDecimal currentAskPrice)
      throws StrategyException {
    try {

      // Fetch our current open orders and see if the buy order is still outstanding/open on the
      // exchange
      final List<com.binance.api.client.domain.account.Order> myOrders =
          apiService.getOpenOrders(marketSymbol);
      boolean lastOrderFound = false;
      for (final Order myOrder : myOrders) {

        if (myOrder.getOrderId().equals(lastOrder.getOrderId())) {
          lastOrderFound = true;
          break;
        }
      }
      // If the order is not there, it must have all filled.
      if (!lastOrderFound) {

        LOG.info(

            marketName + " ^^^ Yay!!! Last BUY Order Id [" + lastOrder.getOrderId()
                + "] filled at [" + lastOrder.getPrice() + "]");

        /*
         * The last buy order was filled, so lets see if we can send a new sell order.
         */
        if (strategy.shouldExit(endIndex)) {

          // Most exchanges (if not all) use 8 decimal places.
          // It's usually best to round up the ASK price in your calculations to maximise gains.
          final BigDecimal newAskPrice =
              apiService.getBestAsk().getValue().setScale(8, RoundingMode.HALF_UP);

          boolean exited = tradingRecord.exit(endIndex, PrecisionNum.valueOf(newAskPrice),
              PrecisionNum.valueOf(lastOrder.getOrigQty()));
          if (exited) {

            LOG.info(marketName + " Placing new SELL order at ask price ["
                + new DecimalFormat(DECIMAL_FORMAT).format(newAskPrice) + "]");
            LOG.info(marketName + " Sending new SELL order to exchange --->");

            // Sending the sell order to the exchange
            NewOrderResponse newOrder =
                apiService.createRealMarketSellOrder(marketSymbol, lastOrder.getOrigQty());

            // Update last order details
            lastOrder.setOrderId(newOrder.getOrderId());
            LOG.info(
                marketName + " New SELL Order sent successfully. ID: " + lastOrder.getOrderId());
            lastOrder.setPrice(newAskPrice.toPlainString());
            lastOrder.setSide(OrderSide.SELL);
          }
        }
      } else {
        /*
         * BUY order has not filled yet.
         */
        LOG.info(marketName + " !!! Still have BUY Order " + lastOrder.getOrderId()
            + " waiting to fill at [" + lastOrder.getPrice() + "] - holding last BUY order...");
      }
    } catch (Exception e) {
      LOG.error(marketName + " New order to SELL base currency failed because Exchange threw an "
          + "exception. Telling Trading Engine to shutdown bot! Last Order: " + lastOrder, e);
      throw new StrategyException(e);
    }
  }

  /**
   * Algo for executing when last order we placed on the exchange was a SELL.
   *
   * <p>
   * If last sell order filled, we send a new buy order to the exchange.
   *
   * @param currentBidPrice the current market BID price.
   * @param currentAskPrice the current market ASK price.
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *         Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   */
  private void executeAlgoForWhenLastOrderWasSell(BigDecimal currentBidPrice,
      BigDecimal currentAskPrice) throws StrategyException {
    try {

      final List<com.binance.api.client.domain.account.Order> myOrders =
          apiService.getOpenOrders(marketSymbol);

      boolean lastOrderFound = false;
      for (final Order myOrder : myOrders) {
        if (myOrder.getOrderId().equals(lastOrder.getOrderId())) {
          lastOrderFound = true;
          break;
        }
      }

      if (!lastOrderFound) {
        LOG.info(marketName + " Yes!!! Last SELL Order Id [" + lastOrder.getOrderId()
            + "] filled at [" + lastOrder.getPrice() + "]");

        if (strategy.shouldEnter(endIndex)) {

          boolean entered = tradingRecord.enter(endIndex, PrecisionNum.valueOf(currentBidPrice),
              PrecisionNum.valueOf(amountOfBaseCurrencyToBuy));

          if (entered) {

            final BigDecimal amountOfBaseCurrencyToBuy =
                getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
                    counterCurrencyBuyOrderAmount);

            LOG.info(marketName + " Placing new BUY order at bid price ["
                + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice) + "]");

            LOG.info(marketName + " Sending new BUY order to exchange --->");

            NewOrderResponse newOrder = apiService.createRealMarketBuyOrder(marketSymbol,
                amountOfBaseCurrencyToBuy.toPlainString());

            // Update last order details
            lastOrder.setOrderId(newOrder.getOrderId());
            LOG.info(marketName + " New BUY Order sent successfully. With ID: "
                + lastOrder.getOrderId());
            lastOrder.setPrice(newOrder.getPrice());
            lastOrder.setSide(newOrder.getSide());
            lastOrder.setOrigQty(newOrder.getExecutedQty());
          }
        }
      }
    } catch (Exception e) {
      LOG.error(marketName + " New order to BUY base currency failed because Exchange threw "
          + "exception. Telling Trading Engine to shutdown bot! Last Order: " + lastOrder, e);
      throw new StrategyException(e);
    }
  }



  private BigDecimal getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
      BigDecimal amountOfCounterCurrencyToTrade) throws TradingApiException {

    LOG.info(

        marketName + " Calculating amount of base currency (BTC) to buy for amount of counter "
            + "currency " + new DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
            + " " + marketConfig.getCounterCurrency());

    // Fetch the last trade price
    final BigDecimal lastTradePriceForOneBtc =
        new BigDecimal(apiService.getTickerFromRestClient(marketSymbol).getPrice());
    LOG.info(marketName + " Last trade price for 1 " + marketConfig.getBaseCurrency() + " was: "
        + new DecimalFormat(DECIMAL_FORMAT).format(lastTradePriceForOneBtc) + " "
        + marketConfig.getCounterCurrency());

    final BigDecimal amountOfBaseCurrencyToBuy =
        amountOfCounterCurrencyToTrade.divide(lastTradePriceForOneBtc, 8, RoundingMode.HALF_DOWN);
    /**
     * Some pairs have a minimum trade size. We have to round the base currency to buy to the minum
     * trade size if the base currency size is smaller then the minimum trade size.
     */
    // TODO: fix ^
    if (marketSymbol.equals("bnbbtc")) {
      BigDecimal newAmountOfBaseCurrencyToBuy =
          amountOfBaseCurrencyToBuy.setScale(0, RoundingMode.UP);

      LOG.info(marketName + " Amount of base currency (" + marketConfig.getBaseCurrency()
          + ") to BUY for " + new DecimalFormat(DECIMAL_FORMAT).format(newAmountOfBaseCurrencyToBuy)
          + " " + marketConfig.getCounterCurrency() + " based on last market trade price: "
          + newAmountOfBaseCurrencyToBuy);
      return newAmountOfBaseCurrencyToBuy;

    }

    LOG.info(marketName + " Amount of base currency (" + marketConfig.getBaseCurrency()
        + ") to BUY for " + new DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
        + " " + marketConfig.getCounterCurrency() + " based on last market trade price: "
        + amountOfBaseCurrencyToBuy);

    return amountOfBaseCurrencyToBuy;
  }


  private static Num getClosePriceMinPercentage(ClosePriceIndicator aClosepriceIndicator,
      BarSeries aBarseries, Num multiplier) {
    Num closePrice = aBarseries.getLastBar().getClosePrice();
    Num percentage = closePrice.multipliedBy(multiplier);

    return closePrice.minus(percentage);
  }


  private BarSeries setupBarsSeries() {
    final int maxBarCount = 50;

    series =
        new BaseBarSeriesBuilder().withName(marketName).withNumTypeOf(PrecisionNum.class).build();
    series.setMaximumBarCount(maxBarCount);

    List<Candlestick> candlesticks = apiService.getCandleStickListFromRestClient(maxBarCount);

    for (Candlestick candleStick : candlesticks) {

      Instant instant = Instant.ofEpochMilli(candleStick.getCloseTime());

      ZonedDateTime closeTime = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
      BigDecimal open = new BigDecimal(candleStick.getOpen());
      BigDecimal close = new BigDecimal(candleStick.getClose());
      BigDecimal high = new BigDecimal(candleStick.getHigh());
      BigDecimal low = new BigDecimal(candleStick.getLow());
      BigDecimal volume = new BigDecimal(candleStick.getVolume());

      series.addBar(Duration.ofMinutes(DURATION_OF_BAR), closeTime, open, high, low, close, volume);

    }
    return series;
  }



}
