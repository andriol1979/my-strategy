package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.rule.*;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.NaN;

@Slf4j
public class MyCustomStrategy extends MyStrategyBase {

    @Override
    public Strategy buildLongStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        Rule entryRule = buildLongEntryRule(barSeries, symbolConfig);
        Rule exitRule = buildLongExitRule(barSeries, symbolConfig);

        return new BaseStrategy(entryRule, exitRule);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        Rule entryRule = buildShortEntryRule(barSeries, symbolConfig);
        Rule exitRule = buildShortExitRule(barSeries, symbolConfig);

        return new BaseStrategy(entryRule, exitRule);
    }

    //---------------------Build LONG entry rule------------------------
    private Rule buildLongEntryRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        //EMAUptrendRule
        Rule emaUpTrendRule = EMAUpTrendRule.buildRule(barSeries, symbolConfig);
        //EMACrossOverRule
        Rule emaCrossUpRule = EMACrossUpRule.buildRule(barSeries, symbolConfig);
        //BullishEngulfingRule
        Rule bullishEngulfingRule = BullishEngulfingRule.buildRule(barSeries);
        //VolumeSlopeRule tăng
        Rule volumeSlopeUpRule = VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(10.0), NaN.NaN);
        //BuyOverSellVolumeRule
        Rule buyOverSellVolumeRule = BuyOverSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(
                Calculator.calculateBuySellVolumePercentageInEntryCase(symbolConfig.getBuyOverSellVolumePercentage())));
        //HammerRule
        Rule hammerRule = HammerRule.buildRule(barSeries);
        //OverSoldRule
        Rule overSoldRule = OverSoldRule.buildRule(barSeries);
        //PriceNearResistanceRule
        Rule priceNearResistanceRule = PriceNearResistanceRule.buildRule(barSeries, symbolConfig.getResistanceThreshold());
        //PriceNearSupportRule
        Rule priceNearSupportRule = PriceNearSupportRule.buildRule(barSeries, symbolConfig.getSupportThreshold());

        /*
        Nhóm 1 (Trend-Following): EMAUptrendRule + EMACrossOverRule + VolumeSlopeRule + BuyOverSellVolumeRule + BullishEngulfingRule → Mua theo xu hướng.
        Nhóm 2 (Reversal): PriceNearSupportRule + OversoldRule + BullishEngulfingRule/HammerRule + VolumeSlopeRule → Mua tại hỗ trợ.
        Nhóm 3 (Breakout): PriceNearResistanceRule + EMACrossOverRule + VolumeSlopeRule + BuyOverSellVolumeRule + BullishEngulfingRule → Mua khi breakout.
         */
        Rule volumeMomentum = volumeSlopeUpRule.and(buyOverSellVolumeRule);
        Rule candleStickRule = bullishEngulfingRule.or(hammerRule);
        //Nhóm 1 (Trend-Following)
        Rule trendFollowing1 = emaUpTrendRule.and(volumeMomentum).and(candleStickRule);
        Rule trendFollowing2 = emaCrossUpRule.and(volumeMomentum).and(candleStickRule);
        //Nhóm 2 (Reversal)
        Rule reversal = overSoldRule.and(volumeMomentum).and(priceNearSupportRule).and(candleStickRule);
        //Nhóm 3 (Breakout)
        Rule breakout = emaCrossUpRule.and(volumeMomentum).and(priceNearResistanceRule).and(candleStickRule);

        return trendFollowing1.or(trendFollowing2).or(reversal).or(breakout);
    }

    //---------------------Build LONG exit rule------------------------
    private Rule buildLongExitRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        //EMADownTrendRule
        Rule emaDownTrendRule = EMADownTrendRule.buildRule(barSeries, symbolConfig);
        //EMACrossDownRule
        Rule emaCrossDownRule = EMACrossDownRule.buildRule(barSeries, symbolConfig);
        //BearishEngulfingRule
        Rule bearishEngulfingRule = BearishEngulfingRule.buildRule(barSeries);
        //VolumeSlopeRule giảm
        Rule volumeSlopeDownRule = VolumeSlopeRule.buildRule(barSeries, NaN.NaN, DecimalNum.valueOf(-10.0));
        //BuyUnderSellVolumeRule
        Rule buyUnderSellVolumeRule = BuyUnderSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(
                Calculator.calculateBuySellVolumePercentageInEntryCase(symbolConfig.getBuyUnderSellVolumePercentage())));
        //HangingManRule
        Rule hangingManRule = HangingManRule.buildRule(barSeries);
        //InvertedHammerRule
        Rule invertedHammerRule = InvertedHammerRule.buildRule(barSeries);
        //OverBoughtRule
        Rule overBoughtRule = OverBoughtRule.buildRule(barSeries);
        //PriceNearResistanceRule
        Rule priceNearResistanceRule = PriceNearResistanceRule.buildRule(barSeries, symbolConfig.getResistanceThreshold());
        //PriceNearSupportRule
        Rule priceNearSupportRule = PriceNearSupportRule.buildRule(barSeries, symbolConfig.getSupportThreshold());

        /*
        Nhóm 1 (Trend Weakening): EMADowntrendRule + VolumeSlopeDownRule + BuyUnderSellVolumeRule + BearishEngulfingRule → thoát
        Nhóm 2 (Reversal): PriceNearSupportRule + OversoldRule + BullishEngulfingRule/HammerRule + VolumeSlopeRule → Mua tại hỗ trợ.
        Nhóm 3 (Breakout): PriceNearResistanceRule + EMACrossOverRule + VolumeSlopeRule + BuyOverSellVolumeRule + BullishEngulfingRule → Mua khi breakout.
         */
        Rule volumeMomentum = volumeSlopeDownRule.and(buyUnderSellVolumeRule);
        Rule candleStickRule = bearishEngulfingRule.or(hangingManRule).or(invertedHammerRule);
        //Nhóm 1 (Trend-Following)
        Rule trendWeakening1 = emaDownTrendRule.and(volumeMomentum).and(candleStickRule);
        Rule trendWeakening2 = emaCrossDownRule.and(volumeMomentum).and(candleStickRule);
        //Nhóm 2 (Bearish Signal / Overbought)
        Rule reversalRejection = overBoughtRule.and(volumeMomentum).and(priceNearResistanceRule).and(candleStickRule);
        //Nhóm 3 (Breakout failed/Trend Failure)
        Rule breakoutFailed = emaCrossDownRule.and(volumeMomentum).and(priceNearSupportRule).and(candleStickRule);

        return trendWeakening1.or(trendWeakening2).or(reversalRejection).or(breakoutFailed);
    }

    //----------------------------------------------------------------------------------------------------------------------------

    //---------------------Build SHORT entry rule------------------------
    private Rule buildShortEntryRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        //EMADownTrendRule
        Rule emaDownTrendRule = EMADownTrendRule.buildRule(barSeries, symbolConfig);
        //EMACrossDownRule
        Rule emaCrossDownRule = EMACrossDownRule.buildRule(barSeries, symbolConfig);
        //BearishEngulfingRule
        Rule bearishEngulfingRule = BearishEngulfingRule.buildRule(barSeries);
        //VolumeSlopeRule tăng
        Rule volumeSlopeFlatRule = VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(5.0), NaN.NaN);
        //BuyUnderSellVolumeRule
        Rule buyUnderSellVolumeRule = BuyUnderSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(
                Calculator.calculateBuySellVolumePercentageInEntryCase(symbolConfig.getBuyUnderSellVolumePercentage())));
        //HangingManRule
        Rule hangingManRule = HangingManRule.buildRule(barSeries);
        //InvertedHammerRule
        Rule invertedHammerRule = InvertedHammerRule.buildRule(barSeries);
        //OverBoughtRule
        Rule overBoughtRule = OverBoughtRule.buildRule(barSeries);
        //PriceNearResistanceRule
        Rule priceNearResistanceRule = PriceNearResistanceRule.buildRule(barSeries, symbolConfig.getResistanceThreshold());
        //PriceNearSupportRule
        Rule priceNearSupportRule = PriceNearSupportRule.buildRule(barSeries, symbolConfig.getSupportThreshold());

        /*
        Nhóm 1 (Trend-Following): EMADowntrendRule AND EMACrossUnderRule AND VolumeSlopeRule AND SellOverBuyVolumeRule OR BearishEngulfingRule → SHORT theo xu hướng giảm.
        Nhóm 2 (Reversal): PriceNearResistanceRule AND OverboughtRule AND (BearishEngulfingRule OR ShootingStarRule) OR SellOverBuyVolumeRule → SHORT tại kháng cự.
        Nhóm 3 (Breakdown): PriceNearSupportRule (phá vỡ) AND EMACrossUnderRule AND VolumeSlopeRule AND SellOverBuyVolumeRule OR BearishEngulfingRule → SHORT khi phá hỗ trợ.
         */
        Rule volumeMomentum = volumeSlopeFlatRule.and(buyUnderSellVolumeRule);
        Rule candleStickRule = bearishEngulfingRule.or(hangingManRule).or(invertedHammerRule);
        //Nhóm 1 (Bearish Trend-Following)
        Rule trendFollowing1 = emaDownTrendRule.and(volumeMomentum).and(candleStickRule);
        Rule trendFollowing2 = emaCrossDownRule.and(volumeMomentum).and(candleStickRule);
        //Nhóm 2 (Reversal)
        Rule reversal = overBoughtRule.and(volumeMomentum).and(priceNearResistanceRule).and(candleStickRule);
        //Nhóm 3 (Breakout)
        Rule breakout = emaCrossDownRule.and(volumeMomentum).and(priceNearSupportRule).and(candleStickRule);

        return trendFollowing1.or(trendFollowing2).or(reversal).or(breakout);
    }

    //---------------------Build SHORT exit rule------------------------
    private Rule buildShortExitRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        //EMAUptrendRule
        Rule emaUpTrendRule = EMAUpTrendRule.buildRule(barSeries, symbolConfig);
        //EMACrossOverRule
        Rule emaCrossUpRule = EMACrossUpRule.buildRule(barSeries, symbolConfig);
        //BullishEngulfingRule
        Rule bullishEngulfingRule = BullishEngulfingRule.buildRule(barSeries);
        //VolumeSlopeRule tăng
        Rule volumeSlopeUpRule = VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(10.0), NaN.NaN);
        //BuyOverSellVolumeRule
        Rule buyOverSellVolumeRule = BuyOverSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(
                Calculator.calculateBuySellVolumePercentageInEntryCase(symbolConfig.getBuyOverSellVolumePercentage())));
        //HammerRule
        Rule hammerRule = HammerRule.buildRule(barSeries);
        //OverSoldRule
        Rule overSoldRule = OverSoldRule.buildRule(barSeries);
        //PriceNearResistanceRule
        Rule priceNearResistanceRule = PriceNearResistanceRule.buildRule(barSeries, symbolConfig.getResistanceThreshold());
        //PriceNearSupportRule
        Rule priceNearSupportRule = PriceNearSupportRule.buildRule(barSeries, symbolConfig.getSupportThreshold());

        /*
        Nhóm 1 (Trend Reversal): EMAUptrendRule AND EMACrossOverRule AND VolumeSlopeRule AND BuyOverSellVolumeRule OR BullishEngulfingRule → Thoát khi xu hướng đảo chiều tăng.
        Nhóm 2 (Reversal): PriceNearSupportRule AND OversoldRule AND (BullishEngulfingRule OR HammerRule) OR BuyOverSellVolumeRule → Thoát tại hỗ trợ.
        Nhóm 3 (Breakout): PriceNearResistanceRule (phá vỡ) AND EMACrossOverRule AND VolumeSlopeRule AND BuyOverSellVolumeRule OR BullishEngulfingRule → Thoát khi phá kháng cự.
         */
        Rule volumeMomentum = volumeSlopeUpRule.and(buyOverSellVolumeRule);
        Rule candleStickRule = bullishEngulfingRule.or(hammerRule);
        //Nhóm 1 (Trend-Following)
        Rule trendWeakening1 = emaUpTrendRule.and(volumeMomentum).and(candleStickRule);
        Rule trendWeakening2 = emaCrossUpRule.and(volumeMomentum).and(candleStickRule);
        //Nhóm 2 (Oversold + Support)
        Rule reversalRejection = overSoldRule.and(volumeMomentum).and(priceNearSupportRule).and(candleStickRule);
        //Nhóm 3 (Breakout failed/Trend Failure)
        Rule breakoutFailed = emaCrossUpRule.and(volumeMomentum).and(priceNearResistanceRule).and(candleStickRule);

        return trendWeakening1.or(trendWeakening2).or(reversalRejection).or(breakoutFailed);
    }
}
