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
    private static final int ENTRY_CONFIRMATION_LOOKBACK = 6;

    private static final BigDecimal DEFAULT_PULLBACK_BUFFER = new BigDecimal("0.008");
    private static final BigDecimal DEFAULT_PULLBACK_LOWER_BUFFER = new BigDecimal("0.006");
    private static final BigDecimal DEFAULT_ENTRY_BREAKOUT_BUFFER = new BigDecimal("0.0015");
    private static final BigDecimal DEFAULT_FAST_ABOVE_MID_BUFFER = new BigDecimal("0.0015");
    private static final BigDecimal DEFAULT_MID_ABOVE_LONG_BUFFER = new BigDecimal("0.0020");
    private static final BigDecimal DEFAULT_MID_SUPPORT_BUFFER = new BigDecimal("0.0040");
    private static final BigDecimal DEFAULT_LONG_SUPPORT_BUFFER = new BigDecimal("0.0100");
    private static final BigDecimal DEFAULT_RECENT_DRAWDOWN_LIMIT = new BigDecimal("0.025");
    private static final BigDecimal DEFAULT_TRAILING_ACTIVATION = new BigDecimal("0.05");
    private static final BigDecimal DEFAULT_TRAILING_DISTANCE = new BigDecimal("0.012");
    private static final BigDecimal DEFAULT_HARD_STOP = new BigDecimal("0.06");
    private static final int DEFAULT_MAX_HOLDING_BARS = 48;
    private static final BigDecimal DEFAULT_SOFT_BREAK_RESCUE_MAX_DRAWDOWN = new BigDecimal("0.03");
    private static final int DEFAULT_MAX_DCA_COUNT = 2;
    private static final List<BigDecimal> DEFAULT_DCA_STEPS = List.of(
            new BigDecimal("0.03"),
            new BigDecimal("0.06")
    );

    private static final String EXIT_REASON_TRAILING = "TRAILING_EXIT";
    private static final String EXIT_REASON_HARD_STOP = "HARD_STOP";
    private static final String EXIT_REASON_REGIME_BREAK_HARD = "REGIME_BREAK_HARD";
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
        return evaluateOpenLong(barSeries, symbolConfig).shouldEnter();
    }

    public String explainOpenLongDecision(BarSeries barSeries, SymbolConfig symbolConfig) {
        EntryDecision decision = evaluateOpenLong(barSeries, symbolConfig);
        return "shouldEnter=" + decision.shouldEnter()
                + ", enoughBars=" + decision.enoughBars()
                + ", strongBullRegime=" + decision.strongBullRegime()
                + ", notTooExtended=" + decision.notTooExtended()
                + ", pullbackNearTrend=" + decision.pullbackNearTrend()
                + ", aboveReboundLine=" + decision.aboveReboundLine()
                + ", pullbackStillRespectingTrend=" + decision.pullbackStillRespectingTrend()
                + ", reboundConfirmed=" + decision.reboundConfirmed()
                + ", noRecentSharpSelloff=" + decision.noRecentSharpSelloff();
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
        boolean hardRegimeBreak = isHardRegimeBreak(snapshot, symbolConfig);
        boolean softRegimeBreak = isSoftRegimeBreak(snapshot, symbolConfig);
        if ((hardRegimeBreak && !softRegimeBreak) || isStrongDowntrend(snapshot)) {
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
        BigDecimal longSupportMultiplier = softRegimeBreak ? BigDecimal.ONE : new BigDecimal("0.995");
        BigDecimal midSupportMultiplier = softRegimeBreak ? new BigDecimal("0.985") : new BigDecimal("0.992");
        boolean aboveLongTermSupport = snapshot.closePrice.compareTo(snapshot.emaLong.multiply(longSupportMultiplier)) >= 0;
        boolean controlledPullback = snapshot.closePrice.compareTo(snapshot.emaMid.multiply(midSupportMultiplier)) >= 0;
        boolean notKnifeCatch = snapshot.closePrice.compareTo(snapshot.emaFast) <= 0;
        boolean normalDcaAllowed = reachedDcaZone && aboveLongTermSupport && controlledPullback && notKnifeCatch;
        if (!normalDcaAllowed) {
            return false;
        }
        if (!softRegimeBreak) {
            return true;
        }

        return isSoftRecoveryDcaCandidate(barSeries, storage, averageEntryPrice, snapshot);
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

        if (isHardRegimeBreak(snapshot, symbolConfig)) {
            return EXIT_REASON_REGIME_BREAK_HARD;
        }

        if (isSoftRegimeBreak(snapshot, symbolConfig)) {
            if (storage.getSoftBreakStartIndex() == null) {
                storage.setSoftBreakStartIndex(barSeries.getEndIndex());
            }
            return null;
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

        if (!isSoftRegimeBreak(snapshot, symbolConfig) || closePrice.compareTo(snapshot.emaMid) >= 0) {
            storage.setSoftBreakStartIndex(null);
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

    private EntryDecision evaluateOpenLong(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (!hasEnoughBars(barSeries)) {
            return new EntryDecision(false, false, false, false, false, false, false, false, false);
        }

        MarketSnapshot snapshot = buildSnapshot(barSeries);
        BigDecimal closePrice = snapshot.closePrice;
        BigDecimal emaFast = snapshot.emaFast;
        BigDecimal emaMid = snapshot.emaMid;

        boolean strongBullRegime = isBullRegime(snapshot, symbolConfig);
        boolean notTooExtended = closePrice.compareTo(emaMid.multiply(new BigDecimal("1.025"))) <= 0;
        boolean pullbackNearTrend = isCloseNearFastEma(closePrice, emaFast)
                || closePrice.compareTo(emaMid.multiply(new BigDecimal("1.003"))) <= 0;
        boolean aboveReboundLine = closePrice.compareTo(emaFast) >= 0;
        boolean pullbackStillRespectingTrend = recentPullbackHeldTrend(barSeries, snapshot);
        boolean reboundConfirmed = hasBullishReboundConfirmation(barSeries, snapshot);
        boolean noRecentSharpSelloff = !hasRecentSharpSelloff(barSeries, symbolConfig)
                || closePrice.compareTo(emaMid) >= 0;
        boolean closeAboveMidIfRequired = !Boolean.TRUE.equals(symbolConfig.getRequireCloseAboveMidForEntry())
                || closePrice.compareTo(emaMid) >= 0;
        boolean shouldEnter = strongBullRegime
                && notTooExtended
                && pullbackNearTrend
                && aboveReboundLine
                && pullbackStillRespectingTrend
                && reboundConfirmed
                && closeAboveMidIfRequired
                && noRecentSharpSelloff;

        return new EntryDecision(true, shouldEnter, strongBullRegime, notTooExtended, pullbackNearTrend,
                aboveReboundLine, pullbackStillRespectingTrend, reboundConfirmed, noRecentSharpSelloff);
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

    private boolean isHardRegimeBreak(MarketSnapshot snapshot, SymbolConfig symbolConfig) {
        boolean strongDowntrend = isStrongDowntrend(snapshot);
        boolean emaStackBrokenHard = snapshot.emaMid.compareTo(snapshot.emaLong) < 0;
        boolean closeUnderLongWithBuffer = snapshot.closePrice.compareTo(snapshot.emaLong.multiply(new BigDecimal("0.997"))) < 0;
        return strongDowntrend || emaStackBrokenHard || closeUnderLongWithBuffer;
    }

    private boolean isSoftRegimeBreak(MarketSnapshot snapshot, SymbolConfig symbolConfig) {
        boolean lostBullRegime = !isBullRegime(snapshot, symbolConfig);
        boolean closeUnderMid = snapshot.closePrice.compareTo(snapshot.emaMid) < 0;
        boolean stillAboveLong = snapshot.closePrice.compareTo(snapshot.emaLong) >= 0;
        return lostBullRegime && closeUnderMid && stillAboveLong && !isHardRegimeBreak(snapshot, symbolConfig);
    }

    private boolean isSoftRecoveryDcaCandidate(BarSeries barSeries, OrderResponseStorage storage,
                                               BigDecimal averageEntryPrice, MarketSnapshot snapshot) {
        int endIndex = barSeries.getEndIndex();
        if (endIndex < 1) {
            return false;
        }
        BigDecimal currentClose = toBigDecimal(barSeries.getBar(endIndex).getClosePrice());
        BigDecimal currentOpen = toBigDecimal(barSeries.getBar(endIndex).getOpenPrice());
        BigDecimal currentHigh = toBigDecimal(barSeries.getBar(endIndex).getHighPrice());
        BigDecimal previousClose = toBigDecimal(barSeries.getBar(endIndex - 1).getClosePrice());
        BigDecimal previousHigh = toBigDecimal(barSeries.getBar(endIndex - 1).getHighPrice());

        BigDecimal drawdownRatio = averageEntryPrice.subtract(currentClose)
                .divide(averageEntryPrice, 8, RoundingMode.HALF_UP);
        boolean drawdownStillManageable = drawdownRatio.compareTo(DEFAULT_SOFT_BREAK_RESCUE_MAX_DRAWDOWN) <= 0;
        boolean currentBarGreen = currentClose.compareTo(currentOpen) > 0;
        boolean closeIsRecovering = currentClose.compareTo(previousClose) > 0;
        boolean reclaimFast = currentClose.compareTo(snapshot.emaFast) >= 0;
        boolean takeOutPreviousHigh = currentHigh.compareTo(previousHigh) > 0;
        boolean noPriorRescueInSoftBreak = storage.getSoftBreakStartIndex() == null
                || storage.getLastEntryIndex() == null
                || storage.getLastEntryIndex() < storage.getSoftBreakStartIndex();

        return drawdownStillManageable
                && currentBarGreen
                && closeIsRecovering
                && reclaimFast
                && takeOutPreviousHigh
                && noPriorRescueInSoftBreak;
    }

    private boolean isBullRegime(MarketSnapshot snapshot, SymbolConfig symbolConfig) {
        boolean priceAboveLong = snapshot.closePrice.compareTo(snapshot.emaLong) >= 0;
        BigDecimal fastAboveMid = snapshot.emaMid.multiply(BigDecimal.ONE.add(getFastAboveMidBuffer(symbolConfig)));
        BigDecimal midAboveLong = snapshot.emaLong.multiply(BigDecimal.ONE.add(getMidAboveLongBuffer(symbolConfig)));
        boolean stackedEma = snapshot.emaFast.compareTo(fastAboveMid) >= 0
                && snapshot.emaMid.compareTo(midAboveLong) >= 0;
        boolean fastSlopeUp = snapshot.emaFast.compareTo(snapshot.emaFastLookback) >= 0;
        boolean midSlopeUp = snapshot.emaMid.compareTo(snapshot.emaMidLookback) >= 0;
        boolean longSlopeFlatOrUp = snapshot.emaLong.compareTo(snapshot.emaLongLookback) >= 0;
        return priceAboveLong && stackedEma && fastSlopeUp && midSlopeUp && longSlopeFlatOrUp;
    }

    private boolean hasTrendSeparation(MarketSnapshot snapshot) {
        BigDecimal fastAboveMid = snapshot.emaMid.multiply(BigDecimal.ONE.add(DEFAULT_FAST_ABOVE_MID_BUFFER));
        BigDecimal midAboveLong = snapshot.emaLong.multiply(BigDecimal.ONE.add(DEFAULT_MID_ABOVE_LONG_BUFFER));
        return snapshot.emaFast.compareTo(fastAboveMid) >= 0
                && snapshot.emaMid.compareTo(midAboveLong) >= 0;
    }

    private boolean isCloseNearFastEma(BigDecimal closePrice, BigDecimal emaFast) {
        BigDecimal lowerBound = emaFast.multiply(BigDecimal.ONE.subtract(DEFAULT_PULLBACK_LOWER_BUFFER));
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
        return greenBars >= 3;
    }

    private boolean recentPullbackHeldTrend(BarSeries barSeries, MarketSnapshot snapshot) {
        int endIndex = barSeries.getEndIndex();
        int startIndex = Math.max(0, endIndex - ENTRY_CONFIRMATION_LOOKBACK);
        BigDecimal minLow = null;
        for (int index = startIndex; index <= endIndex; index++) {
            BigDecimal low = toBigDecimal(barSeries.getBar(index).getLowPrice());
            if (minLow == null || low.compareTo(minLow) < 0) {
                minLow = low;
            }
        }

        if (minLow == null) {
            return false;
        }

        boolean heldMidSupport = minLow.compareTo(snapshot.emaMid.multiply(BigDecimal.ONE.subtract(DEFAULT_MID_SUPPORT_BUFFER))) >= 0;
        boolean heldLongSupport = minLow.compareTo(snapshot.emaLong.multiply(BigDecimal.ONE.subtract(DEFAULT_LONG_SUPPORT_BUFFER))) >= 0;
        return heldMidSupport && heldLongSupport;
    }

    private boolean hasBullishReboundConfirmation(BarSeries barSeries, MarketSnapshot snapshot) {
        int endIndex = barSeries.getEndIndex();
        if (endIndex < 2) {
            return false;
        }

        BigDecimal currentClose = toBigDecimal(barSeries.getBar(endIndex).getClosePrice());
        BigDecimal previousClose = toBigDecimal(barSeries.getBar(endIndex - 1).getClosePrice());
        BigDecimal previousOpen = toBigDecimal(barSeries.getBar(endIndex - 1).getOpenPrice());
        BigDecimal previousLow = toBigDecimal(barSeries.getBar(endIndex - 1).getLowPrice());

        BigDecimal currentOpen = toBigDecimal(barSeries.getBar(endIndex).getOpenPrice());
        BigDecimal currentHigh = toBigDecimal(barSeries.getBar(endIndex).getHighPrice());
        BigDecimal previousHigh = toBigDecimal(barSeries.getBar(endIndex - 1).getHighPrice());
        boolean currentBarGreen = currentClose.compareTo(currentOpen) >= 0;
        boolean previousBarFoundDemand = previousClose.compareTo(previousOpen) >= 0
                || previousLow.compareTo(snapshot.emaFast) <= 0;
        boolean currentCloseRecovered = currentClose.compareTo(previousClose) > 0
                && currentClose.compareTo(snapshot.emaFast) >= 0;
        boolean currentLowHeldFast = toBigDecimal(barSeries.getBar(endIndex).getLowPrice())
                .compareTo(snapshot.emaFast.multiply(BigDecimal.ONE.subtract(DEFAULT_PULLBACK_BUFFER))) >= 0;
        boolean reclaimedMidTrend = currentClose.compareTo(snapshot.emaMid) >= 0;
        boolean brokePreviousHigh = currentHigh.compareTo(previousHigh.multiply(BigDecimal.ONE.add(DEFAULT_ENTRY_BREAKOUT_BUFFER))) >= 0;
        boolean reboundHasFollowThrough = reclaimedMidTrend || brokePreviousHigh || previousBarFoundDemand;

        return currentBarGreen
                && currentCloseRecovered
                && currentLowHeldFast
                && reboundHasFollowThrough;
    }

    private boolean hasRecentSharpSelloff(BarSeries barSeries, SymbolConfig symbolConfig) {
        int endIndex = barSeries.getEndIndex();
        int startIndex = Math.max(0, endIndex - ENTRY_CONFIRMATION_LOOKBACK);
        BigDecimal recentHigh = null;
        BigDecimal recentLow = null;
        for (int index = startIndex; index <= endIndex; index++) {
            BigDecimal high = toBigDecimal(barSeries.getBar(index).getHighPrice());
            BigDecimal low = toBigDecimal(barSeries.getBar(index).getLowPrice());
            if (recentHigh == null || high.compareTo(recentHigh) > 0) {
                recentHigh = high;
            }
            if (recentLow == null || low.compareTo(recentLow) < 0) {
                recentLow = low;
            }
        }

        if (recentHigh == null || recentLow == null || recentHigh.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal drawdownRatio = recentHigh.subtract(recentLow)
                .divide(recentHigh, 8, RoundingMode.HALF_UP);
        return drawdownRatio.compareTo(getRecentDrawdownLimit(symbolConfig)) > 0;
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

    private BigDecimal getFastAboveMidBuffer(SymbolConfig symbolConfig) {
        return symbolConfig.getEntryFastAboveMidBuffer() == null
                ? DEFAULT_FAST_ABOVE_MID_BUFFER
                : symbolConfig.getEntryFastAboveMidBuffer();
    }

    private BigDecimal getMidAboveLongBuffer(SymbolConfig symbolConfig) {
        return symbolConfig.getEntryMidAboveLongBuffer() == null
                ? DEFAULT_MID_ABOVE_LONG_BUFFER
                : symbolConfig.getEntryMidAboveLongBuffer();
    }

    private BigDecimal getRecentDrawdownLimit(SymbolConfig symbolConfig) {
        return symbolConfig.getEntryRecentDrawdownLimit() == null
                ? DEFAULT_RECENT_DRAWDOWN_LIMIT
                : symbolConfig.getEntryRecentDrawdownLimit();
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

    private record EntryDecision(
            boolean enoughBars,
            boolean shouldEnter,
            boolean strongBullRegime,
            boolean notTooExtended,
            boolean pullbackNearTrend,
            boolean aboveReboundLine,
            boolean pullbackStillRespectingTrend,
            boolean reboundConfirmed,
            boolean noRecentSharpSelloff
    ) {
    }
}
