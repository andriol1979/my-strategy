package com.vut.mystrategy.service;

import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class TradingSignalAnalyzer {
    /*
    common logic to analyze how to find:
        - entry long
        - exit long
        - entry short
        - exit short
     */

    private final int MIN_VOLUME_STRENGTH_THRESHOLD = 4;

    public boolean isEntryLong(MarketData marketData, SymbolConfig symbolConfig) {
        EmaPrice shortCurrEmaPrice = marketData.getShortEmaPricesList().get(0);
        EmaPrice shortPrevEmaPrice = marketData.getShortEmaPricesList().get(1);

        int bullishSignal = Calculator.isBullishTrend(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), marketData.getLongEmaPrice().getPrice(), symbolConfig.getEmaThreshold());
        int volumeTrendStrengthPoint = Calculator.analyzeVolumeTrendStrengthPoint(marketData.getVolumeTrend());
        boolean volumeTrendUp = marketData.getVolumeTrend().getCurrTrendDirection().equals(VolumeTrendEnum.UP.getValue());
        boolean volumeSignalBullish = volumeTrendStrengthPoint >= MIN_VOLUME_STRENGTH_THRESHOLD && volumeTrendUp;

        if(bullishSignal >= 3 && volumeTrendUp) {
            log.info("ENTRY-LONG detected. BullishSignal={}", bullishSignal);
            return true;
        }
        if(bullishSignal >= 2 && volumeSignalBullish) {
            log.info("ENTRY-LONG detected. BullishSignal={}, VolumeTrendStrengthPoint={}", bullishSignal, volumeTrendStrengthPoint);
            return true;
        }
        if(bullishSignal >= 1 && volumeSignalBullish && smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig)) {
            log.info("ENTRY-LONG detected. BullishSignal={}, VolumeTrendStrengthPoint={}, Market price={}",
                    bullishSignal, volumeTrendStrengthPoint, marketData.getTradeEvent().getPriceAsBigDecimal());
            return true;
        }
        boolean priceIsNearSupport = priceIsNearSupport(marketData.getSmaTrend(), marketData.getTradeEvent().getPriceAsBigDecimal(), symbolConfig.getEmaThreshold());
        boolean priceIsUpOverResistance = priceIsUpOverResistance(marketData.getSmaTrend(), marketData.getTradeEvent().getPriceAsBigDecimal(), symbolConfig.getEmaThreshold());
        if(volumeSignalBullish && (priceIsNearSupport || priceIsUpOverResistance)) {
            log.info("ENTRY-LONG detected. VolumeTrendStrengthPoint={}, Market price={}, PriceIsNearSupport={}, PriceIsUpOverResistance={}",
                    volumeTrendStrengthPoint, marketData.getTradeEvent().getPriceAsBigDecimal(), priceIsNearSupport, priceIsUpOverResistance);
            return true;
        }
        log.info("Not found ENTRY-LONG signal. The condition does NOT match");
        return false;
    }

    public boolean isExitLong(MarketData marketData, SymbolConfig symbolConfig) {
        EmaPrice shortCurrEmaPrice = marketData.getShortEmaPricesList().get(0);
        EmaPrice shortPrevEmaPrice = marketData.getShortEmaPricesList().get(1);

        int bearishSignal = Calculator.isBearishTrend(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), marketData.getLongEmaPrice().getPrice(), symbolConfig.getEmaThreshold());
        int volumeTrendStrengthPoint = Calculator.analyzeVolumeTrendStrengthPoint(marketData.getVolumeTrend());
        boolean volumeTrendDown = marketData.getVolumeTrend().getCurrTrendDirection().equals(VolumeTrendEnum.DOWN.getValue());
        boolean volumeSignalBearish = volumeTrendStrengthPoint >= MIN_VOLUME_STRENGTH_THRESHOLD && volumeTrendDown;

        if(bearishSignal >= 3 && volumeTrendDown) {
            log.info("EXIT-LONG detected. BearSignal={}", bearishSignal);
            return true;
        }
        if(bearishSignal >= 2 && volumeSignalBearish) {
            log.info("EXIT-LONG detected. BearSignal={}, VolumeTrendStrengthPoint={}", bearishSignal, volumeTrendStrengthPoint);
            return true;
        }
        if(bearishSignal >= 1 && volumeSignalBearish && smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig)) {
            log.info("EXIT-LONG detected. BearSignal={}, VolumeTrendStrengthPoint={}, Market price={}",
                    bearishSignal, volumeTrendStrengthPoint, marketData.getTradeEvent().getPriceAsBigDecimal());
            return true;
        }
        boolean priceIsNearResistance = priceIsNearResistance(marketData.getSmaTrend(), marketData.getTradeEvent().getPriceAsBigDecimal(), symbolConfig.getEmaThreshold());
        boolean priceIsDownUnderSupport = priceIsDownUnderSupport(marketData.getSmaTrend(), marketData.getTradeEvent().getPriceAsBigDecimal(), symbolConfig.getEmaThreshold());
        if(volumeSignalBearish && (priceIsNearResistance || priceIsDownUnderSupport)) {
            log.info("EXIT-LONG detected. VolumeTrendStrengthPoint={}, Market price={}, PriceIsNearResistance={}, PriceIsDownUnderSupport={}",
                    volumeTrendStrengthPoint, marketData.getTradeEvent().getPriceAsBigDecimal(), priceIsNearResistance, priceIsDownUnderSupport);
            return true;
        }
        log.info("Not found EXIT-LONG signal. The condition does NOT match");
        return false;
    }

    private boolean smaTrendIsBullish(SmaTrend smaTrend, SymbolConfig symbolConfig) {
        //SMA level dương - UP và SMA strength tỷ lệ tăng > smaThreshold (config trong symbol)
        boolean isBullish = smaTrend.getSmaTrendLevel() > 0 &&
                smaTrend.getSmaTrendStrength().compareTo(symbolConfig.getSmaThreshold()) >= 0;
        log.info("smaTrendIsBullish - SmaTrendLevel={}, SmaTrendStrength={}, SmaThreshold={} -> Result: {}",
                smaTrend.getSmaTrendLevel(), smaTrend.getSmaTrendStrength(), symbolConfig.getSmaThreshold(), isBullish);
        return isBullish;
    }

    private boolean smaTrendIsBearish(SmaTrend smaTrend, SymbolConfig symbolConfig) {
        //SMA level dương - UP và SMA strength tỷ lệ tăng > smaThreshold (config trong symbol)
        boolean isBearish = smaTrend.getSmaTrendLevel() < 0 &&
                smaTrend.getSmaTrendStrength().compareTo(symbolConfig.getSmaThreshold()) >= 0;
        log.info("smaTrendIsBearish - SmaTrendLevel={}, SmaTrendStrength={}, SmaThreshold={} -> Result: {}",
                smaTrend.getSmaTrendLevel(), smaTrend.getSmaTrendStrength(), symbolConfig.getSmaThreshold(), isBearish);
        return isBearish;
    }

    private boolean priceIsNearResistance(SmaTrend smaTrend, BigDecimal price, BigDecimal emaThreshold) {
        //price: maybe marketPrice or EMA price
        //resistance
        BigDecimal subtract = price.subtract(smaTrend.getResistancePrice());
        log.info("priceIsNearResistance - Price={}, Resistance={}, subtract={}, emaThreshold={}",
                price, smaTrend.getResistancePrice(), subtract, emaThreshold);
        //subtract âm: gần từ dưới lên -> càng gần càng tốt <= emaThreshold (giá up gần tới resistance)
        return subtract.compareTo(BigDecimal.ZERO) < 0 && subtract.compareTo(emaThreshold) <= 0;
    }

    private boolean priceIsUpOverResistance(SmaTrend smaTrend, BigDecimal price, BigDecimal emaThreshold) {
        //price: maybe marketPrice or EMA price
        //resistance
        BigDecimal subtract = price.subtract(smaTrend.getResistancePrice());
        log.info("priceIsUpOverResistance - Price={}, Resistance={}, subtract={}, emaThreshold={}",
                price, smaTrend.getResistancePrice(), subtract, emaThreshold);
        //subtract dương: gần từ trên xuống -> càng xa càng tốt (giá đã up vượt qua resistance)
        return subtract.compareTo(BigDecimal.ZERO) >= 0 && subtract.compareTo(emaThreshold) >= 0;
    }

    private boolean priceIsNearSupport(SmaTrend smaTrend, BigDecimal price, BigDecimal emaThreshold) {
        //price : maybe marketPrice or EMA price
        //resistance
        BigDecimal subtract = price.subtract(smaTrend.getSupportPrice());
        log.info("priceIsNearSupport - Price={}, Support={}, subtract={}, emaThreshold={}",
                price, smaTrend.getSupportPrice(), subtract, emaThreshold);
        //subtract dương: gần từ trên xuống -> càng gần càng tốt <= emaThreshold (giá down gần tới support)
        return subtract.compareTo(BigDecimal.ZERO) >= 0 && subtract.compareTo(emaThreshold) <= 0;
    }

    private boolean priceIsDownUnderSupport(SmaTrend smaTrend, BigDecimal price, BigDecimal emaThreshold) {
        //price : maybe marketPrice or EMA price
        //resistance
        BigDecimal subtract = price.subtract(smaTrend.getSupportPrice());
        log.info("priceIsDownUnderSupport - Price={}, Support={}, subtract={}, emaThreshold={}",
                price, smaTrend.getSupportPrice(), subtract, emaThreshold);
        //subtract âm: gần từ dưới lên -> càng xa càng tốt (giá đã down qua support)
        return subtract.compareTo(BigDecimal.ZERO) < 0 && subtract.compareTo(emaThreshold) >= 0;
    }
}
