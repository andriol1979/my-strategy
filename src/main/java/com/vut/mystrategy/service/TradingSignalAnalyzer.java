package com.vut.mystrategy.service;

import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    public boolean isEntryLong(MarketData marketData, SymbolConfig symbolConfig) {
        EmaPrice shortCurrEmaPrice = marketData.getShortEmaPricesList().get(0);
        EmaPrice shortPrevEmaPrice = marketData.getShortEmaPricesList().get(1);

        int emaBullishSignal = isEmaBullishTrend(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), marketData.getLongEmaPrice().getPrice(), symbolConfig);
        VolumeTrendEnum currentVolumeTrend = marketData.getVolumeTrend().getCurrentVolumeTrend();
        boolean isVolumeBullish = currentVolumeTrend.getValue().equals(VolumeTrendEnum.BULL.getValue());
        //just know the strength of volume - don't know the direction UP or DOWN (should be combined with the others)
        int volumeStrengthPoint = analyzeVolumeStrengthPoint(marketData.getVolumeTrend(), symbolConfig.getDivergenceThreshold());
        boolean strengthPointOverThreshold = volumeStrengthPoint >= symbolConfig.getMinVolumeStrengthThreshold();
        boolean strengthPointCrossOverThreshold = volumeStrengthPoint > symbolConfig.getMinVolumeStrengthThreshold() + 1;

        boolean volumeSignalBullish = strengthPointOverThreshold && isVolumeBullish;
        boolean volumeSignalBullishStrong = strengthPointCrossOverThreshold && isVolumeBullish;

        boolean priceIsNearSupport = priceIsNearSupport(marketData.getSmaTrend(), shortCurrEmaPrice.getPrice(), symbolConfig.getSupportThreshold());
        boolean priceIsUpOverResistance = priceIsUpOverResistance(marketData.getSmaTrend(), shortCurrEmaPrice.getPrice(), symbolConfig.getResistanceThreshold());

        log.info("ENTRY-LONG debugging Market data: {}", marketData);
        //EMA signal(bullishSignal) == 3 AND [(volume is UP && volume strength > MinVolumeStrengthThreshold) OR SMA signal is UP]
        if(emaBullishSignal == 3 && (volumeSignalBullish || smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig))) {
            log.info("ENTRY-LONG detected. Condition = (bullishSignal == 3 && (volumeSignalBullish || smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig)))");
            return true;
        }
        //EMA signal(bullishSignal) == 2 && volume is UP && volume strength > MinVolumeStrengthThreshold && MA signal is UP
        if(emaBullishSignal == 2 && volumeSignalBullish && smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig)) {
            log.info("ENTRY-LONG detected. Condition = (bullishSignal == 2 && volumeSignalBullish && smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig))");
            return true;
        }
        //Check theo bounce OR breakout
        //bounce: nếu giá EMA ngắn về gần support && volume BUY tăng lên đột biến -> kỳ vọng giá tăng lại
        if(priceIsNearSupport && volumeSignalBullishStrong) {
            log.info("ENTRY-LONG detected. Condition = (priceIsNearSupport && volumeSignalBullishStrong)");
            return true;
        }
        //breakout: nếu giá EMA ngắn vượt qua resistance && volume BUY tăng lên đột biến -> kỳ vọng giá tăng tiếp
        if(priceIsUpOverResistance && volumeSignalBullishStrong) {
            log.info("ENTRY-LONG detected. Condition = (priceIsUpOverResistance && volumeSignalBullishStrong)");
            return true;
        }
        log.info("ENTRY-LONG signal NOT FOUND. The condition does NOT match");
        return false;
    }

    public boolean isExitLong(MarketData marketData, SymbolConfig symbolConfig) {
        EmaPrice shortCurrEmaPrice = marketData.getShortEmaPricesList().get(0);
        EmaPrice shortPrevEmaPrice = marketData.getShortEmaPricesList().get(1);

        int bearishSignal = isEmaBearishTrend(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), marketData.getLongEmaPrice().getPrice(), symbolConfig.getEmaThresholdAbove());
        int volumeStrengthPoint = analyzeVolumeStrengthPoint(marketData.getVolumeTrend(), symbolConfig.getDivergenceThreshold());
        boolean volumeTrendBearish = marketData.getVolumeTrend().getCurrTrendDirection().equals(VolumeTrendEnum.BEAR.getValue());
        boolean volumeTrendStrengthBearishOver = volumeStrengthPoint >= symbolConfig.getMinVolumeStrengthThreshold();
        boolean volumeSignalBearish = volumeStrengthPoint >= symbolConfig.getMinVolumeStrengthThreshold() && volumeTrendBearish;

        log.info("EXIT-LONG debugging Market data: {}", marketData);
        if(bearishSignal >= 3 && (volumeTrendStrengthBearishOver ||
                smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig) || volumeTrendBearish)) {
            log.info("EXIT-LONG detected. BearSignal={}", bearishSignal);
            return true;
        }
        if(bearishSignal >= 2 && (volumeTrendStrengthBearishOver || smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig))) {
            log.info("EXIT-LONG detected. BearSignal={}, VolumeTrendStrengthPoint={}", bearishSignal, volumeStrengthPoint);
            return true;
        }
        if(bearishSignal >= 1 && volumeSignalBearish && smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig)) {
            log.info("EXIT-LONG detected. BearSignal={}, VolumeTrendStrengthPoint={}, Market price={}",
                    bearishSignal, volumeStrengthPoint, marketData.getTradeEvent().getPriceAsBigDecimal());
            return true;
        }
        boolean priceIsNearResistance = priceIsNearResistance(marketData.getSmaTrend(), shortCurrEmaPrice.getPrice(), symbolConfig.getResistanceThreshold());
        boolean priceIsDownUnderSupport = priceIsDownUnderSupport(marketData.getSmaTrend(), shortCurrEmaPrice.getPrice(), symbolConfig.getSupportThreshold());
        if((volumeSignalBearish || smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig)) &&
                (priceIsNearResistance || priceIsDownUnderSupport)) {
            log.info("EXIT-LONG detected. VolumeTrendStrengthPoint={}, ShortCurrEmaPrice={}, PriceIsNearResistance={}, PriceIsDownUnderSupport={}",
                    volumeStrengthPoint, shortCurrEmaPrice.getPrice(), priceIsNearResistance, priceIsDownUnderSupport);
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

    //analyzeVolumeTrendStrengthPoint method just analyze strength of volume -> don't know direction
    private int analyzeVolumeStrengthPoint(VolumeTrend volumeTrend, BigDecimal divergenceThreshold) {
        //strengthPoint range 0 - 7
        int strengthPoint = 0;
        if(volumeTrend.getCurrDivergence() == null ||
                StringUtils.isEmpty(volumeTrend.getCurrTrendDirection())) {
            log.info("VolumeTrend is not enough data to analyze volume strength point - Strength point = {}", strengthPoint);
            return strengthPoint;
        }

        BigDecimal currDivergenceAbs = volumeTrend.getCurrDivergence().abs(); //trị tuyệt đối bull -> 0-100, bear -> 0->100
        BigDecimal prevDivergenceAbs = volumeTrend.getPrevDivergence().abs(); //trị tuyệt đối bull -> 0-100, bear -> 0->100

        //check currDivergence > divergenceThreshold
        if(currDivergenceAbs.compareTo(divergenceThreshold) > 0) {
           strengthPoint += 2;
        }
        //check prevDivergence > divergenceThreshold
        if(prevDivergenceAbs.compareTo(divergenceThreshold) > 0) {
            strengthPoint += 2;
        }
        //check currDivergence > prevDivergence -> tăng liên tiếp
        if(currDivergenceAbs.compareTo(prevDivergenceAbs) > 0) {
            strengthPoint += 1;
        }
        //check volume spike NOT flat -> should be BULL or BEAR
        if(!volumeTrend.getVolumeSpike().equals(VolumeSpikeEnum.FLAT.getValue())) {
            strengthPoint += 1;
        }
        //check new total volume > previous total volume
        if(volumeTrend.getNewTotalVolume().compareTo(volumeTrend.getPrevTotalVolume()) > 0) {
            strengthPoint += 1;
        }
        log.info("Analyzed volume strength point from VolumeTrend: {} and divergenceThreshold = {} - Strength point = {}",
                volumeTrend, divergenceThreshold, strengthPoint);
        return strengthPoint;
    }

    /*
    Logic to calculate point EMA: (updated 21.Mar.2025)
        - DK_1: crossover - current EMA - Long EMA > EmaThresholdAbove(USDT) && previous EMA < Long EMA (nhỏ hơn một khoảng EmaThresholdBelow(USDT) )
        - DK_2: normal Bullish - (short prev < long && short curr > long): 3
        - DK_3: normal bullish - short curr > long và % chênh lệnh >= threshold (EMA threshold trong config): 2
        - DK_4: small bullish - short curr > long: 1
        - không thoả 3 điều kiện trên: 0
     */
    private int isEmaBullishTrend(BigDecimal shortPrevEmaPrice, BigDecimal shortCurrEmaPrice,
                                        BigDecimal longEmaPrice, SymbolConfig symbolConfig) {
        BigDecimal currShortEmaDiff = shortCurrEmaPrice.subtract(longEmaPrice); //should be positive
        BigDecimal prevShortEmaDiff = shortPrevEmaPrice.subtract(longEmaPrice); //should be negative

        // Kiểm tra EMA ngắn hiện tại có vượt EMA dài không
        boolean isCurrAbove = currShortEmaDiff.compareTo(symbolConfig.getEmaThresholdAbove()) >= 0;
        // Kiểm tra EMA ngắn trước đó có nằm dưới EMA dài không
        boolean prevShortDiffNegative = prevShortEmaDiff.compareTo(BigDecimal.ZERO) < 0;
        boolean isPrevBelow = prevShortDiffNegative &&
                prevShortEmaDiff.abs().compareTo(symbolConfig.getEmaThresholdBelow()) >= 0;

        if(isCurrAbove && isPrevBelow) {
            log.info("EMA BullishTrend - Crossover detected: shortPrev={}, shortCurr={}, longEmaPrice={}, " +
                            "currShortEmaDiff={} - EmaThresholdAbove={}, prevShortEmaDiff={} - EmaThresholdBelow={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, currShortEmaDiff, symbolConfig.getEmaThresholdAbove(),
                    prevShortEmaDiff, symbolConfig.getEmaThresholdBelow());
            return 3; //big crossover - DK_1
        }
        if(isCurrAbove && prevShortDiffNegative) {
            log.info("EMA BullishTrend - Normal Bullish detected: shortPrev={}, shortCurr={}, longEmaPrice={}, " +
                            "currShortEmaDiff={} - EmaThresholdAbove={}, prevShortEmaDiff={} - EmaThresholdBelow={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, currShortEmaDiff, symbolConfig.getEmaThresholdAbove(),
                    prevShortEmaDiff, symbolConfig.getEmaThresholdBelow());
            return 2; //crossover - DK_2
        }
        if(isCurrAbove) {
            log.info("EMA BullishTrend - Small Bullish detected: shortPrev={}, shortCurr={}, longEmaPrice={}, " +
                            "currShortEmaDiff={} - EmaThresholdAbove={}, prevShortEmaDiff={} - EmaThresholdBelow={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, currShortEmaDiff, symbolConfig.getEmaThresholdAbove(),
                    prevShortEmaDiff, symbolConfig.getEmaThresholdBelow());
            return 1; //small bullish - DK_3
        }
        log.info("EMA BullishTrend - No Bullish detected: shortPrev={}, shortCurr={}, longEmaPrice={}, " +
                        "currShortEmaDiff={} - EmaThresholdAbove={}, prevShortEmaDiff={} - EmaThresholdBelow={}",
                shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, currShortEmaDiff, symbolConfig.getEmaThresholdAbove(),
                prevShortEmaDiff, symbolConfig.getEmaThresholdBelow());
        return 0; // Không thỏa mãn
    }

    /*
    Logic to calculate point EMA:
        - DK_1: big crossover - (short prev > long && short curr < long và % chênh lệnh >= threshold (EMA threshold trong config)): 4
        - DK_2: normal crossover - (short prev > long && short curr < long): 3
        - DK_3: normal bearish - short curr < long và % chênh lệnh >= threshold (EMA threshold trong config): 2
        - DK_4: small bearish - short curr < long: 1
        - không thoả 3 điều kiện trên: 0
     */
    private int isEmaBearishTrend(BigDecimal shortPrevEmaPrice, BigDecimal shortCurrEmaPrice,
                                        BigDecimal longEmaPrice, BigDecimal emaThreshold) {
        // Kiểm tra EMA ngắn hiện tại có dưới EMA dài không
        boolean isCurrBelow = shortCurrEmaPrice.compareTo(longEmaPrice) < 0;
        // Kiểm tra crossover truyền thống
        boolean isTraditionalCrossover = shortPrevEmaPrice.compareTo(longEmaPrice) >= 0 && isCurrBelow;
        // Tính % chênh lệch
        BigDecimal diffPercent = shortCurrEmaPrice.subtract(longEmaPrice)
                .abs().divide(longEmaPrice, Calculator.SCALE, Calculator.ROUNDING_MODE_HALF_UP);

        if(isTraditionalCrossover && diffPercent.compareTo(emaThreshold) >= 0) {
            log.info("EMA BearishTrend - Big crossover detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
            return 4; //big crossover - DK_1
        }
        if(isTraditionalCrossover) {
            log.info("EMA BearishTrend - Normal crossover detected: shortPrev={}, shortCurr={}, longEmaPrice={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice);
            return 3; //crossover - DK_2
        }
        if(isCurrBelow && diffPercent.compareTo(emaThreshold) >= 0) {
            log.info("EMA BearishTrend - Normal bearish detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
            return 2; //normal bearish - DK_3
        }
        if(isCurrBelow) {
            log.info("EMA BearishTrend - Small bearish detected: shortPrev={}, shortCurr={}, longEmaPrice={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice);
            return 1; //small bearish - DK_4
        }
        log.info("EMA BearishTrend - No bearish trend detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
        return 0; // Không thỏa mãn
    }
}
