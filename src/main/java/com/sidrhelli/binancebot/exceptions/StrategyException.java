package com.sidrhelli.binancebot.exceptions;

public final class StrategyException extends Exception {

  private static final long serialVersionUID = 1L;

  public StrategyException(String msg) {
    super(msg);
  }

  public StrategyException(Throwable e) {
    super(e);
  }

  public StrategyException(String msg, Throwable e) {
    super(msg, e);
  }
}
