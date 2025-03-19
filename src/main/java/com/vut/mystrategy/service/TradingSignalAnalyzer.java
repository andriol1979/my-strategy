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

        int bullishSignal = Calculator.isEmaBullishTrend(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), marketData.getLongEmaPrice().getPrice(), symbolConfig.getEmaThreshold());
        int volumeTrendStrengthPoint = Calculator.analyzeVolumeTrendStrengthPoint(marketData.getVolumeTrend());
        boolean volumeTrendUp = marketData.getVolumeTrend().getCurrTrendDirection().equals(VolumeTrendEnum.UP.getValue());
        boolean volumeSignalBullish = volumeTrendStrengthPoint >= MIN_VOLUME_STRENGTH_THRESHOLD && volumeTrendUp;

        log.info("ENTRY-LONG debugging Market data: {}", marketData);
        if(bullishSignal >= 3 && (volumeTrendUp || smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig))) {
            log.info("ENTRY-LONG detected. BullishSignal={}", bullishSignal);
            return true;
        }
        if(bullishSignal >= 2 && (volumeSignalBullish || smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig))) {
            log.info("ENTRY-LONG detected. BullishSignal={}, VolumeTrendStrengthPoint={}", bullishSignal, volumeTrendStrengthPoint);
            return true;
        }
        if(bullishSignal >= 1 && volumeSignalBullish && smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig)) {
            log.info("ENTRY-LONG detected. BullishSignal={}, VolumeTrendStrengthPoint={}, Market price={}",
                    bullishSignal, volumeTrendStrengthPoint, marketData.getTradeEvent().getPriceAsBigDecimal());
            return true;
        }
        boolean priceIsNearSupport = priceIsNearSupport(marketData.getSmaTrend(), shortCurrEmaPrice.getPrice(), symbolConfig.getSupportThreshold());
        boolean priceIsUpOverResistance = priceIsUpOverResistance(marketData.getSmaTrend(), shortCurrEmaPrice.getPrice(), symbolConfig.getResistanceThreshold());
        if((volumeSignalBullish || smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig)) &&
                (priceIsNearSupport || priceIsUpOverResistance)) {
            log.info("ENTRY-LONG detected. VolumeTrendStrengthPoint={}, ShortCurrEmaPrice={}, PriceIsNearSupport={}, PriceIsUpOverResistance={}",
                    volumeTrendStrengthPoint, shortCurrEmaPrice.getPrice(), priceIsNearSupport, priceIsUpOverResistance);
            return true;
        }
        log.info("Not found ENTRY-LONG signal. The condition does NOT match");
        return false;
    }

    public boolean isExitLong(MarketData marketData, SymbolConfig symbolConfig) {
        EmaPrice shortCurrEmaPrice = marketData.getShortEmaPricesList().get(0);
        EmaPrice shortPrevEmaPrice = marketData.getShortEmaPricesList().get(1);

        int bearishSignal = Calculator.isEmaBearishTrend(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), marketData.getLongEmaPrice().getPrice(), symbolConfig.getEmaThreshold());
        int volumeTrendStrengthPoint = Calculator.analyzeVolumeTrendStrengthPoint(marketData.getVolumeTrend());
        boolean volumeTrendDown = marketData.getVolumeTrend().getCurrTrendDirection().equals(VolumeTrendEnum.DOWN.getValue());
        boolean volumeSignalBearish = volumeTrendStrengthPoint >= MIN_VOLUME_STRENGTH_THRESHOLD && volumeTrendDown;

        log.info("EXIT-LONG debugging Market data: {}", marketData);
        if(bearishSignal >= 3 && (volumeTrendDown || smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig))) {
            log.info("EXIT-LONG detected. BearSignal={}", bearishSignal);
            return true;
        }
        if(bearishSignal >= 2 && (volumeSignalBearish || smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig))) {
            log.info("EXIT-LONG detected. BearSignal={}, VolumeTrendStrengthPoint={}", bearishSignal, volumeTrendStrengthPoint);
            return true;
        }
        if(bearishSignal >= 1 && volumeSignalBearish && smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig)) {
            log.info("EXIT-LONG detected. BearSignal={}, VolumeTrendStrengthPoint={}, Market price={}",
                    bearishSignal, volumeTrendStrengthPoint, marketData.getTradeEvent().getPriceAsBigDecimal());
            return true;
        }
        boolean priceIsNearResistance = priceIsNearResistance(marketData.getSmaTrend(), shortCurrEmaPrice.getPrice(), symbolConfig.getResistanceThreshold());
        boolean priceIsDownUnderSupport = priceIsDownUnderSupport(marketData.getSmaTrend(), shortCurrEmaPrice.getPrice(), symbolConfig.getSupportThreshold());
        if((volumeSignalBearish || smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig)) &&
                (priceIsNearResistance || priceIsDownUnderSupport)) {
            log.info("EXIT-LONG detected. VolumeTrendStrengthPoint={}, ShortCurrEmaPrice={}, PriceIsNearResistance={}, PriceIsDownUnderSupport={}",
                    volumeTrendStrengthPoint, shortCurrEmaPrice.getPrice(), priceIsNearResistance, priceIsDownUnderSupport);
            return true;
        }
        log.info("Not found EXIT-LONG signal. The condition does NOT match");
        return false;
    }

    private boolean smaTrendIsBullish(SmaTrend smaTrend, SymbolConfig symbolConfig) {
        //SMA level dương - UP và SMA strength tỷ lệ tăng > smaThreshold (config trong symbol)
        boolean isBullish = smaTrend.getSmaTrendDirection().equals(PriceTrendEnum.UP.getValue()) &&
                smaTrend.getSmaTrendStrength().compareTo(symbolConfig.getSmaTrendStrengthThreshold()) >= 0;
        log.info("SMA SmaTrendLevel={}, SmaTrendStrength={}, SmaTrendStrengthThreshold={} -> smaTrendIsBullish: {}",
                smaTrend.getSmaTrendLevel(), smaTrend.getSmaTrendStrength(), symbolConfig.getSmaTrendStrengthThreshold(), isBullish);
        return isBullish;
    }

    private boolean smaTrendIsBearish(SmaTrend smaTrend, SymbolConfig symbolConfig) {
        //SMA level dương - UP và SMA strength tỷ lệ tăng > smaThreshold (config trong symbol)
        boolean isBearish = smaTrend.getSmaTrendDirection().equals(PriceTrendEnum.DOWN.getValue()) &&
                smaTrend.getSmaTrendStrength().compareTo(symbolConfig.getSmaTrendStrengthThreshold()) >= 0;
        log.info("SMA SmaTrendLevel={}, SmaTrendStrength={}, SmaTrendStrengthThreshold={} -> smaTrendIsBearish: {}",
                smaTrend.getSmaTrendLevel(), smaTrend.getSmaTrendStrength(), symbolConfig.getSmaTrendStrengthThreshold(), isBearish);
        return isBearish;
    }

    private boolean priceIsNearResistance(SmaTrend smaTrend, BigDecimal price, BigDecimal resistanceThreshold) {
        //price: maybe marketPrice or EMA price
        //resistance
        //subtract âm: gần từ dưới lên -> càng gần càng tốt <= emaThreshold (giá up gần tới resistance)
        BigDecimal subtract = price.subtract(smaTrend.getResistancePrice());
        boolean priceIsNearResistance = subtract.compareTo(BigDecimal.ZERO) < 0 && subtract.compareTo(resistanceThreshold) <= 0;
        log.info("SMA Price={}, Resistance={}, subtract={}, resistanceThreshold={} -> priceIsNearResistance: {}",
                price, smaTrend.getResistancePrice(), subtract, resistanceThreshold, priceIsNearResistance);

        return priceIsNearResistance;
    }

    private boolean priceIsUpOverResistance(SmaTrend smaTrend, BigDecimal price, BigDecimal resistanceThreshold) {
        //price: maybe marketPrice or EMA price
        //resistance
        //subtract dương: gần từ trên xuống -> càng xa càng tốt (giá đã up vượt qua resistance)
        BigDecimal subtract = price.subtract(smaTrend.getResistancePrice());
        boolean priceIsUpOverResistance = subtract.compareTo(BigDecimal.ZERO) >= 0 && subtract.compareTo(resistanceThreshold) >= 0;
        log.info("SMA Price={}, Resistance={}, subtract={}, resistanceThreshold={} -> priceIsUpOverResistance: {}",
                price, smaTrend.getResistancePrice(), subtract, resistanceThreshold, priceIsUpOverResistance);
        return priceIsUpOverResistance;
    }

    private boolean priceIsNearSupport(SmaTrend smaTrend, BigDecimal price, BigDecimal supportThreshold) {
        //price : maybe marketPrice or EMA price
        //resistance
        //subtract dương: gần từ trên xuống -> càng gần càng tốt <= emaThreshold (giá down gần tới support)
        BigDecimal subtract = price.subtract(smaTrend.getSupportPrice());
        boolean priceIsNearSupport = subtract.compareTo(BigDecimal.ZERO) >= 0 && subtract.compareTo(supportThreshold) <= 0;
        log.info("SMA Price={}, Support={}, subtract={}, supportThreshold={} -> priceIsNearSupport: {}",
                price, smaTrend.getSupportPrice(), subtract, supportThreshold, priceIsNearSupport);
        return priceIsNearSupport;
    }

    private boolean priceIsDownUnderSupport(SmaTrend smaTrend, BigDecimal price, BigDecimal supportThreshold) {
        //price : maybe marketPrice or EMA price
        //resistance
        //subtract âm: gần từ dưới lên -> càng xa càng tốt (giá đã down qua support)
        BigDecimal subtract = price.subtract(smaTrend.getSupportPrice());
        boolean priceIsDownUnderSupport = subtract.compareTo(BigDecimal.ZERO) < 0 && subtract.compareTo(supportThreshold) >= 0;
        log.info("SMA Price={}, Support={}, subtract={}, supportThreshold={} -> priceIsDownUnderSupport: {}",
                price, smaTrend.getSupportPrice(), subtract, supportThreshold, priceIsDownUnderSupport);
        return priceIsDownUnderSupport;
    }
}
