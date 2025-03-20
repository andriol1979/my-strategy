package com.vut.mystrategy.service.binance;

import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.BaseOrderResponse;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.TradeSignal;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.model.binance.TradeEvent;
import com.vut.mystrategy.service.AbstractOrderManager;
import com.vut.mystrategy.service.RedisClientService;
import com.vut.mystrategy.service.TradeEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@Profile("dev")
public class DevBinanceOrderManager implements AbstractOrderManager {
    private final BinanceOrderService binanceOrderService;
    private final RedisClientService redisClientService;
    private final TradeEventService tradeEventService;

    @Autowired
    public DevBinanceOrderManager(BinanceOrderService binanceOrderService,
                                  RedisClientService redisClientService,
                                  TradeEventService tradeEventService) {
        this.binanceOrderService = binanceOrderService;
        this.redisClientService = redisClientService;
        this.tradeEventService = tradeEventService;
    }

    @Override
    public BaseOrderResponse placeOrder(TradeSignal tradeSignal, SymbolConfig symbolConfig) {
        BinanceOrderResponse binanceOrderResponse = fakeOrderResponse(tradeSignal, symbolConfig);
        binanceOrderResponse.setStatus("NEW");
        binanceOrderService.createOrder(binanceOrderResponse, symbolConfig);

        return binanceOrderResponse;
    }

    @Override
    public BaseOrderResponse closeOrder(TradeSignal tradeSignal, SymbolConfig symbolConfig) {
        BinanceOrderResponse binanceOrderResponse = fakeOrderResponse(tradeSignal, symbolConfig);
        binanceOrderResponse.setStatus("CLOSED");
        binanceOrderService.updateOrder(binanceOrderResponse, symbolConfig);

        return binanceOrderResponse;
    }

    private BinanceOrderResponse fakeOrderResponse(TradeSignal tradeSignal, SymbolConfig symbolConfig) {
        String tradeEventRedisKey = KeyUtility.getTradeEventRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol());
        TradeEvent tradeEvent = redisClientService.getDataByIndex(tradeEventRedisKey, 0, TradeEvent.class);
        BigDecimal avgPrice = Calculator.calculatePriceWithSlippage(tradeEvent.getPriceAsBigDecimal(), symbolConfig.getSlippage(), tradeSignal.getSide());
        BinanceFutureLotSizeResponse lotSize = tradeEventService.getBinanceFutureLotSizeFilter(symbolConfig.getSymbol());
        BigDecimal executedQty = Calculator.calculateQuantity(lotSize, symbolConfig.getOrderVolume(), tradeEvent.getPriceAsBigDecimal());
        return BinanceOrderResponse.builder()
                .orderId(KeyUtility.generateOrderId())
                .clientOrderId(KeyUtility.generateClientOrderId())
                .symbol(symbolConfig.getSymbol())
                .side(tradeSignal.getSide())
                .positionSide(tradeSignal.getPositionSide())
                .avgPrice(avgPrice.toPlainString())
                .executedQuantity(executedQty.toPlainString())
                .cumQuote(symbolConfig.getOrderVolume().toPlainString())
                .updateTime(System.currentTimeMillis())
                .build();
    }
}
