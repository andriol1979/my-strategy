package com.vut.mystrategy.configuration.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.service.RedisClientService;
import com.vut.mystrategy.service.TradingConfigManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BinanceExchangeInfoConfig {

    @Value("${binance.future.api.url}")
    private String binanceFutureApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisClientService redisClientService;
    private final TradingConfigManager tradingConfigManager;

    @Autowired
    public BinanceExchangeInfoConfig(RedisClientService redisClientService,
                                     TradingConfigManager tradingConfigManager) {
        this.redisClientService = redisClientService;
        this.tradingConfigManager = tradingConfigManager;
    }

    @PostConstruct
    public void init() {
        List<TradingConfig> tradingConfigs = tradingConfigManager.getActiveConfigs(Constant.EXCHANGE_NAME_BINANCE);
        fetchAndStoreLotSizeFilters(tradingConfigs);
    }

    // Lấy và lưu toàn bộ LOT_SIZE vào Redis
    private void fetchAndStoreLotSizeFilters(List<TradingConfig> tradingConfigs) {
        try {
            String url = binanceFutureApiUrl + "/fapi/v1/exchangeInfo";
            String response = restTemplate.getForObject(url, String.class);
            Map<String, Object> json = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> symbols = (List<Map<String, Object>>) json.get("symbols");

            for (Map<String, Object> symbolInfo : symbols) {
                String symbol = (String) symbolInfo.get("symbol");
                if (!isSaveLotSizeFilter(tradingConfigs, symbol)) {
                    continue;
                }

                List<Map<String, Object>> filters = (List<Map<String, Object>>) symbolInfo.get("filters");
                for (Map<String, Object> filter : filters) {
                    if ("LOT_SIZE".equals(filter.get("filterType"))) {
                        BinanceFutureLotSizeResponse lotSizeFilter = objectMapper.convertValue(filter, BinanceFutureLotSizeResponse.class);
                        lotSizeFilter.setSymbol(symbol);
                        redisClientService.saveFutureLotSize(Constant.EXCHANGE_NAME_BINANCE, symbol, lotSizeFilter);
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("Error fetching and storing LOT_SIZE filters: {}", e.getMessage());
        }
    }

    private boolean isSaveLotSizeFilter(List<TradingConfig> tradingConfigs, String symbol) {
        return tradingConfigs.stream().anyMatch(tradingConfig ->
                tradingConfig.getSymbol().equalsIgnoreCase(symbol));
    }
}
