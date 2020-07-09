package com.sidrhelli.binancebot.config.service.impl;

import java.io.File;
import java.io.IOException;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sidrhelli.binancebot.config.service.ConfigService;
import com.sidrhelli.binancebot.config.service.domain.objects.FileLocations;
import com.sidrhelli.binancebot.config.service.domain.objects.MarketConfig;


@Service
public class ConfigServiceImpl implements ConfigService {

  public ConfigServiceImpl() {}

  @Override
  public MarketConfig getMarketConfig() {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    objectMapper.findAndRegisterModules();
    MarketConfig markets = null;

    try {
      markets = objectMapper.readValue(new File(FileLocations.MARKETS_CONFIG), MarketConfig.class);
      return markets;
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return markets;
  }

  @Override
  public AuthenticationConfig getAuthenticationConfig() {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    objectMapper.findAndRegisterModules();
    AuthenticationConfig authenticationConfig = null;

    try {
      authenticationConfig = objectMapper.readValue(new File(FileLocations.AUTHENTICATION_CONFIG),
          AuthenticationConfig.class);
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return authenticationConfig;


  }

}
