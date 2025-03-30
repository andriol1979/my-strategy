package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.RedisClientService;
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

    @Autowired
    public MyStrategyManager(RedisClientService redisClientService) {
        this.redisClientService = redisClientService;
    }

    @Async("myStrategyManagerAsync")
    public void runStrategy(BarSeries barSeries, TradingRecord tradingRecord,
                            MyStrategyBase myStrategyBase, SymbolConfig symbolConfig) {
        // Building the trading strategy - EMACrossOver
        //If you want to change strategy -> just need to replace your strategy here
        //----------------------------------------------------------------------------
        Strategy longStrategy = myStrategyBase.buildLongStrategy(barSeries);
        Strategy shortStrategy = myStrategyBase.buildShortStrategy(barSeries);

        //----------------------------------------------------------------------------
        // Running the strategy
        String redisKey = symbolConfig.getExchangeName() + "_" + symbolConfig.getSymbol();
        boolean isShort = redisClientService.exists(redisKey)
                ? redisClientService.getDataAsSingle(redisKey, Boolean.class)
                : false;

        int endIndex = barSeries.getEndIndex(); // Lấy chỉ số của bar cuối cùng
        Bar newBar = barSeries.getBar(endIndex); // lấy Bar của index cuối cùng
        DecimalNum orderVolume = DecimalNum.valueOf(symbolConfig.getOrderVolume());
        if (!tradingRecord.isClosed()) { // Đã có vị thế mở
            if (isShort && shortStrategy.shouldExit(endIndex)) {
                tradingRecord.exit(endIndex, newBar.getClosePrice(), orderVolume); // BUY để đóng short
                log.info("Close SHORT: BUY at index: {} - Price: {} - Trade: {}", endIndex, newBar.getClosePrice(),
                        tradingRecord.getLastTrade());
                isShort = false;
            }
            else if (!isShort && longStrategy.shouldExit(endIndex)) {
                tradingRecord.exit(endIndex, newBar.getClosePrice(), orderVolume); // SELL để đóng long
                log.info("Close LONG: SELL at index: {} - Price: {} - Trade: {}", endIndex, newBar.getClosePrice(),
                        tradingRecord.getLastTrade());
            }
        }
        else { // Chưa có vị thế
            if (shortStrategy.shouldEnter(endIndex)) { // Điều kiện bán khống (mày tự định nghĩa)
                tradingRecord.enter(endIndex, newBar.getClosePrice(), orderVolume); // SELL để mở short
                log.info("Open SHORT: SELL at index: {} - Price: {} - Trade: {}", endIndex, newBar.getClosePrice(),
                        tradingRecord.getLastTrade());
                isShort = true;
            }
            else if (longStrategy.shouldEnter(endIndex)) {
                tradingRecord.enter(endIndex, newBar.getClosePrice(), orderVolume); // BUY để mở long
                log.info("Open LONG: BUY at index: {} - Price: {} - Trade: {}", endIndex, newBar.getClosePrice(),
                        tradingRecord.getLastTrade());
                isShort = false;
            }
        }
        redisClientService.saveDataAsSingle(redisKey, isShort);
        LogMessage.printStrategyAnalysis(log, barSeries,tradingRecord);
    }
}
