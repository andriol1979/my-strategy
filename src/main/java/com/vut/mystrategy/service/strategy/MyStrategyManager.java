package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.MyStrategyBaseBar;
import com.vut.mystrategy.model.PositionSideEnum;
import com.vut.mystrategy.model.SideEnum;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.AbstractOrderManager;
import com.vut.mystrategy.service.RedisClientService;
import com.vut.mystrategy.service.binance.BinanceOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;

@Slf4j
@Service
public class MyStrategyManager {

    private final RedisClientService redisClientService;
    private final BinanceOrderService orderService;
    private final AbstractOrderManager orderManager;

    @Autowired
    public MyStrategyManager(RedisClientService redisClientService,
                             BinanceOrderService orderService,
                             AbstractOrderManager orderManager) {
        this.redisClientService = redisClientService;
        this.orderService = orderService;
        this.orderManager = orderManager;
    }

    @Async("myStrategyManagerAsync")
    public void runStrategy(BarSeries barSeries, TradingRecord tradingRecord,
                            MyStrategyBase myStrategyBase, SymbolConfig symbolConfig) {
        //Turn on - off short for testing purpose
        boolean turnOnLongStrategy = true;
        boolean turnOnShortStrategy = true;
        // Building the trading strategy - EMACrossOver
        //If you want to change strategy -> just need to replace your strategy here
        //----------------------------------------------------------------------------
        Strategy longStrategy = myStrategyBase.buildLongStrategy(barSeries, symbolConfig);
        Strategy shortStrategy = myStrategyBase.buildShortStrategy(barSeries, symbolConfig);

        //----------------------------------------------------------------------------
        // Running the strategy
        String redisKey = KeyUtility.getShortOrderFlag(symbolConfig.getExchangeName(), symbolConfig.getSymbol(),
                myStrategyBase.getClass().getSimpleName());
        boolean isShort = redisClientService.exists(redisKey)
                ? redisClientService.getDataAsSingle(redisKey, Boolean.class)
                : false;
        boolean isShortAtEntryIndex = isShort;

        int endIndex = barSeries.getEndIndex(); // Lấy chỉ số của bar cuối cùng
        MyStrategyBaseBar newBar = (MyStrategyBaseBar) barSeries.getBar(endIndex); // lấy Bar của index cuối cùng

        //Find market trend
//        String trendDetect = new TrendDetector(barSeries).detectTrend(endIndex, DecimalNum.valueOf(0.0005));
//        log.info("++++++++++++ Trend detected: {}", trendDetect);

        //write log object Bar to debug
        LogMessage.printBarDebugMessage(log, endIndex, newBar, barSeries.getName());
        DecimalNum orderVolume = DecimalNum.valueOf(symbolConfig.getOrderVolume());
        if (!tradingRecord.isClosed()) { // Đã có vị thế mở
            if (turnOnShortStrategy && isShort && shortStrategy.shouldExit(endIndex)) {
                tradingRecord.exit(endIndex, newBar.getClosePrice(), orderVolume); // BUY để đóng short
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(), SideEnum.SIDE_BUY,
                        PositionSideEnum.POSITION_SIDE_SHORT, tradingRecord.getLastTrade());
                isShort = false;
            }
            else if (turnOnLongStrategy && !isShort && longStrategy.shouldExit(endIndex)) {
                tradingRecord.exit(endIndex, newBar.getClosePrice(), orderVolume); // SELL để đóng long
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(), SideEnum.SIDE_SELL,
                        PositionSideEnum.POSITION_SIDE_LONG, tradingRecord.getLastTrade());
            }
        }
        else { // Chưa có vị thế
            if (turnOnShortStrategy && shortStrategy.shouldEnter(endIndex)) { // Điều kiện bán khống
                tradingRecord.enter(endIndex, newBar.getClosePrice(), orderVolume); // SELL để mở short
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(), SideEnum.SIDE_SELL,
                        PositionSideEnum.POSITION_SIDE_SHORT, tradingRecord.getLastTrade());
                isShort = true;
            }
            else if (turnOnLongStrategy && longStrategy.shouldEnter(endIndex)) {
                tradingRecord.enter(endIndex, newBar.getClosePrice(), orderVolume); // BUY để mở long
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(), SideEnum.SIDE_BUY,
                        PositionSideEnum.POSITION_SIDE_LONG, tradingRecord.getLastTrade());
                isShort = false;
            }
        }
        redisClientService.saveDataAsSingle(redisKey, isShort);
        LogMessage.printStrategyAnalysis(log, barSeries,tradingRecord);

        //Save closed order to database
        if(tradingRecord.isClosed() && tradingRecord.getTrades().size() > 1) {
            buildAndSaveOrder(tradingRecord, symbolConfig, isShortAtEntryIndex);
        }
    }

    private void buildAndSaveOrder(TradingRecord tradingRecord, SymbolConfig symbolConfig, boolean isShort) {
        //save entryLongOrderRedisKey
        Order order = orderService.buildOrder(tradingRecord, symbolConfig, isShort);
        if(order != null) {
            orderService.saveOrderToDb(order);
        }
    }
}
