package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.DataFetcher;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExitLongSignalMonitor extends AbstractSignalMonitor {

    @Autowired
    public ExitLongSignalMonitor(RedisClientService redisClientService,
                                 @Qualifier("dataFetchersMap") Map<String, DataFetcher> dataFetchersMap) {
        super(redisClientService, dataFetchersMap);
    }

    @Async("monitorExitLongSignalAsync")
    @Override
    public void monitorSignal(DataFetcher dataFetcher) {
        if(dataFetcher.getMarketData() == null) {
            log.warn("MarketData is null in DataFetcher - Exchange {} - Symbol {}",
                    dataFetcher.getSymbolConfig().getExchangeName(), dataFetcher.getSymbolConfig().getSymbol());
            return;
        }

        //Collect data
        TradeEvent tradeEvent = dataFetcher.getMarketData().getTradeEvent();
        SmaTrend smaTrend = dataFetcher.getMarketData().getSmaTrend();
        VolumeTrend volumeTrend = dataFetcher.getMarketData().getVolumeTrend();
        // get 2 short EMA
        List<EmaPrice> shortEmaPricesList = dataFetcher.getMarketData().getShortEmaPricesList();
        EmaPrice longEmaPrice = dataFetcher.getMarketData().getLongEmaPrice();
        EmaPrice shortCurrEmaPrice = shortEmaPricesList.get(0);
        EmaPrice shortPrevEmaPrice = shortEmaPricesList.get(1);

        int bearishSignal = Calculator.isBearishCrossOver(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), longEmaPrice.getPrice(), dataFetcher.getSymbolConfig().getEmaThreshold());
        int volumeTrendStrengthPoint = Calculator.analyzeVolumeTrendStrengthPoint(volumeTrend);
        int minStrengthThreshold = 5;
        boolean volumeSignalBearish = volumeTrendStrengthPoint >= minStrengthThreshold &&
                volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.DOWN.getValue());
        log.info("BearishSignal = {}, VolumeTrendStrengthPoint = {}, VolumeSignalBearish = {}",
                bearishSignal, volumeTrendStrengthPoint, volumeSignalBearish);

        if(bearishSignal >= 1 && volumeSignalBearish) {
            //EXIT LONG
            TradeSignal tradeSignal = TradeSignal.builder()
                    .exchangeName(dataFetcher.getSymbolConfig().getExchangeName())
                    .symbol(dataFetcher.getSymbolConfig().getSymbol())
                    .side(SideEnum.SIDE_SELL.getValue())
                    .positionSide(PositionSideEnum.POSITION_SIDE_LONG.getValue())
                    .price(tradeEvent.getPriceAsBigDecimal())
                    .stopLoss(smaTrend.getSupportPrice())
                    .takeProfit(smaTrend.getResistancePrice())
                    .action("EXIT-LONG")
                    .timestamp(System.currentTimeMillis())
                    .build();
            // save to redis -> or trigger API to order SELL - LONG
            // Always create new trading signal and save to redis (new or override)
            String exitLongSignalRedisKey = KeyUtility.getExitLongSignalRedisKey(
                    dataFetcher.getSymbolConfig().getExchangeName(),
                    dataFetcher.getSymbolConfig().getSymbol());
            redisClientService.saveDataAsSingle(exitLongSignalRedisKey, tradeSignal);
            LogMessage.printInsertRedisLogMessage(log, exitLongSignalRedisKey, tradeSignal);
            return;
        }
        log.info("Not found EXIT LONG signal. The condition does NOT match");
    }
}
