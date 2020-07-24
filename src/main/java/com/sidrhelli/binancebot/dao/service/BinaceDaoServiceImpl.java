package com.sidrhelli.binancebot.dao.service;

import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sidrhelli.binancebot.dao.BinanceDao;
import com.sidrhelli.binancebot.dao.entitities.NewBinanceOrder;

@Service
public class BinaceDaoServiceImpl implements BinanceDaoService {
  @Autowired
  private BinanceDao binanceDao;

  @Transactional
  public NewBinanceOrder save(NewBinanceOrder order) {
    return binanceDao.save(order);
  }

  public Optional<NewBinanceOrder> findById(long id) {
    return binanceDao.findById(id);
  }


}
