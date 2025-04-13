package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.*;
import org.springframework.stereotype.Service;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Service
public abstract class AbstractOrderManager {

    protected final RedisClientService redisClientService;
    private final OrderService orderService;
    private final static long durationInMillis = 4 * 60 * 1000;

    public AbstractOrderManager(RedisClientService redisClientService,
                                OrderService orderService) {
        this.redisClientService = redisClientService;
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

    protected boolean isReachStopLoss(BigDecimal entryPrice, Double stopLoss, Num currentPrice, boolean isShort) {
        //Check giá entry * stopLossThreshold
        BigDecimal stopLossPrice = entryPrice.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(stopLoss)));
        if(isShort) {
            // SHORT: giá hiện tại >= giá stop loss. Vd: 28000 > 27500
            return currentPrice.isGreaterThanOrEqual(DecimalNum.valueOf(stopLossPrice));
        }
        // LONG: giá hiện tại <= giá stop loss. Vd: 28000 < 28500
        return currentPrice.isLessThanOrEqual(DecimalNum.valueOf(stopLossPrice));
    }

    protected boolean isStuckOrder(Long entryTransactionTime, ZonedDateTime barEndTime) {
        long currentTime = Utility.getEpochMilliFromZonedDateTime(barEndTime);
        boolean isWithinDuration = Utility.isWithinDuration(entryTransactionTime, currentTime, durationInMillis);
        return !isWithinDuration;
    }
}
