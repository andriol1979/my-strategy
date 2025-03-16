package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class TradingSignalMonitor {

    private final RedisClientService redisClientService;
    private final TradingConfigManager tradingConfigManager;

    @Autowired
    public TradingSignalMonitor(RedisClientService redisClientService,
                                TradingConfigManager tradingConfigManager) {
        this.redisClientService = redisClientService;
        this.tradingConfigManager = tradingConfigManager;
    }

    @Async("monitorTradingSignalAsync")
    public void monitorTradingSignal(String exchangeName, String symbol, BigDecimal marketPrice) {
        TradingConfig tradingConfig =  tradingConfigManager.getActiveConfigBySymbol(exchangeName, symbol).orElseThrow();
        //Gen redis keys
        String smaTrendRedisKey = KeyUtility.getSmaTrendRedisKey(exchangeName, symbol);
        String volumeTrendRedisKey = KeyUtility.getVolumeTrendRedisKey(exchangeName, symbol);
        String shortEmaPriceRedisKey = KeyUtility.getShortEmaPriceRedisKey(exchangeName, symbol);
        String longEmaPriceRedisKey = KeyUtility.getLongEmaPriceRedisKey(exchangeName, symbol);

        //Collect data
        SmaTrend smaTrend = redisClientService.getDataAsSingle(smaTrendRedisKey, SmaTrend.class);
        VolumeTrend volumeTrend = redisClientService.getDataAsSingle(volumeTrendRedisKey, VolumeTrend.class);
        // get 2 short EMA
        List<EmaPrice> shortEmaPricesList = redisClientService.getDataList(shortEmaPriceRedisKey, 0, 1, EmaPrice.class);
        EmaPrice longEmaPrice = redisClientService.getDataByIndex(longEmaPriceRedisKey, 0, EmaPrice.class);
        if(smaTrend == null || volumeTrend == null ||
                shortEmaPricesList == null || shortEmaPricesList.size() < 2 || longEmaPrice == null) {
            log.info("Not enough data to monitor trading signal");
            return;
        }
        EmaPrice shortCurrEmaPrice = shortEmaPricesList.get(0);
        EmaPrice shortPrevEmaPrice = shortEmaPricesList.get(1);
        //Get or new Trading Signal
        boolean newTradingSignal = false;
        String tradingSignalRedisKey = KeyUtility.getTradingSignalRedisKey(exchangeName, symbol);
        TradeSignal tradeSignal = redisClientService.getDataAsSingle(tradingSignalRedisKey, TradeSignal.class);
        if(tradeSignal == null) {
            newTradingSignal = true;
            tradeSignal = TradeSignal.builder()
                    .exchangeName(exchangeName)
                    .symbol(symbol)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        //Check entry LONG: EMA(5) cắt lên EMA(10) (Bullish crossover).
        int bullishSignal = Calculator.isBullishCrossOver(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), longEmaPrice.getPrice(), tradingConfig.getEmaThreshold());
        int bearishSignal = Calculator.isBearishCrossOver(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), longEmaPrice.getPrice(), tradingConfig.getEmaThreshold());
        int volumeTrendStrengthPoint = Calculator.analyzeVolumeTrendStrengthPoint(volumeTrend);
        int priceNearResistanceOrSupport = Calculator.analyzePriceNearResistanceOrSupport(marketPrice,
                smaTrend.getResistancePrice(), smaTrend.getSupportPrice(), tradingConfig.getPriceThreshold(),
                bullishSignal >= 1, bearishSignal >= 1);
        log.info("BullishCrossOver: {}, BearishCrossover: {}, VolumeTrendStrengthPoint: {}, PriceNearResistanceOrSupport: {}",
                bullishSignal, bearishSignal, volumeTrendStrengthPoint, priceNearResistanceOrSupport);


        int minStrengthThreshold = 5;
        boolean volumeSignalBullish = volumeTrendStrengthPoint >= minStrengthThreshold &&
                volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.UP.getValue());
        boolean volumeSignalBearish = volumeTrendStrengthPoint >= minStrengthThreshold &&
                volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.DOWN.getValue());
        //Breakout strategy
        if(bullishSignal >= 1 && volumeSignalBullish && priceNearResistanceOrSupport == 1) {
            //ENTRY LONG
            tradeSignal.setSide(SideEnum.SIDE_BUY.getValue());
            tradeSignal.setPositionSide(PositionSideEnum.POSITION_SIDE_LONG.getValue());
            tradeSignal.setPrice(marketPrice);
            tradeSignal.setStopLoss(smaTrend.getSupportPrice());
            tradeSignal.setTakeProfit(smaTrend.getResistancePrice());
            tradeSignal.setAction("ENTRY-LONG");
        }
        else if(bearishSignal >= 1 && volumeSignalBearish && priceNearResistanceOrSupport == 1) {
            //EXIT LONG
            tradeSignal.setSide(SideEnum.SIDE_SELL.getValue());
            tradeSignal.setPositionSide(PositionSideEnum.POSITION_SIDE_LONG.getValue());
            tradeSignal.setPrice(marketPrice);
            tradeSignal.setStopLoss(smaTrend.getSupportPrice());
            tradeSignal.setTakeProfit(smaTrend.getResistancePrice());
            tradeSignal.setAction("EXIT-LONG");
        }
        else if(bearishSignal >= 1 && volumeSignalBearish && priceNearResistanceOrSupport == 2) {
            //ENTRY SHORT
            tradeSignal.setSide(SideEnum.SIDE_SELL.getValue());
            tradeSignal.setPositionSide(PositionSideEnum.POSITION_SIDE_SHORT.getValue());
            tradeSignal.setPrice(marketPrice);
            tradeSignal.setStopLoss(smaTrend.getResistancePrice());
            tradeSignal.setTakeProfit(smaTrend.getSupportPrice());
            tradeSignal.setAction("ENTRY-SHORT");
        }
        else if(bullishSignal >= 1 && volumeSignalBullish && priceNearResistanceOrSupport == 2) {
            //EXIT SHORT
            tradeSignal.setSide(SideEnum.SIDE_BUY.getValue());
            tradeSignal.setPositionSide(PositionSideEnum.POSITION_SIDE_SHORT.getValue());
            tradeSignal.setPrice(marketPrice);
            tradeSignal.setStopLoss(smaTrend.getResistancePrice());
            tradeSignal.setTakeProfit(smaTrend.getSupportPrice());
            tradeSignal.setAction("EXIT-SHORT");
        }

        //còn thiếu long bounce & short reversal strategy

        log.info("TradingSignalMonitor: {}", tradeSignal);
        // save to redis
        if(!StringUtils.isEmpty(tradeSignal.getAction())) {
            redisClientService.saveDataAsSingle(tradingSignalRedisKey, tradeSignal);
            if(newTradingSignal) {
                LogMessage.printInsertRedisLogMessage(log, tradingSignalRedisKey, tradeSignal);
            }
            else {
                LogMessage.printUpdateRedisLogMessage(log, tradingSignalRedisKey, tradeSignal);
            }
        }
    }
}
