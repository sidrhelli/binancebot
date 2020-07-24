package com.sidrhelli.binancebot.apiservice.impl;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.event.UserDataUpdateEvent;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.binance.api.client.domain.market.TickerPrice;
import com.sidrhelli.binancebot.apiservice.ApiService;
import com.sidrhelli.binancebot.config.service.ConfigService;


@Component
public class ApiServiceImpl implements ApiService {

  private ConfigService configService;
  private BinanceApiClientFactory factory;

  private static final String BIDS = "BIDS";
  private static final String ASKS = "ASKS";
  private static final CandlestickInterval INTERVAL = CandlestickInterval.ONE_MINUTE;
  private long lastUpdateId;
  private Map<String, NavigableMap<BigDecimal, BigDecimal>> depthCache;
  private OrderBook orderBook;
  private String apiKey;
  private String secret;
  private String symbol;
  private Map<Long, Candlestick> candlesticksCache;
  private Map<String, AssetBalance> accountBalanceCache;
  private String listenKey;
  @SuppressWarnings("unused")
  private String latestPrice;


  @Autowired
  public ApiServiceImpl(ConfigService configService) {
    this.configService = configService;
    this.depthCache = new HashMap<>();
    initApi();
  }

  public Map<String, AssetBalance> getAccountBalanceCache() {
    return accountBalanceCache;
  }

  @Override
  public NavigableMap<BigDecimal, BigDecimal> getAsks() {
    return depthCache.get(ASKS);
  }

  @Override
  public NavigableMap<BigDecimal, BigDecimal> getBids() {
    return depthCache.get(BIDS);
  }

  @Override
  public Map.Entry<BigDecimal, BigDecimal> getBestAsk() {
    return getAsks().lastEntry();
  }

  @Override
  public Map.Entry<BigDecimal, BigDecimal> getBestBid() {
    return getBids().firstEntry();
  }

  @Override
  public Map<String, NavigableMap<BigDecimal, BigDecimal>> getMarketOrderBook() {
    return depthCache;
  }


  @Override
  public Map<Long, Candlestick> getCandlesticksCache() {
    return candlesticksCache;
  }

  @Override
  public NewOrderResponse createRealMarketBuyOrder(String symbol, String quantity) {
    BinanceApiRestClient restClient = factory.newRestClient();
    return restClient.newOrder(marketBuy(symbol.toUpperCase(), quantity));
  }

  @Override
  public NewOrderResponse createRealMarketSellOrder(String symbol, String quantity) {
    BinanceApiRestClient restClient = factory.newRestClient();
    return restClient.newOrder(marketSell(symbol.toUpperCase(), quantity));
  }

  @Override
  public List<Order> getOpenOrders(String symbol) {
    BinanceApiRestClient restClient = factory.newRestClient();
    OrderRequest orderRequest = new OrderRequest(symbol.toUpperCase());
    return restClient.getOpenOrders(orderRequest);
  }

  @Override
  public Order getOrderStatus(Long orderId, String symbol) {
    BinanceApiRestClient restClient = factory.newRestClient();
    OrderStatusRequest orderStatusRequest = new OrderStatusRequest(symbol.toUpperCase(), orderId);
    return restClient.getOrderStatus(orderStatusRequest);
  }

  @Override
  public List<Candlestick> getCandleStickInitiatorList() {
    BinanceApiRestClient restClient = factory.newRestClient();
    List<Candlestick> candlestickBars =
        restClient.getCandlestickBars(symbol.toUpperCase(), INTERVAL);
    return candlestickBars;

  }

  @Override
  public TickerPrice getTickerFromRestClient(String symbol) {
    BinanceApiRestClient restClient = factory.newRestClient();
    return restClient.getPrice(symbol.toUpperCase());
  }

  @Override
  public List<Candlestick> getCandleStickListFromRestClient(int size) {
    BinanceApiRestClient restClient = factory.newRestClient();
    List<Candlestick> retList = new ArrayList<Candlestick>();
    List<Candlestick> candleStickBars =
        restClient.getCandlestickBars(symbol.toUpperCase(), INTERVAL);

    int candleStickBarsSize = candleStickBars.size();

    for (int i = (candleStickBarsSize - 1) - size; i < candleStickBarsSize - 1; i++) {
      Candlestick candlestick = candleStickBars.get(i);
      retList.add(candlestick);
    }
    return retList;
  }

  public void setLatestPrice(String latestPrice) {
    this.latestPrice = latestPrice;
  }



  /**
   * Initializes the asset balance cache by using the REST API and starts a new user data streaming
   * session.
   *
   * @return a listenKey that can be used with the user data streaming API.
   */
  private String initializeAssetBalanceCacheAndStreamSession() {
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(apiKey, secret);
    BinanceApiRestClient restClient = factory.newRestClient();
    Account account = restClient.getAccount();

    this.accountBalanceCache = new TreeMap<>();
    for (AssetBalance assetBalance : account.getBalances()) {
      accountBalanceCache.put(assetBalance.getAsset(), assetBalance);
    }

    return restClient.startUserDataStream();
  }


