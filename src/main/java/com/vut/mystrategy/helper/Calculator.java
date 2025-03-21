package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.LotSizeResponse;
import com.vut.mystrategy.model.SideEnum;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
public class Calculator {

    public static final int SCALE = 8;
    public static final BigDecimal ONE_HUNDRED = new BigDecimal(100);
    public static final RoundingMode ROUNDING_MODE_HALF_UP = RoundingMode.HALF_UP;

    public static BigDecimal calculateQuantity(LotSizeResponse binanceFutureLotSize,
                                           BigDecimal amount, BigDecimal price) {
        // Tính quantity thô
        BigDecimal quantity = amount.divide(price, SCALE, ROUNDING_MODE_HALF_UP);
        // Làm tròn theo step size
        BigDecimal multiplier = BigDecimal.ONE.divide(binanceFutureLotSize.getStepSizeAsBigDecimal(), 0, ROUNDING_MODE_HALF_UP);
        BigDecimal roundedQuantity = quantity.multiply(multiplier)
                .setScale(0, ROUNDING_MODE_HALF_UP)
                .divide(multiplier, SCALE, ROUNDING_MODE_HALF_UP);

        // Kiểm tra minimum notional (5 USDT cho Futures)
        BigDecimal notional = roundedQuantity.multiply(price);
        if (notional.compareTo(BigDecimal.valueOf(5)) < 0) {
            log.warn("Order value {} USDT for {} below min notional 5 USDT",
                    notional, binanceFutureLotSize.getSymbol());
        }

        return roundedQuantity;
    }

    public static BigDecimal calculateEmaPrice(BigDecimal currPrice, BigDecimal prevEmaPrice,
                                               BigDecimal smoothingFactor) {
        //dùng trọng số (smoothingFactor = 0.3333) để đề cao vai trò của prevEmaPrice (1 - 0.3333 = 0.6667)
        return smoothingFactor.multiply(currPrice)
                .add((BigDecimal.ONE.subtract(smoothingFactor)).multiply(prevEmaPrice))
                .setScale(SCALE, ROUNDING_MODE_HALF_UP);
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

        return sum.divide(BigDecimal.valueOf(list.size()), SCALE, ROUNDING_MODE_HALF_UP);
    }

    public static BigDecimal calculateRatio(BigDecimal decimal1, BigDecimal decimal2) {
        return decimal1.min(decimal2)
                .divide(decimal1.max(decimal2), 2, ROUNDING_MODE_HALF_UP);
    }

    public static BigDecimal calculateChangeRate(BigDecimal newDecimal, BigDecimal prevDecimal) {
        return newDecimal.subtract(prevDecimal)
                .divide(prevDecimal, SCALE, ROUNDING_MODE_HALF_UP);
    }

    public static BigDecimal calculateEmaSmoothingFactor(int emaPeriod) {
        return new BigDecimal(2).divide(
                new BigDecimal(emaPeriod + 1), 4, ROUNDING_MODE_HALF_UP); // 2/(5+1) = 0.3333
    }

    public static BigDecimal calculateVolumeBasedOnWeight(BigDecimal takerVolume, BigDecimal makerVolume,
                                                          BigDecimal sumVolumeTakerWeight, BigDecimal sumVolumeMakerWeight) {
        // Ex: bullVolume = (0.6 * bullTakerVolume) + (0.4 * bullMakerVolume)
        return sumVolumeTakerWeight.multiply(takerVolume)
                .add(sumVolumeMakerWeight.multiply(makerVolume));
    }

    public static BigDecimal calculatePriceWithSlippage(BigDecimal price, BigDecimal slippage, String side) {
        if (price == null || slippage == null || side == null) {
            throw new IllegalArgumentException("Price, slippage, and side must not be null");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (slippage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Slippage must be non-negative");
        }

        // Worse case: BUY thì giá tăng, SELL thì giá giảm
        BigDecimal factor = side.equalsIgnoreCase(SideEnum.SIDE_BUY.getValue())
                ? BigDecimal.ONE.add(slippage)
                : BigDecimal.ONE.subtract(slippage);

        return price.multiply(factor).setScale(2, ROUNDING_MODE_HALF_UP);
    }
}
