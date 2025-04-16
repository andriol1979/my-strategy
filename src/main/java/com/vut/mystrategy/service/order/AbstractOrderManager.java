package com.vut.mystrategy.service.order;

import com.vut.mystrategy.component.binance.BinanceFutureRestApiClient;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.service.OrderService;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;

@Slf4j
public abstract class AbstractOrderManager {

    protected final RedisClientService redisClientService;
    protected final BinanceFutureRestApiClient binanceFutureRestApiClient;

    private final OrderService orderService;
    private final static long durationInMillis = 4 * 60 * 1000;

    public AbstractOrderManager(RedisClientService redisClientService,
                                BinanceFutureRestApiClient binanceFutureRestApiClient,
                                OrderService orderService) {
        this.redisClientService = redisClientService;
        this.binanceFutureRestApiClient = binanceFutureRestApiClient;
        //private service -> use only in parent class
        this.orderService = orderService;
    }

    //call API to place order
    public abstract BaseOrderResponse placeOrder(MyStrategyBaseBar entryBar, int entryIndex,
                                                 SymbolConfig symbolConfig, boolean isShort);
    //call API to close order
    public abstract BaseOrderResponse exitOrder(BaseOrderResponse entryResponse, MyStrategyBaseBar exitBar,
                                                int exitIndex, SymbolConfig symbolConfig, boolean isShort);

    //Should implement to stop order manually
    // stopLoss or stuck order
    public abstract boolean shouldStopOrder(String orderStorageRedisKey, BaseOrderResponse entryResponse,
                                   MyStrategyBaseBar exitBar, SymbolConfig symbolConfig, boolean isShort);

    public void saveOrderResponse(BaseOrderResponse response, SymbolConfig symbolConfig) {
        String redisKey = KeyUtility.getOrderResponseStorageRedisKey(response.getExchange(), response.getSymbol(), response.getInterval());
        //Check entry or exit long/sell
        //isEntry = BUY - LONG || SELL - SHORT
        boolean isEntry =
                (response.getSide().equals(SideEnum.SIDE_BUY.getValue()) &&
                response.getPositionSide().equals(PositionSideEnum.POSITION_SIDE_LONG.getValue()))
                ||
                (response.getSide().equals(SideEnum.SIDE_SELL.getValue()) &&
                response.getPositionSide().equals(PositionSideEnum.POSITION_SIDE_SHORT.getValue()));

        if (isEntry) {
            OrderResponseStorage orderResponseStorage = new OrderResponseStorage();
            orderResponseStorage.setEntryResponse(response);
            redisClientService.saveDataAsSingle(redisKey, orderResponseStorage);
        }
        else {
            OrderResponseStorage orderResponseStorage = redisClientService.getDataAndDeleteAsSingle(redisKey, OrderResponseStorage.class);
            orderResponseStorage.setExitResponse(response);

            //Call Order Service to store closed order to postgres
            orderService.buildAndSaveOrder(orderResponseStorage, symbolConfig);
        }
    }

    public boolean isShortEntry(OrderResponseStorage storage) {
        BaseOrderResponse orderResponse = storage.getEntryResponse();
        if(orderResponse == null) {
            throw new RuntimeException("Entry Response is null");
        }
        return (orderResponse.getSide().equals(SideEnum.SIDE_SELL.getValue()) &&
                orderResponse.getPositionSide().equals(PositionSideEnum.POSITION_SIDE_SHORT.getValue()));
    }

    protected boolean shouldStopOrder(String orderStorageRedisKey, BigDecimal avgPrice, Num currentPrice,
                                      double stopLoss, double targetProfit,
                                      boolean isShort, long transactionTime) {
        if(!redisClientService.exists(orderStorageRedisKey)) {
            //vị thế đã được đóng bởi điều kiện shouldExit hoặc chưa mở
            // không cần kiểm tra stop
            return false;
        }
        boolean isReachStopLoss = isReachStopLoss(avgPrice, stopLoss, currentPrice, isShort);
        boolean isReachTakeProfit = isReachTakeProfit(avgPrice, targetProfit, currentPrice, isShort);
        boolean isStuck = isStuckOrder(transactionTime);
        log.info("isReachStopLoss: {} - isReachTakeProfit: {} - isStuck: {}", isReachStopLoss, isReachTakeProfit, isStuck);
        return isReachStopLoss || isReachTakeProfit || isStuck;
    }

    private boolean isReachStopLoss(BigDecimal entryPrice, Double stopLoss, Num currentPrice, boolean isShort) {
        BigDecimal stopLossPrice;
        if(isShort) {
            /*
             SHORT:
             entryPrice = 28000 - currentPrice = 283000 (giá tăng) - stopLoss = 0.01 (1%)
             => stopLossPrice = 28000 * (1 + 0.01) = 28280
             => currentPrice >= stopLossPrice => true
            */
            stopLossPrice = entryPrice.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(stopLoss)));
            return currentPrice.isGreaterThanOrEqual(DecimalNum.valueOf(stopLossPrice));
        }
        else {
            /*
             LONG:
             entryPrice = 28000 - currentPrice = 276000 (giá giảm) - stopLoss = 0.01 (1%)
             => stopLossPrice = 28000 * (1 - 0.01) = 27720
             => currentPrice <= stopLossPrice => true
            */
            stopLossPrice = entryPrice.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(stopLoss)));
            return currentPrice.isLessThanOrEqual(DecimalNum.valueOf(stopLossPrice));
        }
    }

    private boolean isReachTakeProfit(BigDecimal entryPrice, Double takeProfit, Num currentPrice, boolean isShort) {
        BigDecimal targetProfitPrice;
        if(isShort) {
            /*
             SHORT:
             entryPrice = 28000 - currentPrice = 26500 (giá giảm) - takeProfit = 0.05 (5%)
             => targetProfitPrice = 28000 * (1 - 0.05) = 26600
             => currentPrice <= targetProfitPrice => true
            */
            targetProfitPrice = entryPrice.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(takeProfit)));
            return currentPrice.isLessThanOrEqual(DecimalNum.valueOf(targetProfitPrice));
        }
        else {
            /*
             LONG:
             entryPrice = 28000 - currentPrice = 29600 (giá tăng) - takeProfit = 0.05 (5%)
             => targetProfitPrice = 28000 * (1 + 0.05) = 29400
             => currentPrice >= targetProfitPrice => true
            */
            targetProfitPrice = entryPrice.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(takeProfit)));
            return currentPrice.isGreaterThanOrEqual(DecimalNum.valueOf(targetProfitPrice));
        }
    }

    private boolean isStuckOrder(Long entryTransactionTime) {
        boolean isWithinDuration = Utility.isWithinDuration(entryTransactionTime,
                System.currentTimeMillis(), durationInMillis);
        return !isWithinDuration;
    }
}
