package com.sidrhelli.binancebot.config.service.impl;

public class AuthenticationConfig {
  private String name;
  private String key;
  private String secret;


  public AuthenticationConfig() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getSecret() {
    return secret;
  }

  public void setSectret(String secret) {
    this.secret = secret;
  }


  @Override
  public String toString() {
    return "AuthenticationConfig [name=" + name + ", key=" + key + ", secret=" + secret + "]";
  }



}
