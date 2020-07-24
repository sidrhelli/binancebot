package com.sidrhelli.binancebot.dao.service;

import com.sidrhelli.binancebot.dao.entitities.NewBinanceOrder;

public interface BinanceDaoService {
  public NewBinanceOrder save(NewBinanceOrder order);
}
