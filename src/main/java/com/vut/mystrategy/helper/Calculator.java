package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.LotSizeResponse;
import lombok.extern.slf4j.Slf4j;

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
            return BigDecimal.ZERO;
        }
        BigDecimal sum = list.stream()
                .map(priceMapper)
                .filter(Objects::nonNull) // Lọc giá trị null
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(list.size()), SCALE, ROUNDING_MODE);
    }

    public static BigDecimal getPercentChange(BigDecimal currAvg, BigDecimal prevAvg) {
        return currAvg.subtract(prevAvg)
                .divide(prevAvg, 4, ROUNDING_MODE)
                .multiply(ONE_HUNDRED).setScale(2, ROUNDING_MODE);
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
        BigDecimal strength = newDivergence.abs();
        if (prevTotalVolume.compareTo(BigDecimal.ZERO) == 0) {
            return strength; // Giữ nguyên strength nếu prevTotalVolume = 0
        }
        BigDecimal volumeChangePercent = newTotalVolume.subtract(prevTotalVolume)
                .divide(prevTotalVolume, SCALE, ROUNDING_MODE)
                .multiply(ONE_HUNDRED);
        BigDecimal adjustmentFactor = BigDecimal.ONE.add(volumeChangePercent.divide(ONE_HUNDRED, SCALE, ROUNDING_MODE));
        if (volumeChangePercent.abs().compareTo(divergenceThreshold) > 0) { // Ngưỡng 10%
            strength = strength.multiply(adjustmentFactor.max(new BigDecimal("0.5")).min(new BigDecimal("1.5")));
        }

        return strength;
    }
}
