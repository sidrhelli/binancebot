package com.sidrhelli.binancebot.dao;

import org.springframework.data.repository.CrudRepository;
import com.sidrhelli.binancebot.dao.entitities.NewBinanceOrder;

public interface BinanceDao extends CrudRepository<NewBinanceOrder, Long> {

}
