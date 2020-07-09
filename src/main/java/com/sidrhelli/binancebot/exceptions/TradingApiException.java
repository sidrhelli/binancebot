package com.sidrhelli.binancebot.exceptions;

public class TradingApiException extends Exception {

  private static final long serialVersionUID = -8279304672615688060L;

  public TradingApiException(String msg) {
    super(msg);
  }

  public TradingApiException(String msg, Throwable e) {
    super(msg, e);
  }
}
