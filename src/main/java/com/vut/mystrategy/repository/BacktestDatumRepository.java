package com.vut.mystrategy.repository;

import com.vut.mystrategy.entity.BacktestDatum;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BacktestDatumRepository extends JpaRepository<BacktestDatum, Long> {
    List<BacktestDatum> findByExchangeNameAndSymbolAndKlineInterval(String exchangeName, String symbol,
                                                                    String klineInterval, Sort sort);
}