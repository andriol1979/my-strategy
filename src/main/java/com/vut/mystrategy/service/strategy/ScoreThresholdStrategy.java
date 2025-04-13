package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.rule.*;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.NaN;

import java.util.ArrayList;
import java.util.List;

public class ScoreThresholdStrategy extends MyStrategyBase {
    private final double entryScoreThreshold = 5.0;
    private final double exitScoreThreshold = 3.5;

    @Override
    public Strategy buildLongStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        List<LoggingRule> entryRules = buildLongEntryRules(barSeries, symbolConfig);
        List<LoggingRule> exitRules = buildLongExitRules(barSeries, symbolConfig);

        return new CustomBaseStrategy(entryRules, entryScoreThreshold, exitRules, exitScoreThreshold);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        List<LoggingRule> entryRules = buildShortEntryRules(barSeries, symbolConfig);
        List<LoggingRule> exitRules = buildShortExitRules(barSeries, symbolConfig);

        return new CustomBaseStrategy(entryRules, entryScoreThreshold, exitRules, exitScoreThreshold);
    }

    //-------------Build rules---------------------------------------------------

    private List<LoggingRule> buildLongEntryRules(BarSeries barSeries, SymbolConfig symbolConfig) {
        List<LoggingRule> entryRules = new ArrayList<>();
        //EMAUptrendRule
        LoggingRule emaUpTrendRule = (LoggingRule) EMAUpTrendRule.buildRule(barSeries, symbolConfig);
        //EMACrossOverRule
        LoggingRule emaCrossUpRule = (LoggingRule) EMACrossUpRule.buildRule(barSeries, symbolConfig);
        //BullishEngulfingRule
        LoggingRule bullishEngulfingRule = (LoggingRule) BullishEngulfingRule.buildRule(barSeries);
        //VolumeSlopeRule
        LoggingRule volumeSlopeRule = (LoggingRule) VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(10.0), NaN.NaN);
        //BuyOverSellVolumeRule
        LoggingRule buyOverSellVolumeRule = (LoggingRule) BuyOverSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(
                Calculator.calculateBuySellVolumePercentageInEntryCase(symbolConfig.getBuyOverSellVolumePercentage())));
        //HammerRule
        LoggingRule hammerRule = (LoggingRule) HammerRule.buildRule(barSeries);
        //OverSoldRule
        LoggingRule overSoldRule = (LoggingRule) OverSoldRule.buildRule(barSeries);
        //PriceNearResistanceRule
        LoggingRule priceNearResistanceRule = (LoggingRule) PriceNearResistanceRule.buildRule(barSeries, symbolConfig.getResistanceThreshold());
        //PriceNearSupportRule
        LoggingRule priceNearSupportRule = (LoggingRule) PriceNearSupportRule.buildRule(barSeries, symbolConfig.getSupportThreshold());

        /*
        Nhóm 1 (Trend-Following): EMAUptrendRule + EMACrossOverRule + VolumeSlopeRule + BuyOverSellVolumeRule + BullishEngulfingRule → Mua theo xu hướng.
        Nhóm 2 (Reversal): PriceNearSupportRule + OversoldRule + BullishEngulfingRule/HammerRule + VolumeSlopeRule → Mua tại hỗ trợ.
        Nhóm 3 (Breakout): PriceNearResistanceRule + EMACrossOverRule + VolumeSlopeRule + BuyOverSellVolumeRule + BullishEngulfingRule → Mua khi breakout.
         */
        LoggingRule volumeMomentum = (LoggingRule) volumeSlopeRule.and(buyOverSellVolumeRule);
        //Nhóm 1 (Trend-Following)
        LoggingRule trendFollowing1 = (LoggingRule) emaUpTrendRule.and(volumeMomentum).and(bullishEngulfingRule.xor(hammerRule));
        LoggingRule trendFollowing2 = (LoggingRule) emaCrossUpRule.and(volumeMomentum).or(bullishEngulfingRule.xor(hammerRule));
        //Nhóm 2 (Reversal)
        LoggingRule reversal = (LoggingRule) overSoldRule.and(volumeMomentum).and(priceNearSupportRule).and(bullishEngulfingRule.xor(hammerRule));
        //Nhóm 3 (Breakout)
        LoggingRule breakout = (LoggingRule) emaCrossUpRule.and(volumeMomentum).and(priceNearResistanceRule).and(bullishEngulfingRule);

        //HammerRule
        entryRules.add((LoggingRule) HammerRule.buildRule(barSeries));
        //Oversold
        entryRules.add((LoggingRule) OverSoldRule.buildRule(barSeries));

        return entryRules;
    }

    private List<LoggingRule> buildLongExitRules(BarSeries barSeries, SymbolConfig symbolConfig) {
        List<LoggingRule> exitRules = new ArrayList<>();
        //EMADownTrendRule
        exitRules.add((LoggingRule) EMADownTrendRule.buildRule(barSeries, symbolConfig));
        //EMACrossDownRule
        exitRules.add((LoggingRule) EMACrossDownRule.buildRule(barSeries, symbolConfig));
        //BearishEngulfingRule
        exitRules.add((LoggingRule) BearishEngulfingRule.buildRule(barSeries));
        //VolumeSlopeRule giảm
        exitRules.add((LoggingRule) VolumeSlopeRule.buildRule(barSeries, NaN.NaN, DecimalNum.valueOf(-5.0)));
        //BuyUnderSellVolumeRule
        exitRules.add((LoggingRule) BuyUnderSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(
                Calculator.calculateBuySellVolumePercentageInEntryCase(symbolConfig.getBuyUnderSellVolumePercentage()))));
        //HangingManRule
        exitRules.add((LoggingRule) HangingManRule.buildRule(barSeries));
        //InvertedHammerRule
        exitRules.add((LoggingRule) InvertedHammerRule.buildRule(barSeries));
        //Over bought
        exitRules.add((LoggingRule) OverBoughtRule.buildRule(barSeries));
        //Stoploss rule
        exitRules.add((LoggingRule) StopLossLongRule.buildRule(barSeries));

        return exitRules;
    }

    //---------------------------------------------------------------------------------

    private List<LoggingRule> buildShortEntryRules(BarSeries barSeries, SymbolConfig symbolConfig) {
        List<LoggingRule> entryRules = new ArrayList<>();
        //EMADownTrendRule
        entryRules.add((LoggingRule) EMADownTrendRule.buildRule(barSeries, symbolConfig));
        //EMACrossDownRule
        entryRules.add((LoggingRule) EMACrossDownRule.buildRule(barSeries, symbolConfig));
        //BearishEngulfingRule
        entryRules.add((LoggingRule) BearishEngulfingRule.buildRule(barSeries));
        //VolumeSlopeRule
        entryRules.add((LoggingRule) VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(10.0), NaN.NaN));
        //BuyUnderSellVolumeRule
        entryRules.add((LoggingRule) BuyUnderSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(
                Calculator.calculateBuySellVolumePercentageInEntryCase(symbolConfig.getBuyUnderSellVolumePercentage()))));
        //HangingManRule
        entryRules.add((LoggingRule) HangingManRule.buildRule(barSeries));
        //InvertedHammerRule
        entryRules.add((LoggingRule) InvertedHammerRule.buildRule(barSeries));
        //Over bought
        entryRules.add((LoggingRule) OverBoughtRule.buildRule(barSeries));

        return entryRules;
    }

    private List<LoggingRule> buildShortExitRules(BarSeries barSeries, SymbolConfig symbolConfig) {
        List<LoggingRule> exitRules = new ArrayList<>();
        //EMAUptrendRule
        exitRules.add((LoggingRule) EMAUpTrendRule.buildRule(barSeries, symbolConfig));
        //EMACrossOverRule
        exitRules.add((LoggingRule) EMACrossUpRule.buildRule(barSeries, symbolConfig));
        //BullishEngulfingRule
        exitRules.add((LoggingRule) BullishEngulfingRule.buildRule(barSeries));
        //VolumeSlopeRule giảm
        exitRules.add((LoggingRule) VolumeSlopeRule.buildRule(barSeries, NaN.NaN, DecimalNum.valueOf(-5.0)));
        //BuyOverSellVolumeRule
        exitRules.add((LoggingRule) BuyOverSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(
                Calculator.calculateBuySellVolumePercentageInEntryCase(symbolConfig.getBuyOverSellVolumePercentage()))));
        //HammerRule
        exitRules.add((LoggingRule) HammerRule.buildRule(barSeries));
        //Stoploss rule
        exitRules.add((LoggingRule) StopLossShortRule.buildRule(barSeries));
        //Oversold
        exitRules.add((LoggingRule) OverSoldRule.buildRule(barSeries));

        return exitRules;
    }
}
