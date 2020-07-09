package com.sidrhelli.binancebot.engine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.sidrhelli.binancebot.apiservice.ApiService;
import com.sidrhelli.binancebot.config.service.ConfigService;
import com.sidrhelli.binancebot.exceptions.StrategyException;
import com.sidrhelli.binancebot.strategies.api.TradingStrategy;

@Component
public class TradingEngine {
  private static final Logger LOG = LogManager.getLogger(TradingEngine.class);
  private final ConfigService configService;

  // TODO: create configuration item for trade cycle interval
  private static final int TRADE_CYCLE_INTERVAL = 20;
  private static final Object IS_RUNNING_MONITOR = new Object();
  @SuppressWarnings("unused")
  private Thread engineThread;
  private volatile boolean keepAlive = true;
  private boolean isRunning = false;
  private TradingStrategy cryptoMomentumStrategy;
  private ApiService apiService;

  @Autowired
  public TradingEngine(ConfigService configService, TradingStrategy cryptoMomentumStrategy,
      ApiService apiService) {
    this.configService = configService;
    this.cryptoMomentumStrategy = cryptoMomentumStrategy;
    this.apiService = apiService;
  }

  public void start() {

    synchronized (IS_RUNNING_MONITOR) {
      if (isRunning) {
        final String errorMsg = "Cannot start Trading Engine because it is already running!";
        // LOG.error(() -> errorMsg);
        throw new IllegalStateException(errorMsg);
      }
      isRunning = true;
    }

    // store this so we can shutdown the engine later
    engineThread = Thread.currentThread();
    System.out.println("Bot has started...");
    initEngine();
    runMainControlLoop();

  }


  /*
   * The main control loop. We loop infinitely unless an unexpected exception occurs. The code fails
   * hard and fast if an unexpected occurs.
   */
  private void runMainControlLoop() {
    LOG.info("Executing Trading Strategy ---> " + cryptoMomentumStrategy.getClass().getName());
    while (keepAlive) {
      try {
        System.out.println("*** Starting next trade cycle... ***");

        cryptoMomentumStrategy.execute();


        sleepUntilNextTradingCycle();

      } catch (StrategyException e) {
        handleStrategyException(e);


      } catch (Exception e) {
        handleUnexpectedException(e);
      }
    }

    synchronized (IS_RUNNING_MONITOR) {
      isRunning = false;
    }
  }

  synchronized boolean isRunning() {
    System.out.println("isRunning: " + isRunning);
    return isRunning;
  }

  private void sleepUntilNextTradingCycle() {
    System.out
        .println("*** Sleeping " + TRADE_CYCLE_INTERVAL + " seconds til next trade cycle... ***");
    try {
      Thread.sleep(TRADE_CYCLE_INTERVAL * 1000L);
    } catch (InterruptedException e) {
      System.out.println("Control Loop thread interrupted when sleeping before next trade cycle");
      Thread.currentThread().interrupt();
    }
  }

  private void initEngine() {
    cryptoMomentumStrategy.initStrategy(apiService, configService.getMarketConfig());
  }

  private void handleStrategyException(StrategyException e) {
    final String fatalErrorMsg = "A FATAL error has occurred in Trading Strategy!";
    LOG.info(fatalErrorMsg, e);

    keepAlive = false;
  }

  private void handleUnexpectedException(Exception e) {
    final String fatalErrorMsg =
        "An unexpected FATAL error has occurred in Exchange Adapter or " + "Trading Strategy!";
    LOG.fatal(() -> fatalErrorMsg, e);

    keepAlive = false;
  }



}
