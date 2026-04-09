package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.model.OrderResponseStorage;
import com.vut.mystrategy.model.SymbolConfig;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.OverIndicatorRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
public class DefensiveSpotDcaStrategy extends MyStrategyBase {
    private static final int FAST_EMA_PERIOD = 21;
    private static final int MID_EMA_PERIOD = 50;
    private static final int LONG_EMA_PERIOD = 200;
    private static final int EMA_SLOPE_LOOKBACK = 5;
    private static final int DCA_COOLDOWN_BARS = 6;
    private static final int MIN_BULL_STRUCTURE_LOOKBACK = 12;

    private static final BigDecimal DEFAULT_PULLBACK_BUFFER = new BigDecimal("0.003");
    private static final BigDecimal DEFAULT_TRAILING_ACTIVATION = new BigDecimal("0.05");
    private static final BigDecimal DEFAULT_TRAILING_DISTANCE = new BigDecimal("0.012");
    private static final BigDecimal DEFAULT_HARD_STOP = new BigDecimal("0.06");
    private static final int DEFAULT_MAX_HOLDING_BARS = 48;
    private static final int DEFAULT_MAX_DCA_COUNT = 2;
    private static final List<BigDecimal> DEFAULT_DCA_STEPS = List.of(
            new BigDecimal("0.03"),
            new BigDecimal("0.06")
    );

    private static final String EXIT_REASON_TRAILING = "TRAILING_EXIT";
    private static final String EXIT_REASON_HARD_STOP = "HARD_STOP";
    private static final String EXIT_REASON_REGIME_BREAK = "REGIME_BREAK";
    private static final String EXIT_REASON_TIME_STOP = "TIME_STOP";

