package com.vut.mystrategy.configuration;

import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.service.PriceTrendingMonitor;
import com.vut.mystrategy.service.TradingConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PriceTrendingScheduler {

    private final TradingConfigManager tradingConfigManager;
    private final PriceTrendingMonitor priceTrendingMonitor;

    public PriceTrendingScheduler(TradingConfigManager tradingConfigManager,
            PriceTrendingMonitor priceTrendingMonitor) {
        this.tradingConfigManager = tradingConfigManager;
        this.priceTrendingMonitor = priceTrendingMonitor;
    }

    @Scheduled(fixedRateString = "${schedule.delay.price-trend:2000}")
    public void schedulePriceTrendCalculation() {
        List<TradingConfig> tradingConfigs = tradingConfigManager.getActiveConfigs(Constant.EXCHANGE_NAME_BINANCE);
        if (tradingConfigs.isEmpty()) {
            log.warn("No active trading configs found for Binance");
            return;
        }

        tradingConfigs.forEach(tradingConfig ->
                priceTrendingMonitor.calculatePriceTrend(tradingConfig.getExchangeName(), tradingConfig.getSymbol())
        );
    }
}
