package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.LotSizeResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public class Calculator {

    public static final int SCALE = 8;
    public static final BigDecimal ONE_HUNDRED = new BigDecimal(100);
    public static final RoundingMode ROUNDING_MODE_HALF_UP = RoundingMode.HALF_UP;

    public static Pair<BigDecimal, BigDecimal> calculateLotSize(LotSizeResponse binanceFutureLotSize,
                                                                BigDecimal amount, BigDecimal price) {
        // Tính quantity thô
        BigDecimal quantity = amount.divide(price, SCALE, ROUNDING_MODE_HALF_UP);
        log.info("CalculateQuantity: Price: {} - Amount: {} - quantity: {}", price, amount, quantity);
        // Làm tròn theo step size
        BigDecimal multiplier = BigDecimal.ONE.divide(binanceFutureLotSize.getStepSizeAsBigDecimal(), 0, ROUNDING_MODE_HALF_UP);
        BigDecimal roundedQuantity = quantity.multiply(multiplier)
                .setScale(0, ROUNDING_MODE_HALF_UP)
                .divide(multiplier, SCALE, ROUNDING_MODE_HALF_UP);
        BigDecimal newOrderVolume = roundedQuantity.multiply(price); //USDT base on roundQuantity

        return Pair.of(roundedQuantity, newOrderVolume);
    }
}