  private void startAccountBalanceEventStreaming(String listenKey) {
    BinanceApiWebSocketClient webSocketClient =
        BinanceApiClientFactory.newInstance(apiKey, secret).newWebSocketClient();


    webSocketClient.onUserDataUpdateEvent(listenKey, response -> {
      if (response.getEventType() == UserDataUpdateEvent.UserDataUpdateEventType.ACCOUNT_UPDATE) {
        // Override cached asset balances
        for (AssetBalance assetBalance : response.getAccountUpdateEvent().getBalances()) {
          accountBalanceCache.put(assetBalance.getAsset(), assetBalance);
        }
        System.out.println(accountBalanceCache);
      }
    });
  }


  private void initializeDepthCache(String symbol) {
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(apiKey, secret);
    BinanceApiRestClient restClient = factory.newRestClient();
    orderBook = restClient.getOrderBook(symbol.toUpperCase(), 10);
    this.lastUpdateId = orderBook.getLastUpdateId();
    NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>(Comparator.reverseOrder());

    for (OrderBookEntry ask : orderBook.getAsks()) {
      asks.put(new BigDecimal(ask.getPrice()), new BigDecimal(ask.getQty()));
    }
    depthCache.put(ASKS, asks);

    NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
    for (OrderBookEntry bid : orderBook.getBids()) {
      bids.put(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()));
    }
    depthCache.put(BIDS, bids);
  }

  private void startDepthEventStreaming(String symbol) {
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
    BinanceApiWebSocketClient webSocketClient = factory.newWebSocketClient();

    webSocketClient.onDepthEvent(symbol.toLowerCase(), response -> {
      if (response.getFinalUpdateId() > lastUpdateId) {
        lastUpdateId = response.getFinalUpdateId();
        updateOrderBook(getAsks(), response.getAsks());
        updateOrderBook(getBids(), response.getBids());
      }
    });
  }

  /**
   * Updates an order book (bids or asks) with a delta received from the server.
   *
   * Whenever the qty specified is ZERO, it means the price should was removed from the order book.
   */
  private void updateOrderBook(NavigableMap<BigDecimal, BigDecimal> lastOrderBookEntries,
      List<OrderBookEntry> orderBookDeltas) {
    for (OrderBookEntry orderBookDelta : orderBookDeltas) {
      BigDecimal price = new BigDecimal(orderBookDelta.getPrice());
      BigDecimal qty = new BigDecimal(orderBookDelta.getQty());
      if (qty.compareTo(BigDecimal.ZERO) == 0) {
        // qty=0 means remove this level
        lastOrderBookEntries.remove(price);
      } else {
        lastOrderBookEntries.put(price, qty);
      }
    }
  }

  /**
   * Initializes the candlestick cache by using the REST API.
   */
  @Override
  public void initializeCandlestickCache(String symbol) {
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(apiKey, secret);
    BinanceApiRestClient restClient = factory.newRestClient();

    List<Candlestick> candlestickBars =
        restClient.getCandlestickBars(symbol.toUpperCase(), INTERVAL);

    this.candlesticksCache = new TreeMap<>();
    for (Candlestick candlestickBar : candlestickBars) {
      candlesticksCache.put(candlestickBar.getOpenTime(), candlestickBar);
    }
  }


  @Override
  public void startCandlestickEventStreaming(String symbol) {
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
    BinanceApiWebSocketClient webSocketClient = factory.newWebSocketClient();

    webSocketClient.onCandlestickEvent(symbol.toLowerCase(), INTERVAL, response -> {

      Long openTime = response.getOpenTime();
      Candlestick updateCandlestick = candlesticksCache.get(openTime);
      if (updateCandlestick == null) {
        // new candlestick
        updateCandlestick = new Candlestick();
      }
      // update candlestick with the stream data
      updateCandlestick.setOpenTime(response.getOpenTime());
      updateCandlestick.setOpen(response.getOpen());
      updateCandlestick.setLow(response.getLow());
      updateCandlestick.setHigh(response.getHigh());
      updateCandlestick.setClose(response.getClose());
      updateCandlestick.setCloseTime(response.getCloseTime());
      updateCandlestick.setVolume(response.getVolume());
      updateCandlestick.setNumberOfTrades(response.getNumberOfTrades());
      updateCandlestick.setQuoteAssetVolume(response.getQuoteAssetVolume());
      updateCandlestick.setTakerBuyQuoteAssetVolume(response.getTakerBuyQuoteAssetVolume());
      updateCandlestick.setTakerBuyBaseAssetVolume(response.getTakerBuyQuoteAssetVolume());

      candlesticksCache.put(openTime, updateCandlestick);
      setLatestPrice(response.getClose());
    });


  }


  private void initApi() {
    this.apiKey = configService.getAuthenticationConfig().getKey();
    this.secret = configService.getAuthenticationConfig().getSecret();
    factory = BinanceApiClientFactory.newInstance(apiKey, secret);
    listenKey = initializeAssetBalanceCacheAndStreamSession();
    startAccountBalanceEventStreaming(listenKey);
    this.symbol = configService.getMarketConfig().getId();
    initializeDepthCache(symbol);
    startDepthEventStreaming(symbol);
  }



}
