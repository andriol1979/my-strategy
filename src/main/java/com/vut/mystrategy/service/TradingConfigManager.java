package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.repository.TradingConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TradingConfigManager {

    private final TradingConfigRepository configRepository;
    private List<TradingConfig> tradingConfigs = new ArrayList<>();
    private List<TradingConfig> allActiveTradingConfigs = new ArrayList<>();

    @Autowired
    public TradingConfigManager(TradingConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    // Thêm một cấu hình mới
    public TradingConfig saveConfig(TradingConfig config) {
        return configRepository.save(config);
    }

    //Get all trading_config have active = true
    public List<TradingConfig> getAllActiveConfigs() {
        if (allActiveTradingConfigs == null || allActiveTradingConfigs.isEmpty()) {
            allActiveTradingConfigs = configRepository.findByActiveTrue();
        }
        return allActiveTradingConfigs;
    }

    //Get all trading_config have active = true by exchangeName
    public List<TradingConfig> getActiveConfigs(String exchangeName) {
        if (tradingConfigs == null || tradingConfigs.isEmpty()) {
            tradingConfigs = configRepository.findByActiveTrueAndExchangeName(exchangeName);
        }
        return tradingConfigs;
    }

    public Optional<TradingConfig> getActiveConfigBySymbol(String exchangeName, String symbol) {
        return getActiveConfigs(exchangeName).stream().filter(config ->
                config.getSymbol().equalsIgnoreCase(symbol)).findFirst();
    }

    // Lấy cấu hình theo symbol
    public Optional<TradingConfig> getConfigBySymbol(String symbol) {
        return configRepository.findBySymbol(symbol);
    }

    // Xóa cấu hình
    public void deleteConfig(Long id) {
        configRepository.deleteById(id);
    }

    // Kích hoạt/tắt cấu hình
    public TradingConfig toggleConfigActive(Long id, boolean active) {
        Optional<TradingConfig> configOpt = configRepository.findById(id);
        if (configOpt.isPresent()) {
            TradingConfig config = configOpt.get();
            config.setActive(active);
            return configRepository.save(config);
        } else {
            throw new RuntimeException("Config with ID " + id + " not found");
        }
    }
}
