package com.vut.mystrategy.configuration.feeddata.binance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class BinanceExchangeInfoConfig {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, BinanceFutureLotSizeResponse> futureLotSizeResponseMap = new ConcurrentHashMap<>();

    @PostConstruct
    @SneakyThrows
    public void loadLotSize() {
        // Load file từ resources
        ClassPathResource resource = new ClassPathResource("data/binance_exchange_info.json");
        try (InputStream inputStream = resource.getInputStream()) {
            // Chuyển JSON thành List<SymbolConfig>
            Map<String, Object> json = objectMapper.readValue(inputStream, new TypeReference<>() {});
            List<Map<String, Object>> symbols = (List<Map<String, Object>>) json.get("symbols");

            for (Map<String, Object> symbolInfo : symbols) {
                String symbol = (String) symbolInfo.get("symbol");

                List<Map<String, Object>> filters = (List<Map<String, Object>>) symbolInfo.get("filters");
                for (Map<String, Object> filter : filters) {
                    if ("LOT_SIZE".equals(filter.get("filterType"))) {
                        BinanceFutureLotSizeResponse lotSizeFilter = objectMapper.convertValue(filter, BinanceFutureLotSizeResponse.class);
                        lotSizeFilter.setSymbol(symbol);

                        //put it to map
                        String futureLotSizeMapKey = KeyUtility.getFutureLotSizeMapKey(Constant.EXCHANGE_NAME_BINANCE, symbol);
                        futureLotSizeResponseMap.put(futureLotSizeMapKey, lotSizeFilter);
                    }
                }
            }
        }
        log.info("Loaded total {} Binance futures lot sizes", futureLotSizeResponseMap.size());
    }

    public BinanceFutureLotSizeResponse getLotSizeBySymbol(String symbol) {
        String lotSizeMapKey = KeyUtility.getFutureLotSizeMapKey(Constant.EXCHANGE_NAME_BINANCE, symbol);
        return futureLotSizeResponseMap.get(lotSizeMapKey);
    }
}
