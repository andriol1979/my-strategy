package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.LotSizeResponse;
import com.vut.mystrategy.model.VolumeSpikeEnum;
import com.vut.mystrategy.model.VolumeTrend;
import com.vut.mystrategy.model.VolumeTrendEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
public class Calculator {

    private static final int SCALE = 8;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal(100);
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static String calculateQuantity(LotSizeResponse binanceFutureLotSize,
                                           BigDecimal amount, BigDecimal price) {
        // Tính quantity thô
        BigDecimal quantity = amount.divide(price, SCALE, ROUNDING_MODE);
        // Làm tròn theo step size
        BigDecimal multiplier = BigDecimal.ONE.divide(binanceFutureLotSize.getStepSizeAsBigDecimal(), 0, ROUNDING_MODE);
        BigDecimal roundedQuantity = quantity.multiply(multiplier)
                .setScale(0, ROUNDING_MODE)
                .divide(multiplier, SCALE, ROUNDING_MODE);

        // Kiểm tra minimum notional (5 USDT cho Futures)
        BigDecimal notional = roundedQuantity.multiply(price);
        if (notional.compareTo(BigDecimal.valueOf(5)) < 0) {
            log.warn("Order value {} USDT for {} below min notional 5 USDT", notional, binanceFutureLotSize.getSymbol());
        }

        return roundedQuantity.stripTrailingZeros().toPlainString();
    }

    public static BigDecimal calculateEmaPrice(BigDecimal currPrice, BigDecimal prevEmaPrice,
                                               BigDecimal smoothingFactor) {
        //dùng trọng số (smoothingFactor = 0.3333) để đề cao vai trò của prevEmaPrice (1 - 0.3333 = 0.6667)
        return smoothingFactor.multiply(currPrice)
                .add((BigDecimal.ONE.subtract(smoothingFactor)).multiply(prevEmaPrice))
                .setScale(SCALE, ROUNDING_MODE);
    }

    public static <T> BigDecimal getMaxPrice(List<T> list, Function<T, BigDecimal> priceMapper) {
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return list.stream()
                .map(priceMapper)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public static <T> BigDecimal getMinPrice(List<T> list, Function<T, BigDecimal> priceMapper) {
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return list.stream()
                .map(priceMapper)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public static <T> BigDecimal getAveragePrice(List<T> list, Function<T, BigDecimal> priceMapper) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        BigDecimal sum = list.stream()
                .map(priceMapper)
                .filter(Objects::nonNull) // Lọc giá trị null
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(list.size()), SCALE, ROUNDING_MODE);
    }

    public static BigDecimal getRateChange(BigDecimal currAvg, BigDecimal prevAvg) {
        return currAvg.subtract(prevAvg)
                .divide(prevAvg, 4, ROUNDING_MODE);
    }

    public static BigDecimal calculateEmaSmoothingFactor(int emaPeriod) {
        return new BigDecimal(2).divide(
                new BigDecimal(emaPeriod + 1), 4, ROUNDING_MODE); // 2/(5+1) = 0.3333
    }

    public static BigDecimal calculateVolumeBasedOnWeight(BigDecimal takerVolume, BigDecimal makerVolume,
                                                          Double sumVolumeTakerWeight, Double sumVolumeMakerWeight) {
        // Ex: bullVolume = (0.6 * bullTakerVolume) + (0.4 * bullMakerVolume)
        return BigDecimal.valueOf(sumVolumeTakerWeight).multiply(takerVolume)
                .add(BigDecimal.valueOf(sumVolumeMakerWeight).multiply(makerVolume));
    }

    public static BigDecimal calculateBullBearVolumeDivergence(BigDecimal bullVolume, BigDecimal bearVolume) {
        BigDecimal bullBearVolumeDivergence;
        final BigDecimal sumBullBearVolume = bullVolume.add(bearVolume);
        boolean bullVolumeIsZero = bullVolume.compareTo(BigDecimal.ZERO) == 0;
        boolean bearVolumeIsZero = bearVolume.compareTo(BigDecimal.ZERO) == 0;

        if (bullVolumeIsZero && bearVolumeIsZero) {
            bullBearVolumeDivergence = BigDecimal.ZERO;
        }
        else if (bearVolumeIsZero) {
            bullBearVolumeDivergence = ONE_HUNDRED; // Bull thắng tuyệt đối
        }
        else if (bullVolumeIsZero) {
            bullBearVolumeDivergence = ONE_HUNDRED.negate(); // Bear thắng tuyệt đối
        }
        else if (bullVolume.compareTo(bearVolume) > 0) {
            bullBearVolumeDivergence = ((bullVolume.subtract(bearVolume))
                    .divide(sumBullBearVolume, SCALE, ROUNDING_MODE))
                    .multiply(ONE_HUNDRED).setScale(2, ROUNDING_MODE);
        }
        else {
            bullBearVolumeDivergence = ((bearVolume.subtract(bullVolume))
                    .divide(sumBullBearVolume, SCALE, ROUNDING_MODE))
                    .multiply(ONE_HUNDRED)
                    .negate().setScale(2, ROUNDING_MODE);
        }

        return bullBearVolumeDivergence;
    }