    @Override
    public Strategy buildLongStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        Rule disabledRule = new OverIndicatorRule(closePrice, closePrice);
        return new BaseStrategy(disabledRule, disabledRule);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
        Rule disabledRule = new OverIndicatorRule(closePrice, closePrice);
        return new BaseStrategy(disabledRule, disabledRule);
    }

    public boolean shouldOpenLong(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (!hasEnoughBars(barSeries)) {
            return false;
        }

        MarketSnapshot snapshot = buildSnapshot(barSeries);
        BigDecimal closePrice = snapshot.closePrice;
        BigDecimal emaFast = snapshot.emaFast;
        BigDecimal emaMid = snapshot.emaMid;
        BigDecimal emaLong = snapshot.emaLong;

        boolean strongBullRegime = isBullRegime(snapshot);
        boolean notTooExtended = closePrice.compareTo(emaMid.multiply(new BigDecimal("1.025"))) <= 0;
        boolean closeNearFastEma = isCloseNearFastEma(closePrice, emaFast);
        boolean aboveReboundLine = closePrice.compareTo(emaFast) >= 0 && closePrice.compareTo(emaLong) >= 0;
        boolean recentStructureHealthy = hasRecentBullStructure(barSeries);

        return strongBullRegime && notTooExtended && closeNearFastEma && aboveReboundLine && recentStructureHealthy;
    }

    public boolean shouldAddToPosition(BarSeries barSeries, SymbolConfig symbolConfig, OrderResponseStorage storage) {
        if (!hasEnoughBars(barSeries) || storage == null || storage.getEntryResponse() == null) {
            return false;
        }
        if (storage.getDcaCount() == null) {
            storage.setDcaCount(0);
        }
        if (storage.getDcaCount() >= getMaxDcaCount(symbolConfig)) {
            return false;
        }

        MarketSnapshot snapshot = buildSnapshot(barSeries);
        if (!isBullRegime(snapshot) || isStrongDowntrend(snapshot)) {
            return false;
        }

        Integer lastEntryIndex = storage.getLastEntryIndex();
        if (lastEntryIndex != null && (barSeries.getEndIndex() - lastEntryIndex) < DCA_COOLDOWN_BARS) {
            return false;
        }

        BigDecimal averageEntryPrice = getAverageEntryPrice(storage);
        if (averageEntryPrice == null) {
            return false;
        }
        BigDecimal dcaThreshold = getDcaSteps(symbolConfig).get(storage.getDcaCount());
        BigDecimal triggerPrice = averageEntryPrice.multiply(BigDecimal.ONE.subtract(dcaThreshold));
        boolean reachedDcaZone = snapshot.closePrice.compareTo(triggerPrice) <= 0;
        boolean aboveLongTermSupport = snapshot.closePrice.compareTo(snapshot.emaLong.multiply(new BigDecimal("0.995"))) >= 0;
        boolean controlledPullback = snapshot.closePrice.compareTo(snapshot.emaMid.multiply(new BigDecimal("0.992"))) >= 0;
        boolean notKnifeCatch = snapshot.closePrice.compareTo(snapshot.emaFast) <= 0;

        return reachedDcaZone && aboveLongTermSupport && controlledPullback && notKnifeCatch;
    }

    public String getExitReason(BarSeries barSeries, SymbolConfig symbolConfig, OrderResponseStorage storage) {
        if (!hasEnoughBars(barSeries) || storage == null || storage.getEntryResponse() == null) {
            return null;
        }

        MarketSnapshot snapshot = buildSnapshot(barSeries);
        BigDecimal averageEntryPrice = getAverageEntryPrice(storage);
        if (averageEntryPrice == null) {
            return null;
        }

        BigDecimal hardStopPrice = averageEntryPrice.multiply(BigDecimal.ONE.subtract(getHardStopLoss(symbolConfig)));
        if (snapshot.closePrice.compareTo(hardStopPrice) <= 0) {
            return EXIT_REASON_HARD_STOP;
        }

        if (Boolean.TRUE.equals(storage.getTrailingActive())
                && storage.getTrailingStopPrice() != null
                && snapshot.closePrice.compareTo(storage.getTrailingStopPrice()) <= 0) {
            return EXIT_REASON_TRAILING;
        }

        if (!isBullRegime(snapshot) && snapshot.closePrice.compareTo(snapshot.emaMid) < 0) {
            return EXIT_REASON_REGIME_BREAK;
        }

        Integer initialEntryIndex = storage.getInitialEntryIndex();
        if (initialEntryIndex != null) {
            int holdingBars = barSeries.getEndIndex() - initialEntryIndex;
            BigDecimal minimumProgressPrice = averageEntryPrice.multiply(new BigDecimal("1.01"));
            if (holdingBars >= getMaxHoldingBars(symbolConfig)
                    && snapshot.closePrice.compareTo(minimumProgressPrice) <= 0) {
                return EXIT_REASON_TIME_STOP;
            }
        }

        return null;
    }

    public void refreshOpenPositionState(BarSeries barSeries, SymbolConfig symbolConfig, OrderResponseStorage storage) {
        if (!hasEnoughBars(barSeries) || storage == null || storage.getEntryResponse() == null) {
            return;
        }

        MarketSnapshot snapshot = buildSnapshot(barSeries);
        BigDecimal closePrice = snapshot.closePrice;
        BigDecimal averageEntryPrice = getAverageEntryPrice(storage);
        if (averageEntryPrice == null) {
            return;
        }

        BigDecimal existingPeak = storage.getPeakPrice();
        if (existingPeak == null || closePrice.compareTo(existingPeak) > 0) {
            storage.setPeakPrice(closePrice);
        }

        BigDecimal activationPrice = averageEntryPrice.multiply(BigDecimal.ONE.add(getTrailingActivationProfit(symbolConfig)));
        if (Boolean.TRUE.equals(storage.getTrailingActive()) || closePrice.compareTo(activationPrice) >= 0) {
            storage.setTrailingActive(true);
            BigDecimal trailingStop = storage.getPeakPrice()
                    .multiply(BigDecimal.ONE.subtract(getTrailingDistance(symbolConfig)))
                    .setScale(8, RoundingMode.HALF_UP);
            storage.setTrailingStopPrice(trailingStop);
        }
    }

    public String getStrategySummary(SymbolConfig symbolConfig) {
        return "Spot defensive DCA | maxDca=" + getMaxDcaCount(symbolConfig)
                + " | dcaSteps=" + getDcaSteps(symbolConfig)
                + " | trailingActivation=" + getTrailingActivationProfit(symbolConfig)
                + " | trailingDistance=" + getTrailingDistance(symbolConfig)
                + " | hardStop=" + getHardStopLoss(symbolConfig)
                + " | maxHoldingBars=" + getMaxHoldingBars(symbolConfig);
    }

    private boolean hasEnoughBars(BarSeries barSeries) {
        return barSeries != null && barSeries.getBarCount() > LONG_EMA_PERIOD + MIN_BULL_STRUCTURE_LOOKBACK;
    }

    private MarketSnapshot buildSnapshot(BarSeries barSeries) {
        int endIndex = barSeries.getEndIndex();
        ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(barSeries);
        EMAIndicator emaFast = new EMAIndicator(closePriceIndicator, FAST_EMA_PERIOD);
        EMAIndicator emaMid = new EMAIndicator(closePriceIndicator, MID_EMA_PERIOD);
        EMAIndicator emaLong = new EMAIndicator(closePriceIndicator, LONG_EMA_PERIOD);

        return new MarketSnapshot(
                toBigDecimal(closePriceIndicator.getValue(endIndex)),
                toBigDecimal(emaFast.getValue(endIndex)),
                toBigDecimal(emaMid.getValue(endIndex)),
                toBigDecimal(emaLong.getValue(endIndex)),
                toBigDecimal(emaFast.getValue(Math.max(0, endIndex - EMA_SLOPE_LOOKBACK))),
                toBigDecimal(emaMid.getValue(Math.max(0, endIndex - EMA_SLOPE_LOOKBACK))),
                toBigDecimal(emaLong.getValue(Math.max(0, endIndex - EMA_SLOPE_LOOKBACK)))
        );
    }

    private boolean isStrongDowntrend(MarketSnapshot snapshot) {
        boolean priceBelowLongEma = snapshot.closePrice.compareTo(snapshot.emaLong) < 0;
        boolean midBelowLong = snapshot.emaMid.compareTo(snapshot.emaLong) < 0;
        boolean midSlopeDown = snapshot.emaMid.compareTo(snapshot.emaMidLookback) < 0;
        return priceBelowLongEma && midBelowLong && midSlopeDown;
    }

    private boolean isBullRegime(MarketSnapshot snapshot) {
        boolean priceAboveLong = snapshot.closePrice.compareTo(snapshot.emaLong) >= 0;
        boolean stackedEma = snapshot.emaFast.compareTo(snapshot.emaMid) >= 0
                && snapshot.emaMid.compareTo(snapshot.emaLong) >= 0;
        boolean fastSlopeUp = snapshot.emaFast.compareTo(snapshot.emaFastLookback) >= 0;
        boolean midSlopeUp = snapshot.emaMid.compareTo(snapshot.emaMidLookback) >= 0;
        boolean longSlopeFlatOrUp = snapshot.emaLong.compareTo(snapshot.emaLongLookback) >= 0;
        return priceAboveLong && stackedEma && fastSlopeUp && midSlopeUp && longSlopeFlatOrUp;
    }

    private boolean isCloseNearFastEma(BigDecimal closePrice, BigDecimal emaFast) {
        BigDecimal lowerBound = emaFast.multiply(BigDecimal.ONE.subtract(new BigDecimal("0.002")));
        BigDecimal upperBound = emaFast.multiply(BigDecimal.ONE.add(DEFAULT_PULLBACK_BUFFER));
        return closePrice.compareTo(lowerBound) >= 0 && closePrice.compareTo(upperBound) <= 0;
    }

    private boolean hasRecentBullStructure(BarSeries barSeries) {
        int endIndex = barSeries.getEndIndex();
        int startIndex = Math.max(1, endIndex - MIN_BULL_STRUCTURE_LOOKBACK);
        int greenBars = 0;
        for (int index = startIndex; index <= endIndex; index++) {
            BigDecimal open = new BigDecimal(barSeries.getBar(index).getOpenPrice().toString());
            BigDecimal close = new BigDecimal(barSeries.getBar(index).getClosePrice().toString());
            if (close.compareTo(open) >= 0) {
                greenBars++;
            }
        }
        return greenBars >= 4;
    }

    private BigDecimal applyUpBuffer(BigDecimal base, BigDecimal buffer) {
        return base.multiply(BigDecimal.ONE.add(buffer));
    }

    private BigDecimal toBigDecimal(Num value) {
        return new BigDecimal(value.toString());
    }

    private BigDecimal getAverageEntryPrice(OrderResponseStorage storage) {
        try {
            return storage.getEntryResponse().as(com.vut.mystrategy.model.binance.BinanceOrderResponse.class).getAvgPriceAsBigDecimal();
        } catch (Exception exception) {
            log.warn("Cannot read average entry price from storage", exception);
            return null;
        }
    }

    private int getMaxDcaCount(SymbolConfig symbolConfig) {
        return symbolConfig.getMaxDcaCount() == null ? DEFAULT_MAX_DCA_COUNT : symbolConfig.getMaxDcaCount();
    }

    private List<BigDecimal> getDcaSteps(SymbolConfig symbolConfig) {
        List<BigDecimal> configuredSteps = symbolConfig.getDcaStepPercentages();
        if (configuredSteps == null || configuredSteps.isEmpty()) {
            return DEFAULT_DCA_STEPS;
        }
        return configuredSteps;
    }

    private BigDecimal getTrailingActivationProfit(SymbolConfig symbolConfig) {
        return symbolConfig.getTrailingActivationProfit() == null
                ? DEFAULT_TRAILING_ACTIVATION
                : symbolConfig.getTrailingActivationProfit();
    }

    private BigDecimal getTrailingDistance(SymbolConfig symbolConfig) {
        return symbolConfig.getTrailingDistance() == null
                ? DEFAULT_TRAILING_DISTANCE
                : symbolConfig.getTrailingDistance();
    }

    private BigDecimal getHardStopLoss(SymbolConfig symbolConfig) {
        return symbolConfig.getHardStopLoss() == null
                ? DEFAULT_HARD_STOP
                : symbolConfig.getHardStopLoss();
    }

    private int getMaxHoldingBars(SymbolConfig symbolConfig) {
        return symbolConfig.getMaxHoldingBars() == null
                ? DEFAULT_MAX_HOLDING_BARS
                : symbolConfig.getMaxHoldingBars();
    }

    private record MarketSnapshot(
            BigDecimal closePrice,
            BigDecimal emaFast,
            BigDecimal emaMid,
            BigDecimal emaLong,
            BigDecimal emaFastLookback,
            BigDecimal emaMidLookback
            , BigDecimal emaLongLookback
    ) {
    }
}
