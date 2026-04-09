package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.BarDurationHelper;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.service.order.AbstractOrderManager;
import com.vut.mystrategy.service.order.OrderManagerFactory;
import com.vut.mystrategy.service.strategy.DefensiveSpotDcaStrategy;
import com.vut.mystrategy.service.strategy.MyStrategyBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class MyStrategyManager {
    private static final int DEFENSIVE_SPOT_REENTRY_COOLDOWN_BARS = 24;

    private final RedisClientService redisClientService;
    private final OrderManagerFactory orderManagerFactory;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Value("${turn-on-long-strategy}")
    private boolean turnOnLongStrategy;
    @Value("${turn-on-short-strategy}")
    private boolean turnOnShortStrategy;

    @Autowired
    public MyStrategyManager(RedisClientService redisClientService,
                             OrderManagerFactory orderManagerFactory) {
        this.redisClientService = redisClientService;
        this.orderManagerFactory = orderManagerFactory;
    }

    @Async("myStrategyManagerAsync")
    public void runStrategy(BarSeries barSeries, TradingRecord tradingRecord,
                            MyStrategyBase myStrategyBase, SymbolConfig symbolConfig) {
        processStrategy(barSeries, tradingRecord, myStrategyBase, symbolConfig);
    }

    public void runStrategySync(BarSeries barSeries, TradingRecord tradingRecord,
                                MyStrategyBase myStrategyBase, SymbolConfig symbolConfig) {
        processStrategy(barSeries, tradingRecord, myStrategyBase, symbolConfig);
    }

    private void processStrategy(BarSeries barSeries, TradingRecord tradingRecord,
                                 MyStrategyBase myStrategyBase, SymbolConfig symbolConfig) {
        AbstractOrderManager orderManager = orderManagerFactory.getOrderManager(
                symbolConfig.getExchangeName(), activeProfile);
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
        if (myStrategyBase instanceof DefensiveSpotDcaStrategy defensiveSpotDcaStrategy) {
            processDefensiveSpotStrategy(barSeries, tradingRecord, symbolConfig, orderManager, newBar,
                    endIndex, orderStorageRedisKey, orderVolume, defensiveSpotDcaStrategy);
            return;
        }
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
                        endIndex, symbolConfig, isShortEntry);
                orderManager.saveOrderResponse(response, symbolConfig);
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(),
                        isShortEntry ? SideEnum.SIDE_BUY : SideEnum.SIDE_SELL,
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

    private void processDefensiveSpotStrategy(BarSeries barSeries, TradingRecord tradingRecord,
                                              SymbolConfig symbolConfig, AbstractOrderManager orderManager,
                                              MyStrategyBaseBar newBar, int endIndex, String orderStorageRedisKey,
                                              DecimalNum orderVolume, DefensiveSpotDcaStrategy strategy) {
        if (redisClientService.exists(orderStorageRedisKey)) {
            OrderResponseStorage storage = redisClientService.getDataAsSingle(orderStorageRedisKey, OrderResponseStorage.class);
            if (storage == null) {
                return;
            }

            strategy.refreshOpenPositionState(barSeries, symbolConfig, storage);
            String exitReason = strategy.getExitReason(barSeries, symbolConfig, storage);
            if (exitReason != null) {
                storage.setExitReason(exitReason);
                redisClientService.saveDataAsSingle(orderStorageRedisKey, storage);
                tradingRecord.exit(endIndex, newBar.getClosePrice(), orderVolume);
                BaseOrderResponse response = orderManager.exitOrder(storage.getEntryResponse(), newBar, endIndex, symbolConfig, false);
                orderManager.saveOrderResponse(response, symbolConfig);
                LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(),
                        SideEnum.SIDE_SELL, tradingRecord.getLastTrade(), false);
                log.info("Defensive spot exit at index {} because {}", endIndex, exitReason);
                return;
            }

            if (strategy.shouldAddToPosition(barSeries, symbolConfig, storage)) {
                BaseOrderResponse scaleInResponse = orderManager.placeOrder(newBar, endIndex, symbolConfig, false);
                mergeLongPosition(storage, scaleInResponse, endIndex);
                redisClientService.saveDataAsSingle(orderStorageRedisKey, storage);
                log.info("Defensive spot DCA at index {}. dcaCount={}, avgPrice={}, trailingActive={}",
                        endIndex, storage.getDcaCount(),
                        storage.getEntryResponse().as(com.vut.mystrategy.model.binance.BinanceOrderResponse.class).getAvgPrice(),
                        storage.getTrailingActive());
            } else {
                redisClientService.saveDataAsSingle(orderStorageRedisKey, storage);
            }
            return;
        }

        if (isCoolingDownAfterClosedPosition(tradingRecord, endIndex, symbolConfig)) {
            return;
        }

        boolean shouldOpenLong = turnOnLongStrategy && strategy.shouldOpenLong(barSeries, symbolConfig);
        if (!shouldOpenLong && endIndex % 250 == 0) {
            log.info("Defensive spot entry blocked at index {}: {}", endIndex, strategy.explainOpenLongDecision(barSeries, symbolConfig));
        }

        if (shouldOpenLong) {
            BaseOrderResponse response = orderManager.placeOrder(newBar, endIndex, symbolConfig, false);
            OrderResponseStorage storage = initializeLongStorage(response, endIndex, newBar);
            redisClientService.saveDataAsSingle(orderStorageRedisKey, storage);
            tradingRecord.enter(endIndex, newBar.getClosePrice(), orderVolume);
            LogMessage.printTradeDebugMessage(log, endIndex, newBar.getClosePrice(),
                    SideEnum.SIDE_BUY, tradingRecord.getLastTrade(), false);
            log.info("Defensive spot entry at index {} using {}", endIndex, strategy.getStrategySummary(symbolConfig));
        }
    }

    private OrderResponseStorage initializeLongStorage(BaseOrderResponse response, int entryIndex, MyStrategyBaseBar newBar) {
        OrderResponseStorage storage = new OrderResponseStorage();
        storage.setEntryResponse(response);
        storage.setDcaCount(0);
        storage.setInitialEntryIndex(entryIndex);
        storage.setLastEntryIndex(entryIndex);
        storage.setInitialEntryTime(System.currentTimeMillis());
        storage.setLastEntryTime(System.currentTimeMillis());
        storage.setPeakPrice(new BigDecimal(newBar.getClosePrice().toString()));
        storage.setTrailingActive(false);
        storage.setTrailingStopPrice(null);
        storage.setExitReason(null);
        storage.setSoftBreakStartIndex(null);
        return storage;
    }

    private void mergeLongPosition(OrderResponseStorage storage, BaseOrderResponse scaleInResponse, int endIndex) {
        com.vut.mystrategy.model.binance.BinanceOrderResponse aggregatedEntry = storage.getEntryResponse()
                .as(com.vut.mystrategy.model.binance.BinanceOrderResponse.class);
        com.vut.mystrategy.model.binance.BinanceOrderResponse incrementalEntry = scaleInResponse
                .as(com.vut.mystrategy.model.binance.BinanceOrderResponse.class);

        BigDecimal currentQuantity = aggregatedEntry.getExecutedQtyAsBigDecimal();
        BigDecimal addedQuantity = incrementalEntry.getExecutedQtyAsBigDecimal();
        BigDecimal totalQuantity = currentQuantity.add(addedQuantity);

        BigDecimal currentCost = aggregatedEntry.getCumQuoteAsBigDecimal();
        BigDecimal addedCost = incrementalEntry.getCumQuoteAsBigDecimal();
        BigDecimal totalCost = currentCost.add(addedCost);
        BigDecimal averagePrice = totalCost.divide(totalQuantity, 8, RoundingMode.HALF_UP);

        aggregatedEntry.setExecutedQuantity(totalQuantity.toPlainString());
        aggregatedEntry.setCumQuote(totalCost.toPlainString());
        aggregatedEntry.setAvgPrice(averagePrice.toPlainString());

        storage.setEntryResponse(aggregatedEntry);
        storage.setDcaCount(storage.getDcaCount() == null ? 1 : storage.getDcaCount() + 1);
        storage.setLastEntryIndex(endIndex);
        storage.setLastEntryTime(System.currentTimeMillis());
        storage.setSoftBreakStartIndex(null);
    }

    private boolean isCoolingDownAfterClosedPosition(TradingRecord tradingRecord, int currentIndex, SymbolConfig symbolConfig) {
        Position lastPosition = tradingRecord.getLastPosition();
        if (lastPosition == null || !lastPosition.isClosed() || lastPosition.getExit() == null) {
            return false;
        }
        int cooldownBars = symbolConfig.getReentryCooldownBars() == null
                ? DEFENSIVE_SPOT_REENTRY_COOLDOWN_BARS
                : symbolConfig.getReentryCooldownBars();
        return currentIndex - lastPosition.getExit().getIndex() < cooldownBars;
    }
}
