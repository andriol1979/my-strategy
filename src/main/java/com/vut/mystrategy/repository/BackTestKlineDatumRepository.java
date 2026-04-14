package com.vut.mystrategy.repository;

import com.vut.mystrategy.entity.BackTestKlineData;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BackTestKlineDatumRepository extends JpaRepository<BackTestKlineData, Long> {
    List<BackTestKlineData> findByExchangeNameAndSymbolAndKlineInterval(String exchangeName, String symbol,
                                                                    String klineInterval, Sort sort);

    @Query(value = """
               SELECT * FROM backtest_kline_data
               WHERE symbol = :symbol
                     AND kline_interval = :klineInterval
                     AND close_time >= :from
                     AND close_time <= :to
              ORDER BY close_time ASC
              """, nativeQuery = true)
    List<BackTestKlineData> getPeriodBackTestData(String symbol, String klineInterval,
                                              long from, long to);
}