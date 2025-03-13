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
        //dùng trọng số (smoothingFactor = 0.3333) để đề cao vai trò của prevEmaPrice (1 - 0.3333 = 0.6667)
        return smoothingFactor.multiply(currPrice)
                .add((BigDecimal.ONE.subtract(smoothingFactor)).multiply(prevEmaPrice))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculatePercentPriceChange(BigDecimal currAvg, BigDecimal prevAvg) {
        return currAvg.subtract(prevAvg)
                .divide(prevAvg, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateEmaSmoothingFactor(int emaPeriod) {
        return new BigDecimal(2).divide(
                new BigDecimal(emaPeriod + 1), 4, RoundingMode.DOWN); // 2/(5+1) = 0.3333
    }

    public static BigDecimal calculateBullBearVolumeDivergence(BigDecimal bullVolume, BigDecimal bearVolume) {
        BigDecimal bullBearVolumeDivergence;
        final BigDecimal ONE_HUNDRED = new BigDecimal(100);
        final BigDecimal sumBullBearVolume = bullVolume.add(bearVolume);
        boolean bullVolumeIsZero = bullVolume.compareTo(BigDecimal.ZERO) == 0;
        boolean bearVolumeIsZero = bearVolume.compareTo(BigDecimal.ZERO) == 0;

        if (bullVolumeIsZero && bearVolumeIsZero) {
            bullBearVolumeDivergence = BigDecimal.ZERO;
        }
        else if (bearVolumeIsZero) {
            bullBearVolumeDivergence = new BigDecimal(100); // Bull thắng tuyệt đối
        }
        else if (bullVolumeIsZero) {
            bullBearVolumeDivergence = new BigDecimal(-100); // Bear thắng tuyệt đối
        }
        else if (bullVolume.compareTo(bearVolume) > 0) {
            bullBearVolumeDivergence = ((bullVolume.subtract(bearVolume))
                    .divide(sumBullBearVolume, SCALE, RoundingMode.DOWN))
                    .multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
        }
        else {
            bullBearVolumeDivergence = ((bearVolume.subtract(bullVolume))
                    .divide(sumBullBearVolume, SCALE, RoundingMode.DOWN))
                    .multiply(ONE_HUNDRED)
                    .negate().setScale(2, RoundingMode.HALF_UP);
        }

        return bullBearVolumeDivergence;
    }
}
