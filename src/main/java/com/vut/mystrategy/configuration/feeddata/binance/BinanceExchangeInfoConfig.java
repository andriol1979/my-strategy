package com.vut.mystrategy.configuration.feeddata.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.service.KlineEventService;
import com.vut.mystrategy.configuration.SymbolConfigManager;
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
    private final KlineEventService klineEventService;
    private final SymbolConfigManager symbolConfigManager;

    @Autowired
    public BinanceExchangeInfoConfig(KlineEventService klineEventService,
                                     SymbolConfigManager symbolConfigManager) {
        this.klineEventService = klineEventService;
        this.symbolConfigManager = symbolConfigManager;
    }

    public void loadLotSize() {
        List<SymbolConfig> symbolConfigs = symbolConfigManager.getActiveSymbolConfigsListByExchangeName(Constant.EXCHANGE_NAME_BINANCE);
//        fetchAndStoreLotSizeFilters(symbolConfigs);
    }

    // Lấy và lưu toàn bộ LOT_SIZE vào Redis
    private void fetchAndStoreLotSizeFilters(List<SymbolConfig> symbolConfigs) {
        try {
            String url = binanceFutureApiUrl + "/fapi/v1/exchangeInfo";
            String response = restTemplate.getForObject(url, String.class);
            Map<String, Object> json = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> symbols = (List<Map<String, Object>>) json.get("symbols");

            for (Map<String, Object> symbolInfo : symbols) {
                String symbol = (String) symbolInfo.get("symbol");
                if (!isSaveLotSizeFilter(symbolConfigs, symbol)) {
                    continue;
                }

                List<Map<String, Object>> filters = (List<Map<String, Object>>) symbolInfo.get("filters");
                for (Map<String, Object> filter : filters) {
                    if ("LOT_SIZE".equals(filter.get("filterType"))) {
                        BinanceFutureLotSizeResponse lotSizeFilter = objectMapper.convertValue(filter, BinanceFutureLotSizeResponse.class);
                        lotSizeFilter.setSymbol(symbol);
                        klineEventService.saveFutureLotSize(Constant.EXCHANGE_NAME_BINANCE, symbol, lotSizeFilter);
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("Error fetching and storing LOT_SIZE filters: {}", e.getMessage());
        }
    }

    private boolean isSaveLotSizeFilter(List<SymbolConfig> symbolConfigs, String symbol) {
        return symbolConfigs.stream().anyMatch(tradingConfig ->
                tradingConfig.getSymbol().equalsIgnoreCase(symbol));
    }
}
