package com.sidrhelli.binancebot.strategies.api.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
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
import com.sidrhelli.binancebot.dao.entitities.NewBinanceOrder;
import com.sidrhelli.binancebot.dao.service.BinanceDaoService;
import com.sidrhelli.binancebot.exceptions.StrategyException;
import com.sidrhelli.binancebot.exceptions.TradingApiException;
import com.sidrhelli.binancebot.strategies.api.TradingStrategy;

@Component
public class CryptoMomentumStrategyImpl implements TradingStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(CryptoMomentumStrategyImpl.class);
  private static final String AMOUNT_OF_OUNTER_CURRENCY_TO_BUY = "0.00015303";
  private static final String DECIMAL_FORMAT = "#.########";
  private static final Num ZERO_DOT_ZERO_THREE = PrecisionNum.valueOf(0.03);
  private static final long DURATION_OF_BAR = 1L;
  private ApiService apiService;
  @Autowired
  private ConfigService configService;
  @Autowired
  private BinanceDaoService binanceDaoService;
  private BarSeries series;
  private Strategy strategy;
  private TradingRecord tradingRecord;
  private MarketConfig marketConfig;
  private BigDecimal counterCurrencyBuyOrderAmount;
  private BigDecimal amountOfBaseCurrencyToBuy;
  private Order lastOrder;
  private String fullMarketName;
  private String marketSymbol;
  private int endIndex;

  public String getStrategyName() {
    return "Crypto momentum strategy";
  }

  @Override
  public void execute() throws StrategyException {

    LOG.info(fullMarketName + " Checking order status..");

    final BigDecimal bestBidPrice = apiService.getBestBid().getKey();
    final BigDecimal bestAskPrice = apiService.getBestAsk().getKey();

    Map<Long, Candlestick> candleSticks = apiService.getCandlesticksCache();
    Long newestCandleStickKey = Collections.max(candleSticks.keySet());
    Candlestick newestCandleStick = candleSticks.get(newestCandleStickKey);

    /*
     * Is this the first time the Strategy has been called? If yes, we initialise the OrderState so
     * we can keep track of orders during later cycles.
     */
    if (lastOrder == null) {
      LOG.debug(fullMarketName
          + " First time Strategy has been called - creating new empty Order object.");
      lastOrder = new Order();
    }

    series.addBar(convertCandleStickToBaseBar(newestCandleStick));
    endIndex = series.getEndIndex();

    LOG.info("<<< Adding new bar [" + newestCandleStick.getOpen() + " - "
        + newestCandleStick.getHigh() + " - " + newestCandleStick.getLow() + " - "
        + newestCandleStick.getClose() + " to series] >>>");

    LOG.info("AccountDetails: <<< Free amount of BTC on account to trade with: "
        + apiService.getAccountBalanceCache().get("BTC").getFree() + " >>>");

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
    this.fullMarketName = configService.getMarketConfig().getName();
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
   * 
   * Buys when the trend is up and the shorts SMA crosses the long SMA or when the price dropped
   * with 5 % and the MACD is going up.Then check if the stochasticOscillator crossed down the value
   * of 20 and short the EMA is already over long the EMA.
   * 
   * 
   * 
   * Sells if short EMA is going under long EMA, Signal 1. And check if MACD is going down or when
   * the loss is 3% or when a profit of 2% is made.
   * 
   * @parameter Barseries series
   * 
   * @return Strategy
   */
  public Strategy buildStrategy(BarSeries series) {

    ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
    CMOIndicator cmo = new CMOIndicator(closePrice, 9);
    EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
    EMAIndicator longEma = new EMAIndicator(closePrice, 26);
    StochasticOscillatorKIndicator stochasticOscillK =
        new StochasticOscillatorKIndicator(series, 14);
    MACDIndicator macd = new MACDIndicator(closePrice);
    EMAIndicator emaMacd = new EMAIndicator(macd, 18);
    SMAIndicator shortSma = new SMAIndicator(closePrice, 41);
    SMAIndicator longSma = new SMAIndicator(closePrice, 14);
    RSIIndicator rsi = new RSIIndicator(closePrice, 2);

    // Trend
    Rule momentumEntry = new OverIndicatorRule(shortSma, longSma)
        .and(new CrossedDownIndicatorRule(cmo, PrecisionNum.valueOf(0)))
        .and(new OverIndicatorRule(shortEma, closePrice));

    Rule buy =
        new CrossedUpIndicatorRule(shortSma, longSma).or(new CrossedDownIndicatorRule(closePrice,
            getClosePriceMinPercentage(closePrice, series, ZERO_DOT_ZERO_THREE))
                .and(new OverIndicatorRule(macd, emaMacd))
                .and(new CrossedDownIndicatorRule(stochasticOscillK, PrecisionNum.valueOf(20)))
                .and(new OverIndicatorRule(shortEma, longEma)).and(momentumEntry));

    Rule momentumExit = new UnderIndicatorRule(shortSma, longSma)
        .and(new CrossedUpIndicatorRule(cmo, PrecisionNum.valueOf(0)))
        .and(new UnderIndicatorRule(shortSma, closePrice));

    Rule sell = new UnderIndicatorRule(shortEma, longEma)
        .and(new CrossedUpIndicatorRule(stochasticOscillK, PrecisionNum.valueOf(80)))
        .and(new UnderIndicatorRule(macd, emaMacd)).or(momentumExit)
        .or(new StopLossRule(closePrice, PrecisionNum.valueOf(3.0)))
        .or(new StopGainRule(closePrice, PrecisionNum.valueOf(2.0))
            .and(new UnderIndicatorRule(shortSma, closePrice))
            .or(new CrossedUpIndicatorRule(rsi, 95)));

    return new BaseStrategy("Crypto Momentum Strategy ", buy, sell);
  }

  /**
   * Algo for executing when the Trading Strategy is invoked for the first time. We start off with a
   * buy order at current BID price.
   */
  private void executeAlgoForWhenLastOrderWasNone(BigDecimal currentBidPrice)
      throws StrategyException {
    LOG.info(fullMarketName + " OrderType is NONE - looking for new BUY order at ["
        + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice) + "]");
    try {

      if (strategy.shouldEnter(endIndex)) {
        LOG.info(getStrategyName() + " Sending initial BUY order to exchange --->");

        amountOfBaseCurrencyToBuy = getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
            counterCurrencyBuyOrderAmount);

        boolean entered = tradingRecord.enter(endIndex, PrecisionNum.valueOf(currentBidPrice),
            PrecisionNum.valueOf(amountOfBaseCurrencyToBuy));
        if (entered) {
          LOG.info(getStrategyName() + " should ENTER on " + endIndex);

          final NewOrderResponse newOrder = apiService.createRealMarketBuyOrder(marketSymbol,
              amountOfBaseCurrencyToBuy.toPlainString());

          final NewBinanceOrder binanceBuyOrder = new NewBinanceOrder(newOrder.getClientOrderId(),
              newOrder.getSymbol(), newOrder.getOrderId(), newOrder.getTransactTime(),
              newOrder.getPrice(), newOrder.getOrigQty(), newOrder.getExecutedQty(),
              newOrder.getCummulativeQuoteQty(), newOrder.getStatus(), newOrder.getTimeInForce(),
              newOrder.getType(), newOrder.getSide());

          // Update last order details
          lastOrder.setOrderId(newOrder.getOrderId());
          lastOrder.setPrice(newOrder.getPrice());
          lastOrder.setSide(newOrder.getSide());
          lastOrder.setOrigQty(newOrder.getExecutedQty());

          LOG.info(getStrategyName()
              + " <<<<< ***** Yes, we've made a trade! Lets wait and see.. ****** \n Initial BUY Order sent successfully. ID: "
              + lastOrder.getOrderId() + "********>>>>>");

          binanceDaoService.save(binanceBuyOrder);
        }
      }

    } catch (Exception e) {
      LOG.error(fullMarketName, e);
      throw new StrategyException(e);
    }

  }

  private void executeAlgoForWhenLastOrderWasBuy(BigDecimal currentAskPrice)
      throws StrategyException {
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
      // If the order is not there, it must have all filled.
      if (!lastOrderFound) {

        LOG.info(

            fullMarketName + " ^^^ Yay!!! Last BUY Order Id [" + lastOrder.getOrderId()
                + "] filled at [" + lastOrder.getPrice() + "] " + "amount :[ "
                + lastOrder.getOrigQty() + " ]");

        /*
         * The last buy order was filled, so lets see if we can send a new sell order.
         */
        if (strategy.shouldExit(endIndex)) {

          final BigDecimal newAskPrice =
              apiService.getBestAsk().getValue().setScale(8, RoundingMode.HALF_UP);

          boolean exited = tradingRecord.exit(endIndex, PrecisionNum.valueOf(newAskPrice),
              PrecisionNum.valueOf(lastOrder.getOrigQty()));

          if (exited) {
            LOG.info(fullMarketName + " Placing new SELL order at ask price ["
                + new DecimalFormat(DECIMAL_FORMAT).format(newAskPrice) + "]");
            LOG.info(fullMarketName + " Sending new SELL order to exchange --->");

            // Sending the sell order to the exchange
            final NewOrderResponse newSellOrder =
                apiService.createRealMarketSellOrder(marketSymbol, lastOrder.getOrigQty());

            NewBinanceOrder binanceSellOrder = new NewBinanceOrder(newSellOrder.getClientOrderId(),
                newSellOrder.getSymbol(), newSellOrder.getOrderId(), newSellOrder.getTransactTime(),
                newSellOrder.getPrice(), newSellOrder.getOrigQty(), newSellOrder.getExecutedQty(),
                newSellOrder.getCummulativeQuoteQty(), newSellOrder.getStatus(),
                newSellOrder.getTimeInForce(), newSellOrder.getType(), newSellOrder.getSide());

            lastOrder.setOrderId(newSellOrder.getOrderId());
            lastOrder.setPrice(newAskPrice.toPlainString());
            lastOrder.setSide(OrderSide.SELL);

            LOG.info(fullMarketName + " New SELL Order sent successfully. ID: "
                + lastOrder.getOrderId());

            binanceDaoService.save(binanceSellOrder);
          }
        }
      } else {
        LOG.info(fullMarketName + " !!! Still have BUY Order " + lastOrder.getOrderId()
            + " waiting to fill at [" + lastOrder.getPrice() + "] - holding last BUY order...");
      }
    } catch (Exception e) {
      LOG.error(
          fullMarketName + " New order to SELL base currency failed because Exchange threw an "
              + "exception. Telling Trading Engine to shutdown bot! Last Order: " + lastOrder,
          e);
      throw new StrategyException(e);
    }
  }

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
        LOG.info(fullMarketName + " Yes!!! Last SELL Order Id [" + lastOrder.getOrderId()
            + "] filled at [" + lastOrder.getPrice() + "]" + "amount :[ " + lastOrder.getOrigQty()
            + " ]");

        if (strategy.shouldEnter(endIndex)) {

          boolean entered = tradingRecord.enter(endIndex, PrecisionNum.valueOf(currentBidPrice),
              PrecisionNum.valueOf(amountOfBaseCurrencyToBuy));

          if (entered) {

            final BigDecimal amountOfBaseCurrencyToBuy =
                getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
                    counterCurrencyBuyOrderAmount);

            LOG.info(fullMarketName + " Placing new BUY order at bid price ["
                + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice) + "]");

            LOG.info(fullMarketName + " Sending new BUY order to exchange --->");

            final NewOrderResponse newOrder = apiService.createRealMarketBuyOrder(marketSymbol,
                amountOfBaseCurrencyToBuy.toPlainString());

            NewBinanceOrder binanceBuyOrder = new NewBinanceOrder(newOrder.getClientOrderId(),
                newOrder.getSymbol(), newOrder.getOrderId(), newOrder.getTransactTime(),
                newOrder.getPrice(), newOrder.getOrigQty(), newOrder.getExecutedQty(),
                newOrder.getCummulativeQuoteQty(), newOrder.getStatus(), newOrder.getTimeInForce(),
                newOrder.getType(), newOrder.getSide());


            lastOrder.setOrderId(newOrder.getOrderId());
            lastOrder.setPrice(newOrder.getPrice());
            lastOrder.setSide(newOrder.getSide());
            lastOrder.setOrigQty(newOrder.getExecutedQty());

            LOG.info(fullMarketName + " New BUY Order sent successfully. With ID: "
                + lastOrder.getOrderId());

            binanceDaoService.save(binanceBuyOrder);
          }
        }
      }
    } catch (Exception e) {
      LOG.error(fullMarketName + " New order to BUY base currency failed because Exchange threw "
          + "exception. Telling Trading Engine to shutdown bot! Last Order: " + lastOrder, e);
      throw new StrategyException(e);
    }
  }

  private BigDecimal getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
      BigDecimal amountOfCounterCurrencyToTrade) throws TradingApiException {

    LOG.info(
        fullMarketName + " Calculating amount of base currency (BTC) to buy for amount of counter "
            + "currency " + new DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
            + " " + marketConfig.getCounterCurrency());

    // Fetch the last trade price
    final BigDecimal lastTradePriceForOneBtc =
        new BigDecimal(apiService.getTickerFromRestClient(marketSymbol).getPrice());


    LOG.info(fullMarketName + " Last trade price for 1 " + marketConfig.getBaseCurrency() + " was: "
        + new DecimalFormat(DECIMAL_FORMAT).format(lastTradePriceForOneBtc) + " "
        + marketConfig.getCounterCurrency());

    final BigDecimal amountOfBaseCurrencyToBuy =
        amountOfCounterCurrencyToTrade.divide(lastTradePriceForOneBtc, 8, RoundingMode.HALF_DOWN);
    /**
     * Some pairs have a minimum trade size. We have to round the base currency to buy to the
     * minimum trade size if the base currency size is smaller then the minimum trade size.
     */
    // TODO: fix ^
    if (marketSymbol.equals("bnbbtc")) {
      BigDecimal newAmountOfBaseCurrencyToBuy =
          amountOfBaseCurrencyToBuy.setScale(0, RoundingMode.UP);

      LOG.info(fullMarketName + " Amount of base currency (" + marketConfig.getBaseCurrency()
          + ") to BUY for " + new DecimalFormat(DECIMAL_FORMAT).format(newAmountOfBaseCurrencyToBuy)
          + " " + marketConfig.getCounterCurrency() + " based on last market trade price: "
          + newAmountOfBaseCurrencyToBuy);
      return newAmountOfBaseCurrencyToBuy;

    }

    LOG.info(fullMarketName + " Amount of base currency (" + marketConfig.getBaseCurrency()
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

    series = new BaseBarSeriesBuilder().withName(fullMarketName).withNumTypeOf(PrecisionNum.class)
        .build();
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

  private Bar convertCandleStickToBaseBar(Candlestick candleStick) {

    Num open = PrecisionNum.valueOf(candleStick.getOpen());
    Num low = PrecisionNum.valueOf(candleStick.getLow());
    Num high = PrecisionNum.valueOf(candleStick.getHigh());
    Num close = PrecisionNum.valueOf(candleStick.getClose());
    Num volume = PrecisionNum.valueOf(candleStick.getVolume());
    Num amount = PrecisionNum.valueOf(candleStick.getNumberOfTrades());

    return new BaseBar(Duration.ofMinutes(DURATION_OF_BAR), ZonedDateTime.now(ZoneId.of("UTC")),
        open, high, low, close, volume, amount);
  }

}
