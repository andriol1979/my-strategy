package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.LotSizeResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public class Calculator {

    public static final double WEIGHT_NUMBER_MEDIUM = 1.5;
    public static final double WEIGHT_NUMBER_HIGH = 2.0;
    public static final int SCALE = 8;
    public static final BigDecimal one = BigDecimal.ONE;
    public static final RoundingMode ROUNDING_MODE_HALF_UP = RoundingMode.HALF_UP;

    public static void main(String[] args) {
        BigDecimal entryPrice = new BigDecimal("16565.00000000");    // Giá vào lệnh
        BigDecimal exitPrice = new BigDecimal("16730.65000000");     // Giá thoát lệnh
        BigDecimal slippage = new BigDecimal("0.001");               // 0.1% slippage
        int leverage = 5;                                            // Đòn bẩy x5
        BigDecimal orderVolume = new BigDecimal("50");    // Order volume (Notional Value) = 50 USD

        // Tính PnL cho cả LONG và SHORT
        BigDecimal longPnL = calculateLongPnL(entryPrice, exitPrice, slippage, orderVolume, leverage);
        BigDecimal shortPnL = calculateShortPnL(entryPrice, exitPrice, slippage, orderVolume, leverage);

        // In kết quả với 8 chữ số thập phân
        System.out.printf("Position Size (Notional Value): %.8f%n", orderVolume);
        System.out.printf("PnL khi BUY LONG: %.8f%n", longPnL);
        System.out.printf("PnL khi SELL SHORT: %.8f%n", shortPnL);
    }

    public static BigDecimal calculateBuySellVolumePercentageInEntryCase(double buySellVolumePercentage) {
        return BigDecimal.valueOf(buySellVolumePercentage * 1.5);
    }

    public static Pair<BigDecimal, BigDecimal> calculateLotSize(LotSizeResponse binanceFutureLotSize,
                                                                BigDecimal orderAmount, BigDecimal priceAsset) {
        // Tính quantity thô
        BigDecimal quantity = orderAmount.divide(priceAsset, SCALE, ROUNDING_MODE_HALF_UP);
        log.info("CalculateQuantity: Price: {} - Amount: {} - quantity: {}", priceAsset, orderAmount, quantity);
        // Làm tròn theo step size
        BigDecimal multiplier = BigDecimal.ONE.divide(binanceFutureLotSize.getStepSizeAsBigDecimal(), 0, ROUNDING_MODE_HALF_UP);
        BigDecimal roundedQuantity = quantity.multiply(multiplier)
                .setScale(0, ROUNDING_MODE_HALF_UP)
                .divide(multiplier, SCALE, ROUNDING_MODE_HALF_UP);
        BigDecimal newOrderVolume = roundedQuantity.multiply(priceAsset); //USDT base on roundQuantity

        return Pair.of(roundedQuantity, newOrderVolume);
    }

    // Tính PnL cho lệnh BUY LONG
    public static BigDecimal calculateLongPnL(BigDecimal entryPrice, BigDecimal exitPrice,
                                              BigDecimal slippage, BigDecimal orderVolume, int leverage) {
        // Điều chỉnh giá với slippage (worst case)
        BigDecimal adjustedEntryPrice = entryPrice.multiply(one.add(slippage));  // Entry × (1 + slippage)
        BigDecimal adjustedExitPrice = exitPrice.multiply(one.subtract(slippage)); // Exit × (1 - slippage)

        // Tính position size thực tế với leverage
        BigDecimal effectivePositionSize = orderVolume.multiply(BigDecimal.valueOf(leverage));

        // PnL = (Adjusted Exit - Adjusted Entry) × Effective Position Size / Entry Price

        return adjustedExitPrice.subtract(adjustedEntryPrice)
                .multiply(effectivePositionSize)
                .divide(entryPrice, SCALE, ROUNDING_MODE_HALF_UP)
                .setScale(2, ROUNDING_MODE_HALF_UP);
    }

    // Tính PnL cho lệnh SELL SHORT
    public static BigDecimal calculateShortPnL(BigDecimal entryPrice, BigDecimal exitPrice,
                                               BigDecimal slippage, BigDecimal orderVolume, int leverage) {
        // Điều chỉnh giá với slippage (worst case)
        BigDecimal adjustedEntryPrice = entryPrice.multiply(one.subtract(slippage)); // Entry × (1 - slippage)
        BigDecimal adjustedExitPrice = exitPrice.multiply(one.add(slippage));       // Exit × (1 + slippage)

        // Tính position size thực tế với leverage
        BigDecimal effectivePositionSize = orderVolume.multiply(BigDecimal.valueOf(leverage));

        // PnL = (Adjusted Entry - Adjusted Exit) × Effective Position Size / Entry Price

        return adjustedEntryPrice.subtract(adjustedExitPrice)
                .multiply(effectivePositionSize)
                .divide(entryPrice, SCALE, ROUNDING_MODE_HALF_UP)
                .setScale(2, ROUNDING_MODE_HALF_UP);
    }
}
