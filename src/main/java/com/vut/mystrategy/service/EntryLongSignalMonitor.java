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
public class EntryLongSignalMonitor extends AbstractSignalMonitor {

    @Autowired
    public EntryLongSignalMonitor(RedisClientService redisClientService,
                                  @Qualifier("dataFetchersMap") Map<String, DataFetcher> dataFetchersMap) {
        super(redisClientService, dataFetchersMap);
    }

    @Async("monitorEntryLongSignalAsync")
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

        //Check entry LONG: EMA(5) cắt lên EMA(10) (Bullish crossover).
        int bullishSignal = Calculator.isBullishCrossOver(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), longEmaPrice.getPrice(), dataFetcher.getSymbolConfig().getEmaThreshold());
        int volumeTrendStrengthPoint = Calculator.analyzeVolumeTrendStrengthPoint(volumeTrend);
        int minStrengthThreshold = 5;
        boolean volumeSignalBullish = volumeTrendStrengthPoint >= minStrengthThreshold &&
                volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.UP.getValue());
        log.info("BullishSignal = {}, VolumeTrendStrengthPoint = {}, volumeSignalBullish = {}",
                bullishSignal, volumeTrendStrengthPoint, volumeSignalBullish);
        //Breakout strategy
        if(bullishSignal >= 1 && volumeSignalBullish) {
            //ENTRY LONG
            TradeSignal tradeSignal = TradeSignal.builder()
                    .exchangeName(dataFetcher.getSymbolConfig().getExchangeName())
                    .symbol(dataFetcher.getSymbolConfig().getSymbol())
                    .side(SideEnum.SIDE_BUY.getValue())
                    .positionSide(PositionSideEnum.POSITION_SIDE_LONG.getValue())
                    .price(tradeEvent.getPriceAsBigDecimal())
                    .stopLoss(smaTrend.getSupportPrice())
                    .takeProfit(smaTrend.getResistancePrice())
                    .action("ENTRY-LONG")
                    .timestamp(System.currentTimeMillis())
                    .build();
            // save to redis -> or trigger API to order BUY - LONG
            // Always create new trading signal and save to redis (new or override)
            String entryLongSignalRedisKey = KeyUtility.getEntryLongSignalRedisKey(
                    dataFetcher.getSymbolConfig().getExchangeName(),
                    dataFetcher.getSymbolConfig().getSymbol());
            redisClientService.saveDataAsSingle(entryLongSignalRedisKey, tradeSignal);
            LogMessage.printInsertRedisLogMessage(log, entryLongSignalRedisKey, tradeSignal);
            return;
        }
        log.info("Not found ENTRY LONG signal. The condition does NOT match");
    }
}
