package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.LotSizeResponse;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
public class Calculator {
    public static String calculateQuantity(LotSizeResponse binanceFutureLotSize,
                                           BigDecimal amount, BigDecimal price) {
        // Tính quantity thô
        BigDecimal quantity = amount.divide(price, 8, RoundingMode.DOWN);
        // Làm tròn theo step size
        BigDecimal multiplier = BigDecimal.ONE.divide(binanceFutureLotSize.getStepSizeAsBigDecimal(), 0, RoundingMode.DOWN);
        BigDecimal roundedQuantity = quantity.multiply(multiplier)
                .setScale(0, RoundingMode.DOWN)
                .divide(multiplier, 8, RoundingMode.DOWN);

        // Kiểm tra minimum notional (5 USDT cho Futures)
        BigDecimal notional = roundedQuantity.multiply(price);
        if (notional.compareTo(BigDecimal.valueOf(5)) < 0) {
            log.warn("Order value {} USDT for {} below min notional 5 USDT", notional, binanceFutureLotSize.getSymbol());
        }

        return roundedQuantity.stripTrailingZeros().toPlainString();
    }

    public static String calculateTradeEventAveragePrice(List<TradeEvent> groupTradeEvents, int redisTradeEventGroupSize) {
        if(groupTradeEvents.size() == redisTradeEventGroupSize) {
            BigDecimal sum = BigDecimal.ZERO;
            for (TradeEvent tradeEvent : groupTradeEvents) {
                sum = sum.add(tradeEvent.getPriceAsBigDecimal());
            }
            BigDecimal average = sum.divide(BigDecimal.valueOf(redisTradeEventGroupSize), 8, RoundingMode.DOWN);
            return average.stripTrailingZeros().toPlainString();
        }
        return null;
    }
}
