package com.vut.mystrategy.repository;

import com.vut.mystrategy.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByExchangeNameAndSymbol(String exchangeName, String symbol);
    List<Order> findByExchangeNameAndSymbolAndKlineIntervalOrderByCreatedAtAsc(String exchangeName, String symbol, String klineInterval);
    long deleteByExchangeNameAndSymbolAndKlineInterval(String exchangeName, String symbol, String klineInterval);
}
