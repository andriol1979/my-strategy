package com.vut.mystrategy.service.order.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.configuration.feeddata.binance.BinanceExchangeInfoConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.MyStrategyBaseBar;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.service.OrderService;
import com.vut.mystrategy.service.RedisClientService;
import com.vut.mystrategy.service.order.AbstractOrderManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Slf4j
@Service("binance-staging")
@Profile("staging")
public class StagingBinanceOrderManager extends AbstractOrderManager {
    private final BinanceExchangeInfoConfig binanceExchangeInfoConfig;
    private final BinanceApiHelper apiHelper;

    @Value("${binance.testnet.api.key}")
    private String apiKey;
    @Value("${binance.testnet.api.secret}")
    private String secretKey;
    @Value("${binance.testnet.api.url}")
    private String baseUrl;

    private final String endpoint = "/fapi/v1/order";

    @Autowired
    public StagingBinanceOrderManager(RedisClientService redisClientService,
                                      RestTemplate restTemplate,
                                      ObjectMapper objectMapper,
                                      OrderService orderService,
                                      BinanceApiHelper apiHelper,
                                      BinanceExchangeInfoConfig binanceExchangeInfoConfig) {
        super(redisClientService, restTemplate, objectMapper, orderService);
        this.apiHelper = apiHelper;
        this.binanceExchangeInfoConfig = binanceExchangeInfoConfig;
    }

    @PostConstruct
    public void init() {
        log.info("✅ StagingBinanceOrderManager initialized for Testnet");
    }

    @Override
    public BaseOrderResponse placeOrder(MyStrategyBaseBar entryBar, int entryIndex,
                                        SymbolConfig symbolConfig, boolean isShort) {
        try {
            String url = baseUrl + endpoint;

            long timestamp = System.currentTimeMillis();
            String side = isShort ? "SELL" : "BUY";
            String positionSide = isShort ? "SHORT" : "LONG";

            BinanceFutureLotSizeResponse lotSize = binanceExchangeInfoConfig.getLotSizeBySymbol(symbolConfig.getSymbol());
            Pair<BigDecimal, BigDecimal> calculatedLotSize = Calculator.calculateLotSize(lotSize, symbolConfig.getOrderVolume(),
                    entryBar.getClosePrice().bigDecimalValue());

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("symbol", symbolConfig.getSymbol().toUpperCase());
            params.add("side", side);
            params.add("type", "MARKET");
            params.add("quantity", calculatedLotSize.getLeft().toPlainString());
            params.add("positionSide", positionSide);
            params.add("timestamp", String.valueOf(timestamp));
            params.add("recvWindow", "10000");

            String queryString = apiHelper.buildQueryString(params);
            String signature = apiHelper.sign(queryString, secretKey);
            queryString += "&signature=" + signature;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", apiKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>(null, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url + "?" + queryString,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return objectMapper.readValue(response.getBody(), BinanceOrderResponse.class);
            }
            else {
                log.warn("❌ Order failed: {}", response.getBody());
                return null;
            }
        }
        catch (Exception e) {
            log.error("🔥 Error placing order", e);
            return null;
        }
    }

    @Override
    public BaseOrderResponse exitOrder(BaseOrderResponse entryResponse, MyStrategyBaseBar exitBar,
                                       int exitIndex, SymbolConfig symbolConfig, boolean isShort) {
        return placeOrder(exitBar, exitIndex, symbolConfig, isShort);
    }

    @Override
    public boolean shouldStopOrder(String orderStorageRedisKey, BaseOrderResponse entryResponse,
                                   MyStrategyBaseBar exitBar, SymbolConfig symbolConfig, boolean isShort) {
        BinanceOrderResponse response = entryResponse.as(BinanceOrderResponse.class);
        if(!redisClientService.exists(orderStorageRedisKey)) {
            //vị thế đã được đóng bởi điều kiện shouldExit hoặc chưa mở
            // không cần kiểm tra stop
            return false;
        }
        boolean isReachStopLoss = isReachStopLoss(response.getAvgPriceAsBigDecimal(),
                symbolConfig.getStopLoss(), exitBar.getClosePrice(), isShort);
        boolean isReachTakeProfit = isReachTakeProfit(response.getAvgPriceAsBigDecimal(),
                symbolConfig.getTargetProfit(), exitBar.getClosePrice(), isShort);
        boolean isStuck = isStuckOrder(response.getTransactTime());
        log.info("isReachStopLoss: {} - isReachTakeProfit: {} - isStuck: {}", isReachStopLoss, isReachTakeProfit, isStuck);
        return isReachStopLoss || isReachTakeProfit || isStuck;
    }
}
