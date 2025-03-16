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

    public static double analyzeVolumeTrendStrengthPoint(VolumeTrend volumeTrend) {
        /* calculate based on current trend direction
        if UP: + when everything is UP
        if DOWN: + when everything is DOWN
        important point:
            - current UP/DOWN: 0.1
            - continue UP/DOWN: 0.2
            - bull_bear_divergence > threshold(config): 0.2
            - bull_bear_divergence continue positive (UP) = current divergence continue > previous divergence: 0.2
            - volume spike: BULL/BEAR: 0.2 - FLAT = 0
            - volumeTrendStrength: % newTotalVolume and prevTotalVolume:
                + prevTotalVolume < newTotalVolume: The market is hot (good): 0.1
                + prevTotalVolume > newTotalVolume: The market is quiet (bad): 0
        Result:
            positive strength point is BULL
            negative strength point is BEAR
         */
        double strengthLevel = 0;
        if(volumeTrend.getPrevTrendStrength() == null ||
                 volumeTrend.getPrevDivergence() == null ||
                StringUtils.isEmpty(volumeTrend.getPrevTrendDirection())) {
            log.warn("VolumeTrend is not enough data to analyze VolumeTrendStrengthPoint - Strength point = {}", strengthLevel);
            return strengthLevel;
        }
        if(volumeTrend.getCurrTrendStrength().compareTo(volumeTrend.getPrevTrendStrength()) > 0) {
            log.info("analyzeVolumeTrendStrengthPoint - CurrTrendStrength({}) > PrevTrendStrength({})",
                    volumeTrend.getCurrTrendStrength(), volumeTrend.getPrevTrendStrength());
            strengthLevel += 0.1;
        }
        String currTrendDirection = volumeTrend.getCurrTrendDirection();
        if(currTrendDirection.equals(VolumeTrendEnum.UP.getValue())) {
            if(volumeTrend.getPrevTrendDirection().equals(VolumeTrendEnum.UP.getValue())) {
                log.info("analyzeVolumeTrendStrengthPoint - PrevTrendDirection({}) == UP",
                        volumeTrend.getPrevTrendDirection());
                strengthLevel += 0.2;
            }
            // bull and bear Divergence > 10 ~ bull > bear 10%
            if(volumeTrend.getCurrDivergence().compareTo(new BigDecimal("0.15")) > 0) {
                log.info("analyzeVolumeTrendStrengthPoint - CurrDivergence({}) == 0.15",
                        volumeTrend.getCurrDivergence());
                strengthLevel += 0.2;
            }
            if(volumeTrend.getCurrDivergence().compareTo(volumeTrend.getPrevDivergence()) > 0) {
                log.info("analyzeVolumeTrendStrengthPoint - CurrDivergence({}) > PrevDivergence({})",
                        volumeTrend.getCurrDivergence(), volumeTrend.getPrevDivergence());
                strengthLevel += 0.1;
            }
            if(volumeTrend.getVolumeSpike().equals(VolumeSpikeEnum.BULL.getValue())) {
                log.info("analyzeVolumeTrendStrengthPoint - VolumeSpike({}) == BULL",
                        volumeTrend.getVolumeSpike());
                strengthLevel += 0.2;
            }
        }
        else if(currTrendDirection.equals(VolumeTrendEnum.DOWN.getValue())) {
            if(volumeTrend.getPrevTrendDirection().equals(VolumeTrendEnum.DOWN.getValue())) {
                log.info("analyzeVolumeTrendStrengthPoint - PrevTrendDirection({}) == DOWN",
                        volumeTrend.getPrevTrendDirection());
                strengthLevel += 0.2;
            }
            // bull and bear Divergence > 10 ~ bear > bull 10%. Divergence is negative number
            if(volumeTrend.getCurrDivergence().compareTo(new BigDecimal("0.15").negate()) < 0) {
                log.info("analyzeVolumeTrendStrengthPoint - CurrDivergence({}) < -1.5",
                        volumeTrend.getCurrDivergence());
                strengthLevel += 0.2;
            }
            if(volumeTrend.getCurrDivergence().compareTo(volumeTrend.getPrevDivergence()) < 0) {
                log.info("analyzeVolumeTrendStrengthPoint - CurrDivergence({}) < PrevDivergence({})",
                        volumeTrend.getCurrDivergence(), volumeTrend.getPrevDivergence());
                strengthLevel += 0.1;
            }
            if(volumeTrend.getVolumeSpike().equals(VolumeSpikeEnum.BEAR.getValue())) {
                log.info("analyzeVolumeTrendStrengthPoint - VolumeSpike({}) == BEAR",
                        volumeTrend.getVolumeSpike());
                strengthLevel += 0.2;
            }
            strengthLevel *= -1;
        }

        log.info("Analyzed VolumeTrendStrengthPoint - Strength point = {}", strengthLevel);
        return strengthLevel;
    }

    public static int analyzePriceNearResistanceOrSupport(BigDecimal price, BigDecimal resistance,
                                                          BigDecimal support, BigDecimal priceThresholdPercent,
                                                          boolean isBullishCrossOver, boolean isBearishCrossOver) {
        if (resistance.compareTo(support) == 0) {
            throw new IllegalArgumentException("Resistance and Support cannot be equal");
        }
        BigDecimal priceThreshold = price.multiply(priceThresholdPercent);
        BigDecimal distanceToResistance = price.subtract(resistance).abs();
        BigDecimal distanceToSupport = price.subtract(support).abs();
        if (distanceToResistance.compareTo(priceThreshold) > 0 && distanceToSupport.compareTo(priceThreshold) > 0) {
            return 0;
        }
        if (distanceToResistance.compareTo(priceThreshold) <= 0 &&
                distanceToSupport.compareTo(priceThreshold) <= 0) {
            //write log to debug support
            log.info("Price {} is near both resistance {} and support {}", price, resistance, support);
            if(isBullishCrossOver && distanceToResistance.compareTo(distanceToSupport) < 0) {
                return 1; //ưu tiên near resistance in case Bullish
            }
            if(isBearishCrossOver && distanceToResistance.compareTo(distanceToSupport) > 0) {
                return 2; //ưu tiên near support in case Bearish
            }
        }
        return distanceToResistance.compareTo(distanceToSupport) < 0 ? 1 : 2;
        /*
         Explain:
            - 0 (price is NOT near resistance or support, compare with priceThreshold)
            - 1 (price is near resistance)
            - 2 (price is near support)
         */
    }

    public static boolean isBullishCrossOver(BigDecimal shortPrevEmaPrice, BigDecimal shortCurrEmaPrice,
                                             BigDecimal longEmaPrice, BigDecimal emaThreshold) {
        // Kiểm tra null để tránh lỗi
        if (shortPrevEmaPrice == null || shortCurrEmaPrice == null ||
                longEmaPrice == null || emaThreshold == null) {
            return false;
        }

        // Điều kiện Bullish Crossover:
        // 1. Trước đó EMA ngắn ≤ EMA dài
        // 2. Hiện tại EMA ngắn > EMA dài
        // 3. Chênh lệch vượt ngưỡng threshold
        boolean wasBelowOrEqual = shortPrevEmaPrice.compareTo(longEmaPrice) <= 0;
        boolean isAbove = shortCurrEmaPrice.compareTo(longEmaPrice) > 0;

        // Tính % chênh lệch: |(shortCurrEmaPrice - longEmaPrice)| / longEmaPrice
        BigDecimal difference = shortCurrEmaPrice.subtract(longEmaPrice).abs();
        BigDecimal percentageDifference = difference.divide(longEmaPrice, SCALE, ROUNDING_MODE);
        log.info("BullishCrossOver - ShortPrevEmaPrice: {}, ShortCurrEmaPrice: {}, LongEmaPrice: {}, EmaThreshold: {}",
                shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, emaThreshold);
        return wasBelowOrEqual && isAbove && percentageDifference.compareTo(emaThreshold) >= 0;
    }

    public static boolean isBearishCrossOver(BigDecimal shortPrevEmaPrice, BigDecimal shortCurrEmaPrice,
                                             BigDecimal longEmaPrice, BigDecimal emaThreshold) {
        // Kiểm tra null để tránh lỗi
        if (shortPrevEmaPrice == null || shortCurrEmaPrice == null || longEmaPrice == null || emaThreshold == null) {
            return false;
        }

        // Điều kiện Bearish Crossover:
        // 1. Trước đó EMA ngắn ≥ EMA dài
        // 2. Hiện tại EMA ngắn < EMA dài
        // 3. Chênh lệch vượt ngưỡng threshold
        boolean wasAboveOrEqual = shortPrevEmaPrice.compareTo(longEmaPrice) >= 0;
        boolean isBelow = shortCurrEmaPrice.compareTo(longEmaPrice) < 0;

        // Tính % chênh lệch: |(shortCurrEmaPrice - longEmaPrice)| / longEmaPrice
        BigDecimal difference = shortCurrEmaPrice.subtract(longEmaPrice).abs();
        BigDecimal percentageDifference = difference.divide(longEmaPrice, SCALE, ROUNDING_MODE);
        log.info("BearishCrossOver - ShortPrevEmaPrice: {}, ShortCurrEmaPrice: {}, LongEmaPrice: {}, EmaThreshold: {}",
                shortPrevEmaPrice, shortCurrEmaPrice, longEmaPrice, emaThreshold);
        return wasAboveOrEqual && isBelow && percentageDifference.compareTo(emaThreshold) >= 0;
    }
}
