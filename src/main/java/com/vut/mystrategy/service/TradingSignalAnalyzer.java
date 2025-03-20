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

        int bullishSignal = isEmaBullishTrend(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), marketData.getLongEmaPrice().getPrice(), symbolConfig.getEmaThreshold());
        int volumeTrendStrengthPoint = analyzeVolumeTrendStrengthPoint(marketData.getVolumeTrend());
        boolean volumeTrendBullish = marketData.getVolumeTrend().getCurrTrendDirection().equals(VolumeTrendEnum.BULL.getValue());
        boolean volumeTrendStrengthBullishOver = volumeTrendStrengthPoint >= symbolConfig.getMinVolumeStrengthThreshold();
        boolean volumeSignalBullish = volumeTrendStrengthBullishOver && volumeTrendBullish;

        log.info("ENTRY-LONG debugging Market data: {}", marketData);
        if(bullishSignal >= 3 && (volumeTrendStrengthBullishOver ||
                smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig) || volumeTrendBullish)) {
            log.info("ENTRY-LONG detected. BullishSignal={}", bullishSignal);
            return true;
        }
        if(bullishSignal >= 2 && (volumeTrendStrengthBullishOver || smaTrendIsBullish(marketData.getSmaTrend(), symbolConfig))) {
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

        int bearishSignal = isEmaBearishTrend(shortPrevEmaPrice.getPrice(),
                shortCurrEmaPrice.getPrice(), marketData.getLongEmaPrice().getPrice(), symbolConfig.getEmaThreshold());
        int volumeTrendStrengthPoint = analyzeVolumeTrendStrengthPoint(marketData.getVolumeTrend());
        boolean volumeTrendBearish = marketData.getVolumeTrend().getCurrTrendDirection().equals(VolumeTrendEnum.BEAR.getValue());
        boolean volumeTrendStrengthBearishOver = volumeTrendStrengthPoint >= symbolConfig.getMinVolumeStrengthThreshold();
        boolean volumeSignalBearish = volumeTrendStrengthPoint >= symbolConfig.getMinVolumeStrengthThreshold() && volumeTrendBearish;

        log.info("EXIT-LONG debugging Market data: {}", marketData);
        if(bearishSignal >= 3 && (volumeTrendStrengthBearishOver ||
                smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig) || volumeTrendBearish)) {
            log.info("EXIT-LONG detected. BearSignal={}", bearishSignal);
            return true;
        }
        if(bearishSignal >= 2 && (volumeTrendStrengthBearishOver || smaTrendIsBearish(marketData.getSmaTrend(), symbolConfig))) {
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

    private int analyzeVolumeTrendStrengthPoint(VolumeTrend volumeTrend) {
        //strengthPoint range 0 - 12
        int strengthPoint = 0;
        if(volumeTrend.getPrevTrendStrength() == null ||
                volumeTrend.getPrevDivergence() == null ||
                StringUtils.isEmpty(volumeTrend.getPrevTrendDirection())) {
            log.warn("VolumeTrend is not enough data to analyze VolumeTrendStrengthPoint - Strength point = {}", strengthPoint);
            return strengthPoint;
        }
        // 1. Sức mạnh xu hướng hiện tại lớn hơn trước đó
        boolean isCurrStrengthGreater = volumeTrend.getCurrTrendStrength().compareTo(volumeTrend.getPrevTrendStrength()) > 0;
        if (isCurrStrengthGreater) {
            strengthPoint += 2;
        }
        // 2. Có hướng xu hướng rõ ràng (UP hoặc DOWN)
        boolean hasClearDirection = volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.BULL.getValue()) ||
                volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.BEAR.getValue());
        if (hasClearDirection) {
            strengthPoint += 1;
        }
        // 3. Xu hướng tiếp diễn (không NEUTRAL)
        boolean isTrendContinuing = volumeTrend.getCurrTrendDirection().equals(volumeTrend.getPrevTrendDirection()) &&
                !volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.SIDEWAYS.getValue());
        if (isTrendContinuing) {
            strengthPoint += 2;
        }
        // 4. Độ lệch (divergence) lớn
        BigDecimal absCurrDivergence = volumeTrend.getCurrDivergence().abs();
        boolean hasLargeDivergence = absCurrDivergence.compareTo(BigDecimal.TEN) > 0;
        boolean hasMediumDivergence = absCurrDivergence.compareTo(BigDecimal.valueOf(5)) > 0 && !hasLargeDivergence;
        if (hasLargeDivergence) {
            strengthPoint += 2;
        } else if (hasMediumDivergence) {
            strengthPoint += 1;
        }
        // 5. Độ lệch (divergence) tăng theo hướng xu hướng
        boolean isDivergenceIncreasing =
                (volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.BULL.getValue()) &&
                        volumeTrend.getCurrDivergence().compareTo(volumeTrend.getPrevDivergence()) > 0) ||
                        (volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.BEAR.getValue()) &&
                                volumeTrend.getCurrDivergence().compareTo(volumeTrend.getPrevDivergence()) < 0);
        if (isDivergenceIncreasing) {
            strengthPoint += 2;
        }
        // 6. Có volume spike (BULL hoặc BEAR)
        boolean hasVolumeSpike = volumeTrend.getVolumeSpike().equals(VolumeSpikeEnum.BULL.getValue()) ||
                volumeTrend.getVolumeSpike().equals(VolumeSpikeEnum.BEAR.getValue());
        if (hasVolumeSpike) {
            strengthPoint += 3;
        }

        // Log tổng hợp để debug
        log.info("Analyzed VolumeTrendStrengthPoint: strengthPoint={}, isCurrStrengthGreater={}, hasClearDirection={}, " +
                        "isTrendContinuing={}, hasLargeDivergence={}, hasMediumDivergence={}, isDivergenceIncreasing={}, " +
                        "hasVolumeSpike={}, currDirection={}, prevDirection={}, currStrength={}, prevStrength={}, currDivergence={}, " +
                        "prevDivergence={}, volumeSpike={}",
                strengthPoint, isCurrStrengthGreater, hasClearDirection, isTrendContinuing,
                hasLargeDivergence, hasMediumDivergence, isDivergenceIncreasing, hasVolumeSpike,
                volumeTrend.getCurrTrendDirection(), volumeTrend.getPrevTrendDirection(), volumeTrend.getCurrTrendStrength(),
                volumeTrend.getPrevTrendStrength(), volumeTrend.getCurrDivergence(),
                volumeTrend.getPrevDivergence(), volumeTrend.getVolumeSpike());
        return strengthPoint;
    }

    /*
    Logic to calculate point EMA:
        - DK_1: big crossover - (short prev < long && short curr > long và % chênh lệnh >= threshold (EMA threshold trong config)): 4
        - DK_2: normal crossover - (short prev < long && short curr > long): 3
        - DK_3: normal bullish - short curr > long và % chênh lệnh >= threshold (EMA threshold trong config): 2
        - DK_4: small bullish - short curr > long: 1
        - không thoả 3 điều kiện trên: 0
     */
    private int isEmaBullishTrend(BigDecimal shortPrevEmaPrice, BigDecimal shortCurrEmaPrice,
                                        BigDecimal longEmaPrice, BigDecimal emaThreshold) {
        // Kiểm tra EMA ngắn hiện tại có vượt EMA dài không
        boolean isCurrAbove = shortCurrEmaPrice.compareTo(longEmaPrice) > 0;
        // Kiểm tra crossover truyền thống
        boolean isTraditionalCrossover = shortPrevEmaPrice.compareTo(longEmaPrice) <= 0 && isCurrAbove;
        // Tính % chênh lệch để kiểm soát độ nhạy
        BigDecimal diffPercent = shortCurrEmaPrice.subtract(longEmaPrice)
                .abs().divide(longEmaPrice, Calculator.SCALE, Calculator.ROUNDING_MODE_HALF_UP);

        if(isTraditionalCrossover && diffPercent.compareTo(emaThreshold) >= 0) {
            log.info("EMA BullishTrend - Big crossover detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
            return 4; //big crossover - DK_1
        }
        if(isTraditionalCrossover) {
            log.info("EMA BullishTrend - Normal crossover detected: shortPrev={}, shortCurr={}, longEmaPrice={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice);
            return 3; //crossover - DK_2
        }
        if(isCurrAbove && diffPercent.compareTo(emaThreshold) >= 0) {
            log.info("EMA BullishTrend - Normal bullish detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
            return 2; //normal bullish - DK_3
        }
        if(isCurrAbove) {
            log.info("EMA BullishTrend - Small bullish detected: shortPrev={}, shortCurr={}, longEmaPrice={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice);
            return 1; //small bullish - DK_4
        }
        log.info("EMA BullishTrend - No bullish trend detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
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
