package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.BarDurationHelper;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.service.strategy.MyStrategyBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;

@Slf4j
@Service
public class MyStrategyManager {

    private final RedisClientService redisClientService;
    private final AbstractOrderManager orderManager;

    @Value("${turn-on-long-strategy}")
    private boolean turnOnLongStrategy;
    @Value("${turn-on-short-strategy}")
    private boolean turnOnShortStrategy;

    @Autowired
    public MyStrategyManager(RedisClientService redisClientService,
                             AbstractOrderManager orderManager) {
        this.redisClientService = redisClientService;
        this.orderManager = orderManager;
    }

    @Async("myStrategyManagerAsync")
    public void runStrategy(BarSeries barSeries, TradingRecord tradingRecord,
                            MyStrategyBase myStrategyBase, SymbolConfig symbolConfig) {

        int endIndex = barSeries.getEndIndex(); // Lấy chỉ số của bar cuối cùng
        MyStrategyBaseBar newBar = (MyStrategyBaseBar) barSeries.getBar(endIndex); // lấy Bar của index cuối cùng
        //write log object Bar to debug
        LogMessage.printBarDebugMessage(log, endIndex, newBar, barSeries.getName());

        // Building the trading strategy - EMACrossOver
        //If you want to change strategy -> just need to replace your strategy here
        //----------------------------------------------------------------------------
        Strategy longStrategy = myStrategyBase.buildLongStrategy(barSeries, symbolConfig);
        Strategy shortStrategy = myStrategyBase.buildShortStrategy(barSeries, symbolConfig);

        String klineInterval = BarDurationHelper.getEnumFromDuration(newBar.getTimePeriod()).getValue();
        String orderStorageRedisKey = KeyUtility.getOrderResponseStorageRedisKey(symbolConfig.getExchangeName(),
                symbolConfig.getSymbol(), klineInterval);
        DecimalNum orderVolume = DecimalNum.valueOf(symbolConfig.getOrderVolume());
        if(redisClientService.exists(orderStorageRedisKey)) { // Đã có vị thế mở -> kiểm tra để đóng vị thế
            //Lấy OrderStorage từ redis để kiểm tra long - short
            OrderResponseStorage storage = redisClientService.getDataAsSingle(orderStorageRedisKey, OrderResponseStorage.class);
            boolean isShortEntry = orderManager.isShortEntry(storage);

            if (turnOnLongStrategy && !isShortEntry && longStrategy.shouldExit(endIndex)) {
                tradingRecord.exit(endIndex, newBar.getClosePrice(), orderVolume); // SELL để đóng long
                BaseOrderResponse response = orderManager.exitOrder(storage.getEntryResponse(), newBar,
                        endIndex, symbolConfig, false);
                orderManager.saveOrderResponse(response, symbolConfig);
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(),
                        SideEnum.SIDE_SELL, tradingRecord.getLastTrade(), false);
            }
            else if (turnOnShortStrategy && isShortEntry && shortStrategy.shouldExit(endIndex)) {
                tradingRecord.exit(endIndex, newBar.getClosePrice(), orderVolume); // BUY để đóng short
                BaseOrderResponse response = orderManager.exitOrder(storage.getEntryResponse(), newBar,
                        endIndex, symbolConfig, true);
                orderManager.saveOrderResponse(response, symbolConfig);
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(),
                        SideEnum.SIDE_BUY, tradingRecord.getLastTrade(), true);
            }
            //TODO: stop loss process will be implemented here
            //TODO: giải phóng các vị thế bị kẹt quá lâu
            if(orderManager.shouldStopOrder(orderStorageRedisKey,
                    storage.getEntryResponse(), newBar, symbolConfig, isShortEntry)) {
                tradingRecord.exit(endIndex, newBar.getClosePrice(), orderVolume);
                BaseOrderResponse response = orderManager.exitOrder(storage.getEntryResponse(), newBar,
                        endIndex, symbolConfig, false);
                orderManager.saveOrderResponse(response, symbolConfig);
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(),
                        isShortEntry ? SideEnum.SIDE_SELL : SideEnum.SIDE_BUY,
                        tradingRecord.getLastTrade(), isShortEntry);
            }
        }
        else { // Chưa có vị thế -> kiểm tra để mở vị thế
            if (turnOnLongStrategy && longStrategy.shouldEnter(endIndex)) {
                tradingRecord.enter(endIndex, newBar.getClosePrice(), orderVolume); // BUY để mở long
                BaseOrderResponse response = orderManager.placeOrder(newBar, endIndex, symbolConfig, false);
                orderManager.saveOrderResponse(response, symbolConfig);
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(),
                        SideEnum.SIDE_BUY, tradingRecord.getLastTrade(), false);
            }
            else if (turnOnShortStrategy && shortStrategy.shouldEnter(endIndex)) { // Điều kiện bán khống
                tradingRecord.enter(endIndex, newBar.getClosePrice(), orderVolume); // SELL để mở short
                BaseOrderResponse response = orderManager.placeOrder(newBar, endIndex, symbolConfig, true);
                orderManager.saveOrderResponse(response, symbolConfig);
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(),
                        SideEnum.SIDE_SELL, tradingRecord.getLastTrade(), true);
            }
        }
    }
}
