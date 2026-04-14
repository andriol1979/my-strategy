package com.vut.mystrategy.service.order.binance;

import com.vut.mystrategy.component.binance.BinanceFutureRestApiClient;
import com.vut.mystrategy.component.binance.starter.BinanceExchangeInfoConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.MyStrategyBaseBar;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.BinanceOrderRequest;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.service.OrderService;
import com.vut.mystrategy.service.RedisClientService;
import com.vut.mystrategy.service.order.AbstractOrderManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service("binance-staging")
@Profile("staging")
public class StagingBinanceOrderManager extends AbstractOrderManager {
    private final BinanceExchangeInfoConfig binanceExchangeInfoConfig;

    @Autowired
    public StagingBinanceOrderManager(RedisClientService redisClientService,
                                      BinanceFutureRestApiClient binanceFutureRestApiClient,
                                      OrderService orderService,
                                      BinanceExchangeInfoConfig binanceExchangeInfoConfig) {
        super(redisClientService, binanceFutureRestApiClient, orderService);
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
            String side = isShort ? "SELL" : "BUY";
            String positionSide = isShort ? "SHORT" : "LONG";
            BinanceFutureLotSizeResponse lotSize = binanceExchangeInfoConfig.getLotSizeBySymbol(symbolConfig.getSymbol());
            Pair<BigDecimal, BigDecimal> calculatedLotSize = Calculator.calculateLotSize(lotSize, symbolConfig.getOrderVolume(),
                    entryBar.getClosePrice().bigDecimalValue());

            //Build BinanceOrderRequest
            BinanceOrderRequest orderRequest = BinanceOrderRequest.builder()
                    .symbol(symbolConfig.getSymbol())
                    .side(side)
                    .type("MARKET")
                    .quantity(calculatedLotSize.getLeft().toPlainString())
                    .positionSide(positionSide)
                    .newClientOrderId(KeyUtility.generateClientOrderId())
                    .timestamp(System.currentTimeMillis())
                    .build();
            return binanceFutureRestApiClient.placeOrder(orderRequest);
        }
        catch (Exception e) {
            log.error("🔥 [StagingBinanceOrderManager] Error placing order", e);
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
        return super.shouldStopOrder(orderStorageRedisKey, response.getAvgPriceAsBigDecimal(), exitBar.getClosePrice(),
                symbolConfig.getStopLoss(), symbolConfig.getTargetProfit(), isShort, response.getTransactTime());
    }
}
