package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.LotSizeResponse;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
public class Calculator {

    private static final int SCALE = 8;

    public static String calculateQuantity(LotSizeResponse binanceFutureLotSize,
                                           BigDecimal amount, BigDecimal price) {
        // Tính quantity thô
        BigDecimal quantity = amount.divide(price, SCALE, RoundingMode.DOWN);
        // Làm tròn theo step size
        BigDecimal multiplier = BigDecimal.ONE.divide(binanceFutureLotSize.getStepSizeAsBigDecimal(), 0, RoundingMode.DOWN);
        BigDecimal roundedQuantity = quantity.multiply(multiplier)
                .setScale(0, RoundingMode.DOWN)
                .divide(multiplier, SCALE, RoundingMode.DOWN);

        // Kiểm tra minimum notional (5 USDT cho Futures)
        BigDecimal notional = roundedQuantity.multiply(price);
        if (notional.compareTo(BigDecimal.valueOf(5)) < 0) {
            log.warn("Order value {} USDT for {} below min notional 5 USDT", notional, binanceFutureLotSize.getSymbol());
        }

        return roundedQuantity.stripTrailingZeros().toPlainString();
    }

    public static BigDecimal calculateSmaPrice(List<TradeEvent> groupTradeEvents, int smaPeriod) {
        if(groupTradeEvents.size() == smaPeriod) {
            BigDecimal sum = BigDecimal.ZERO;
            for (TradeEvent tradeEvent : groupTradeEvents) {
                sum = sum.add(tradeEvent.getPriceAsBigDecimal());
            }
            return sum.divide(BigDecimal.valueOf(smaPeriod), SCALE, RoundingMode.DOWN);
        }
        return null;
    }

    public static BigDecimal calculateEmaPrice(BigDecimal currPrice, BigDecimal prevEmaPrice,
                                               BigDecimal smoothingFactor) {
        return smoothingFactor.multiply(currPrice)
                .add((BigDecimal.ONE.subtract(smoothingFactor)).multiply(prevEmaPrice))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculatePercentPriceChange(BigDecimal currAvg, BigDecimal prevAvg) {
        return currAvg.subtract(prevAvg)
                .divide(prevAvg, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
}
