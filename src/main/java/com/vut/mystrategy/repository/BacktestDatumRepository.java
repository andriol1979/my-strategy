package com.vut.mystrategy.repository;

import com.vut.mystrategy.entity.BacktestDatum;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BacktestDatumRepository extends JpaRepository<BacktestDatum, Long> {
    List<BacktestDatum> findByExchangeNameAndSymbolAndKlineInterval(String exchangeName, String symbol,
                                                                    String klineInterval, Sort sort);

    @Query(value = """
             SELECT * FROM backtest_data
             WHERE symbol = :symbol
                   AND kline_interval = :klineInterval
                   AND event_time >= :from
                   AND event_time <= :to
            ORDER BY event_time ASC
            """, nativeQuery = true)
    List<BacktestDatum> getPeriodBackTestData(String symbol, String klineInterval,
                                              Instant from, Instant to);
}