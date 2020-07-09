package com.sidrhelli.binancebot.apiservice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.TickerPrice;

public interface ApiService {
  Map<String, AssetBalance> getAccountBalanceCache();

  List<Order> getOpenOrders(String symbol);

  NavigableMap<BigDecimal, BigDecimal> getAsks();

  NavigableMap<BigDecimal, BigDecimal> getBids();

  Map.Entry<BigDecimal, BigDecimal> getBestAsk();

  Map.Entry<BigDecimal, BigDecimal> getBestBid();

  Map<Long, Candlestick> getCandlesticksCache();

  Map<String, NavigableMap<BigDecimal, BigDecimal>> getMarketOrderBook();

  List<Candlestick> getCandleStickInitiatorList();

  Order getOrderStatus(Long orderId, String symbol);

  NewOrderResponse createRealMarketBuyOrder(String symbol, String quantity);

  NewOrderResponse createRealMarketSellOrder(String symbol, String quantity);

  TickerPrice getTickerFromRestClient(String symbol);

  void initializeCandlestickCache(String symbol);

  void startCandlestickEventStreaming(String symbol);

  List<Candlestick> getCandleStickListFromRestClient(int size);


}