    public static BigDecimal calculateVolumeTrendStrength(BigDecimal newTotalVolume, BigDecimal prevTotalVolume,
                                                          BigDecimal newDivergence, BigDecimal divergenceThreshold) {
        if (newTotalVolume == null || prevTotalVolume == null || newDivergence == null || divergenceThreshold == null) {
            return BigDecimal.ZERO;
        }
        // strength = newDivergence / 100 because newDivergence is %. Ex: 10%, 20%...
        BigDecimal strength = newDivergence.abs().divide(ONE_HUNDRED, SCALE, ROUNDING_MODE);
        if (prevTotalVolume.compareTo(BigDecimal.ZERO) == 0) {
            return strength; // Giữ nguyên strength nếu prevTotalVolume = 0
        }
        BigDecimal volumeChangeRate = newTotalVolume.subtract(prevTotalVolume)
                .divide(prevTotalVolume, SCALE, ROUNDING_MODE);
        BigDecimal adjustmentFactor = BigDecimal.ONE.add(volumeChangeRate);
        if (volumeChangeRate.abs().compareTo(divergenceThreshold) > 0) { // Ngưỡng 10%
            strength = strength.multiply(adjustmentFactor.max(new BigDecimal("0.5")).min(new BigDecimal("1.5")));
        }

        return strength;
    }

    public static int analyzeVolumeTrendStrengthPoint(VolumeTrend volumeTrend) {
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
        boolean hasClearDirection = volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.UP.getValue()) ||
                volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.DOWN.getValue());
        if (hasClearDirection) {
            strengthPoint += 1;
        }
        // 3. Xu hướng tiếp diễn (không NEUTRAL)
        boolean isTrendContinuing = volumeTrend.getCurrTrendDirection().equals(volumeTrend.getPrevTrendDirection()) &&
                !volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.NEUTRAL.getValue());
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
                (volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.UP.getValue()) &&
                        volumeTrend.getCurrDivergence().compareTo(volumeTrend.getPrevDivergence()) > 0) ||
                        (volumeTrend.getCurrTrendDirection().equals(VolumeTrendEnum.DOWN.getValue()) &&
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
    public static int isEmaBullishTrend(BigDecimal shortPrevEmaPrice, BigDecimal shortCurrEmaPrice,
                                        BigDecimal longEmaPrice, BigDecimal emaThreshold) {
        // Kiểm tra EMA ngắn hiện tại có vượt EMA dài không
        boolean isCurrAbove = shortCurrEmaPrice.compareTo(longEmaPrice) > 0;
        // Kiểm tra crossover truyền thống
        boolean isTraditionalCrossover = shortPrevEmaPrice.compareTo(longEmaPrice) <= 0 && isCurrAbove;
        // Tính % chênh lệch để kiểm soát độ nhạy
        BigDecimal diffPercent = shortCurrEmaPrice.subtract(longEmaPrice)
                .abs().divide(longEmaPrice, SCALE, ROUNDING_MODE);

        if(isTraditionalCrossover && diffPercent.compareTo(emaThreshold) >= 0) {
            log.info("BullishTrend - Big crossover detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
            return 4; //big crossover - DK_1
        }
        if(isTraditionalCrossover) {
            log.info("BullishTrend - Normal crossover detected: shortPrev={}, shortCurr={}, longEmaPrice={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice);
            return 3; //crossover - DK_2
        }
        if(isCurrAbove && diffPercent.compareTo(emaThreshold) >= 0) {
            log.info("BullishTrend - Normal bullish detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
            return 2; //normal bullish - DK_3
        }
        if(isCurrAbove) {
            log.info("BullishTrend - Small bullish detected: shortPrev={}, shortCurr={}, longEmaPrice={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice);
            return 1; //small bullish - DK_4
        }
        log.info("BullishTrend - No bullish trend detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
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
    public static int isEmaBearishTrend(BigDecimal shortPrevEmaPrice, BigDecimal shortCurrEmaPrice,
                                        BigDecimal longEmaPrice, BigDecimal emaThreshold) {
        // Kiểm tra EMA ngắn hiện tại có dưới EMA dài không
        boolean isCurrBelow = shortCurrEmaPrice.compareTo(longEmaPrice) < 0;
        // Kiểm tra crossover truyền thống
        boolean isTraditionalCrossover = shortPrevEmaPrice.compareTo(longEmaPrice) >= 0 && isCurrBelow;
        // Tính % chênh lệch
        BigDecimal diffPercent = shortCurrEmaPrice.subtract(longEmaPrice)
                .abs().divide(longEmaPrice, SCALE, ROUNDING_MODE);

        if(isTraditionalCrossover && diffPercent.compareTo(emaThreshold) >= 0) {
            log.info("BearishTrend - Big crossover detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
            return 4; //big crossover - DK_1
        }
        if(isTraditionalCrossover) {
            log.info("BearishTrend - Normal crossover detected: shortPrev={}, shortCurr={}, longEmaPrice={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice);
            return 3; //crossover - DK_2
        }
        if(isCurrBelow && diffPercent.compareTo(emaThreshold) >= 0) {
            log.info("BearishTrend - Normal bearish detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
            return 2; //normal bearish - DK_3
        }
        if(isCurrBelow) {
            log.info("BearishTrend - Small bearish detected: shortPrev={}, shortCurr={}, longEmaPrice={}",
                    shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice);
            return 1; //small bearish - DK_4
        }
        log.info("BearishTrend - No bullish trend detected: shortPrev={}, shortCurr={}, longEmaPrice={}, diff%={}, threshold={}",
                shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, diffPercent, emaThreshold);
        return 0; // Không thỏa mãn
    }
}
