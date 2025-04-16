package com.vut.mystrategy.service.order.binance;

import com.vut.mystrategy.component.binance.BinanceFutureRestApiClient;
import com.vut.mystrategy.component.binance.starter.BinanceExchangeInfoConfig;
import com.vut.mystrategy.helper.BarDurationHelper;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.service.order.AbstractOrderManager;
import com.vut.mystrategy.service.OrderService;
import com.vut.mystrategy.service.RedisClientService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service("binance-prod")
@Profile("prod")
public class ProdBinanceOrderManager extends AbstractOrderManager {
    private final BinanceExchangeInfoConfig binanceExchangeInfoConfig;

    @Autowired
    public ProdBinanceOrderManager(RedisClientService redisClientService,
                                   BinanceFutureRestApiClient binanceFutureRestApiClient,
                                   OrderService orderService,
                                   BinanceExchangeInfoConfig binanceExchangeInfoConfig) {
        super(redisClientService, binanceFutureRestApiClient, orderService);
        this.binanceExchangeInfoConfig = binanceExchangeInfoConfig;
    }

    @PostConstruct
    public void init() {
        log.info("✅ ProdBinanceOrderManager initialized for Testnet");
    }

    @Override
    public BaseOrderResponse placeOrder(MyStrategyBaseBar entryBar, int entryIndex,
                                        SymbolConfig symbolConfig, boolean isShort) {
        BinanceFutureLotSizeResponse lotSize = binanceExchangeInfoConfig.getLotSizeBySymbol(symbolConfig.getSymbol());
        Pair<BigDecimal, BigDecimal> calculatedLotSize = Calculator.calculateLotSize(lotSize, symbolConfig.getOrderVolume(),
                entryBar.getClosePrice().bigDecimalValue());
        return BinanceOrderResponse.builder()
                .exchange(symbolConfig.getExchangeName())
                .symbol(symbolConfig.getSymbol())
                .interval(BarDurationHelper.getEnumFromDuration(entryBar.getTimePeriod()).getValue())
                .orderId(KeyUtility.generateOrderId())
                .clientOrderId(KeyUtility.generateClientOrderId())
                .side(isShort ? SideEnum.SIDE_SELL.getValue() : SideEnum.SIDE_BUY.getValue())
                .positionSide(isShort ? PositionSideEnum.POSITION_SIDE_SHORT.getValue() : PositionSideEnum.POSITION_SIDE_LONG.getValue())
                .avgPrice(entryBar.getClosePrice().bigDecimalValue().toPlainString())
                .executedQuantity(calculatedLotSize.getLeft().toPlainString())
                .cumQuote(calculatedLotSize.getRight().toPlainString())
                .type(TypeOrderEnum.TYPE_ORDER_MARKET.getValue())
                .origType(TypeOrderEnum.TYPE_ORDER_MARKET.getValue())
                .transactTime(System.currentTimeMillis())
                .status("NEW")
                .barIndex(entryIndex)
                .build();
    }

    @Override
    public BaseOrderResponse exitOrder(BaseOrderResponse entryResponse, MyStrategyBaseBar exitBar,
                                       int exitIndex, SymbolConfig symbolConfig, boolean isShort) {
        BinanceFutureLotSizeResponse lotSize = binanceExchangeInfoConfig.getLotSizeBySymbol(symbolConfig.getSymbol());
        Pair<BigDecimal, BigDecimal> calculatedLotSize = Calculator.calculateLotSize(lotSize, symbolConfig.getOrderVolume(),
                exitBar.getClosePrice().bigDecimalValue());
        return BinanceOrderResponse.builder()
                .exchange(symbolConfig.getExchangeName())
                .symbol(symbolConfig.getSymbol())
                .interval(BarDurationHelper.getEnumFromDuration(exitBar.getTimePeriod()).getValue())
                .orderId(entryResponse.getOrderId())
                .clientOrderId(entryResponse.getClientOrderId())
                .side(isShort ? SideEnum.SIDE_BUY.getValue() : SideEnum.SIDE_SELL.getValue())
                .positionSide(entryResponse.getPositionSide())
                .avgPrice(exitBar.getClosePrice().bigDecimalValue().toPlainString())
                .executedQuantity(calculatedLotSize.getLeft().toPlainString())
                .cumQuote(calculatedLotSize.getRight().toPlainString())
                .type(TypeOrderEnum.TYPE_ORDER_MARKET.getValue())
                .origType(TypeOrderEnum.TYPE_ORDER_MARKET.getValue())
                .transactTime(System.currentTimeMillis())
                .status("CLOSED")
                .barIndex(exitIndex)
                .build();
    }

    @Override
    public boolean shouldStopOrder(String orderStorageRedisKey, BaseOrderResponse entryResponse,
                                   MyStrategyBaseBar exitBar, SymbolConfig symbolConfig, boolean isShort) {
        BinanceOrderResponse response = entryResponse.as(BinanceOrderResponse.class);
        return super.shouldStopOrder(orderStorageRedisKey, response.getAvgPriceAsBigDecimal(), exitBar.getClosePrice(),
                symbolConfig.getStopLoss(), symbolConfig.getTargetProfit(), isShort, response.getTransactTime());
    }
}
