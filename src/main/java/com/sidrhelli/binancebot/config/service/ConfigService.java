package com.sidrhelli.binancebot.config.service;

import com.sidrhelli.binancebot.config.service.domain.objects.MarketConfig;
import com.sidrhelli.binancebot.config.service.impl.AuthenticationConfig;

public interface ConfigService {
  AuthenticationConfig getAuthenticationConfig();

  MarketConfig getMarketConfig();
}
