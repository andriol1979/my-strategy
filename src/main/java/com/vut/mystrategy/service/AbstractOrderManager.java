package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.*;
import org.springframework.stereotype.Service;

@Service
public abstract class AbstractOrderManager {

    protected final RedisClientService redisClientService;
    private final OrderService orderService;

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
}
