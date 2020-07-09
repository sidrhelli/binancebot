package com.sidrhelli.binancebot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.sidrhelli.binancebot.engine.TradingEngine;

@SpringBootApplication
public class BinancebotApplication implements CommandLineRunner {

  private final TradingEngine tradingEngine;

  @Autowired
  public BinancebotApplication(TradingEngine tradingEngine) {
    this.tradingEngine = tradingEngine;
  }

  public static void main(String[] args) {
    SpringApplication.run(BinancebotApplication.class, args);
  }

  @Override
  public void run(String... args) {
    tradingEngine.start();
  }

}
