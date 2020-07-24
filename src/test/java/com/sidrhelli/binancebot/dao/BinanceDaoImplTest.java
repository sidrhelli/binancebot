package com.sidrhelli.binancebot.dao;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import com.sidrhelli.binancebot.dao.entitities.NewBinanceOrder;

@EnableAutoConfiguration
@ContextConfiguration(classes = BinanceDao.class)
@DataJpaTest
public class BinanceDaoImplTest {

  @Autowired
  private BinanceDao binanceDao;


  @Test
  public void shouldSaveObject() {
    NewBinanceOrder savedNbo = binanceDao.save(new NewBinanceOrder("test Order"));
    assertThat(savedNbo.getId()).isGreaterThan(0);
  }


}
