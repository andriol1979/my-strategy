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

    @Autowired
    public TradingConfigManager(TradingConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    // Thêm một cấu hình mới
    public TradingConfig saveConfig(TradingConfig config) {
        return configRepository.save(config);
    }

    // Lấy tất cả cấu hình active
    public List<TradingConfig> getActiveConfigs(String exchangeName) {
        if (tradingConfigs == null || tradingConfigs.isEmpty()) {
            tradingConfigs = configRepository.findByActiveTrueAndExchangeName(exchangeName);
        }
        return tradingConfigs;
    }

    public Optional<TradingConfig> getActiveConfigBySymbol(String exchangeName, String symbol) {
        return getActiveConfigs(exchangeName).stream().filter(config ->
                config.getSymbol().equals(symbol)).findFirst();
    }

    // Lấy cấu hình theo symbol
    public Optional<TradingConfig> getConfigBySymbol(String symbol) {
        return configRepository.findBySymbol(symbol);
    }

    // Cập nhật cấu hình
    public TradingConfig updateConfig(Long id, TradingConfig updatedConfig) {
        Optional<TradingConfig> existingConfigOpt = configRepository.findById(id);
        if (existingConfigOpt.isPresent()) {
            TradingConfig existingConfig = existingConfigOpt.get();
            existingConfig.setSymbol(updatedConfig.getSymbol());
            existingConfig.setTrailingStopPercent(updatedConfig.getTrailingStopPercent());
            existingConfig.setTargetProfitPercent(updatedConfig.getTargetProfitPercent());
            existingConfig.setDefaultAmount(updatedConfig.getDefaultAmount());
            existingConfig.setActive(updatedConfig.isActive());
            return configRepository.save(existingConfig);
        } else {
            throw new RuntimeException("Config with ID " + id + " not found");
        }
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
