package com.vut.mystrategy.repository;

import com.vut.mystrategy.entity.TradingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradingConfigRepository extends JpaRepository<TradingConfig, Long> {
    List<TradingConfig> findByActiveTrue();
    Optional<TradingConfig> findBySymbol(String symbol);
}
