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
        LoggingRule entryRule = buildLongEntryRule(barSeries, symbolConfig);
        LoggingRule exitRule = buildLongExitRule(barSeries, symbolConfig);

        return new BaseStrategy(entryRule, exitRule);
    }

    @Override
    public Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig) {
        if (barSeries == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        LoggingRule entryRule = buildShortEntryRule(barSeries, symbolConfig);
        LoggingRule exitRule = buildShortExitRule(barSeries, symbolConfig);

        return new BaseStrategy(entryRule, exitRule);
    }

    //---------------------Build LONG entry rule------------------------
    private LoggingRule buildLongEntryRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        //EMAUptrendRule
        LoggingRule emaUpTrendRule = (LoggingRule) EMAUpTrendRule.buildRule(barSeries, symbolConfig);
        //EMACrossOverRule
        LoggingRule emaCrossUpRule = (LoggingRule) EMACrossUpRule.buildRule(barSeries, symbolConfig);
        //BullishEngulfingRule
        LoggingRule bullishEngulfingRule = (LoggingRule) BullishEngulfingRule.buildRule(barSeries);
        //VolumeSlopeRule tăng
        LoggingRule volumeSlopeUpRule = (LoggingRule) VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(10.0), NaN.NaN);
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
        LoggingRule volumeMomentum = (LoggingRule) volumeSlopeUpRule.and(buyOverSellVolumeRule);
        LoggingRule candleStickRule = (LoggingRule) bullishEngulfingRule.or(hammerRule);
        //Nhóm 1 (Trend-Following)
        LoggingRule trendFollowing1 = (LoggingRule) emaUpTrendRule.and(volumeMomentum).and(candleStickRule);
        LoggingRule trendFollowing2 = (LoggingRule) emaCrossUpRule.and(volumeMomentum).and(candleStickRule);
        //Nhóm 2 (Reversal)
        LoggingRule reversal = (LoggingRule) overSoldRule.and(volumeMomentum).and(priceNearSupportRule).and(candleStickRule);
        //Nhóm 3 (Breakout)
        LoggingRule breakout = (LoggingRule) emaCrossUpRule.and(volumeMomentum).and(priceNearResistanceRule).and(candleStickRule);

        return (LoggingRule) trendFollowing1.or(trendFollowing2).or(reversal).or(breakout);
    }

    //---------------------Build LONG exit rule------------------------
    private LoggingRule buildLongExitRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        //EMADownTrendRule
        LoggingRule emaDownTrendRule = (LoggingRule) EMADownTrendRule.buildRule(barSeries, symbolConfig);
        //EMACrossDownRule
        LoggingRule emaCrossDownRule = (LoggingRule) EMACrossDownRule.buildRule(barSeries, symbolConfig);
        //BearishEngulfingRule
        LoggingRule bearishEngulfingRule = (LoggingRule) BearishEngulfingRule.buildRule(barSeries);
        //VolumeSlopeRule giảm
        LoggingRule volumeSlopeDownRule = (LoggingRule) VolumeSlopeRule.buildRule(barSeries, NaN.NaN, DecimalNum.valueOf(-10.0));
        //BuyUnderSellVolumeRule
        LoggingRule buyUnderSellVolumeRule = (LoggingRule) BuyUnderSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(
                Calculator.calculateBuySellVolumePercentageInEntryCase(symbolConfig.getBuyUnderSellVolumePercentage())));
        //HangingManRule
        LoggingRule hangingManRule = (LoggingRule) HangingManRule.buildRule(barSeries);
        //InvertedHammerRule
        LoggingRule invertedHammerRule = (LoggingRule) InvertedHammerRule.buildRule(barSeries);
        //OverBoughtRule
        LoggingRule overBoughtRule = (LoggingRule) OverBoughtRule.buildRule(barSeries);
        //PriceNearResistanceRule
        LoggingRule priceNearResistanceRule = (LoggingRule) PriceNearResistanceRule.buildRule(barSeries, symbolConfig.getResistanceThreshold());
        //PriceNearSupportRule
        LoggingRule priceNearSupportRule = (LoggingRule) PriceNearSupportRule.buildRule(barSeries, symbolConfig.getSupportThreshold());

        /*
        Nhóm 1 (Trend Weakening): EMADowntrendRule + VolumeSlopeDownRule + BuyUnderSellVolumeRule + BearishEngulfingRule → thoát
        Nhóm 2 (Reversal): PriceNearSupportRule + OversoldRule + BullishEngulfingRule/HammerRule + VolumeSlopeRule → Mua tại hỗ trợ.
        Nhóm 3 (Breakout): PriceNearResistanceRule + EMACrossOverRule + VolumeSlopeRule + BuyOverSellVolumeRule + BullishEngulfingRule → Mua khi breakout.
         */
        LoggingRule volumeMomentum = (LoggingRule) volumeSlopeDownRule.and(buyUnderSellVolumeRule);
        LoggingRule candleStickRule = (LoggingRule) bearishEngulfingRule.or(hangingManRule).or(invertedHammerRule);
        //Nhóm 1 (Trend-Following)
        LoggingRule trendWeakening1 = (LoggingRule) emaDownTrendRule.and(volumeMomentum).and(candleStickRule);
        LoggingRule trendWeakening2 = (LoggingRule) emaCrossDownRule.and(volumeMomentum).and(candleStickRule);
        //Nhóm 2 (Bearish Signal / Overbought)
        LoggingRule reversalRejection = (LoggingRule) overBoughtRule.and(volumeMomentum).and(priceNearResistanceRule).and(candleStickRule);
        //Nhóm 3 (Breakout failed/Trend Failure)
        LoggingRule breakoutFailed = (LoggingRule) emaCrossDownRule.and(volumeMomentum).and(priceNearSupportRule).and(candleStickRule);

        return (LoggingRule) trendWeakening1.or(trendWeakening2).or(reversalRejection).or(breakoutFailed);
    }

    //----------------------------------------------------------------------------------------------------------------------------

    //---------------------Build SHORT entry rule------------------------
    private LoggingRule buildShortEntryRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        //EMADownTrendRule
        LoggingRule emaDownTrendRule = (LoggingRule) EMADownTrendRule.buildRule(barSeries, symbolConfig);
        //EMACrossDownRule
        LoggingRule emaCrossDownRule = (LoggingRule) EMACrossDownRule.buildRule(barSeries, symbolConfig);
        //BearishEngulfingRule
        LoggingRule bearishEngulfingRule = (LoggingRule) BearishEngulfingRule.buildRule(barSeries);
        //VolumeSlopeRule tăng
        LoggingRule volumeSlopeFlatRule = (LoggingRule) VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(5.0), NaN.NaN);
        //BuyUnderSellVolumeRule
        LoggingRule buyUnderSellVolumeRule = (LoggingRule) BuyUnderSellVolumeRule.buildRule(barSeries, DecimalNum.valueOf(
                Calculator.calculateBuySellVolumePercentageInEntryCase(symbolConfig.getBuyUnderSellVolumePercentage())));
        //HangingManRule
        LoggingRule hangingManRule = (LoggingRule) HangingManRule.buildRule(barSeries);
        //InvertedHammerRule
        LoggingRule invertedHammerRule = (LoggingRule) InvertedHammerRule.buildRule(barSeries);
        //OverBoughtRule
        LoggingRule overBoughtRule = (LoggingRule) OverBoughtRule.buildRule(barSeries);
        //PriceNearResistanceRule
        LoggingRule priceNearResistanceRule = (LoggingRule) PriceNearResistanceRule.buildRule(barSeries, symbolConfig.getResistanceThreshold());
        //PriceNearSupportRule
        LoggingRule priceNearSupportRule = (LoggingRule) PriceNearSupportRule.buildRule(barSeries, symbolConfig.getSupportThreshold());

        /*
        Nhóm 1 (Trend-Following): EMADowntrendRule AND EMACrossUnderRule AND VolumeSlopeRule AND SellOverBuyVolumeRule OR BearishEngulfingRule → SHORT theo xu hướng giảm.
        Nhóm 2 (Reversal): PriceNearResistanceRule AND OverboughtRule AND (BearishEngulfingRule OR ShootingStarRule) OR SellOverBuyVolumeRule → SHORT tại kháng cự.
        Nhóm 3 (Breakdown): PriceNearSupportRule (phá vỡ) AND EMACrossUnderRule AND VolumeSlopeRule AND SellOverBuyVolumeRule OR BearishEngulfingRule → SHORT khi phá hỗ trợ.
         */
        LoggingRule volumeMomentum = (LoggingRule) volumeSlopeFlatRule.and(buyUnderSellVolumeRule);
        LoggingRule candleStickRule = (LoggingRule) bearishEngulfingRule.or(hangingManRule).or(invertedHammerRule);
        //Nhóm 1 (Bearish Trend-Following)
        LoggingRule trendFollowing1 = (LoggingRule) emaDownTrendRule.and(volumeMomentum).and(candleStickRule);
        LoggingRule trendFollowing2 = (LoggingRule) emaCrossDownRule.and(volumeMomentum).and(candleStickRule);
        //Nhóm 2 (Reversal)
        LoggingRule reversal = (LoggingRule) overBoughtRule.and(volumeMomentum).and(priceNearResistanceRule).and(candleStickRule);
        //Nhóm 3 (Breakout)
        LoggingRule breakout = (LoggingRule) emaCrossDownRule.and(volumeMomentum).and(priceNearSupportRule).and(candleStickRule);

        return (LoggingRule) trendFollowing1.or(trendFollowing2).or(reversal).or(breakout);
    }

    //---------------------Build SHORT exit rule------------------------
    private LoggingRule buildShortExitRule(BarSeries barSeries, SymbolConfig symbolConfig) {
        //EMAUptrendRule
        LoggingRule emaUpTrendRule = (LoggingRule) EMAUpTrendRule.buildRule(barSeries, symbolConfig);
        //EMACrossOverRule
        LoggingRule emaCrossUpRule = (LoggingRule) EMACrossUpRule.buildRule(barSeries, symbolConfig);
        //BullishEngulfingRule
        LoggingRule bullishEngulfingRule = (LoggingRule) BullishEngulfingRule.buildRule(barSeries);
        //VolumeSlopeRule tăng
        LoggingRule volumeSlopeUpRule = (LoggingRule) VolumeSlopeRule.buildRule(barSeries, DecimalNum.valueOf(10.0), NaN.NaN);
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
        Nhóm 1 (Trend Reversal): EMAUptrendRule AND EMACrossOverRule AND VolumeSlopeRule AND BuyOverSellVolumeRule OR BullishEngulfingRule → Thoát khi xu hướng đảo chiều tăng.
        Nhóm 2 (Reversal): PriceNearSupportRule AND OversoldRule AND (BullishEngulfingRule OR HammerRule) OR BuyOverSellVolumeRule → Thoát tại hỗ trợ.
        Nhóm 3 (Breakout): PriceNearResistanceRule (phá vỡ) AND EMACrossOverRule AND VolumeSlopeRule AND BuyOverSellVolumeRule OR BullishEngulfingRule → Thoát khi phá kháng cự.
         */
        LoggingRule volumeMomentum = (LoggingRule) volumeSlopeUpRule.and(buyOverSellVolumeRule);
        LoggingRule candleStickRule = (LoggingRule) bullishEngulfingRule.or(hammerRule);
        //Nhóm 1 (Trend-Following)
        LoggingRule trendWeakening1 = (LoggingRule) emaUpTrendRule.and(volumeMomentum).and(candleStickRule);
        LoggingRule trendWeakening2 = (LoggingRule) emaCrossUpRule.and(volumeMomentum).and(candleStickRule);
        //Nhóm 2 (Oversold + Support)
        LoggingRule reversalRejection = (LoggingRule) overSoldRule.and(volumeMomentum).and(priceNearSupportRule).and(candleStickRule);
        //Nhóm 3 (Breakout failed/Trend Failure)
        LoggingRule breakoutFailed = (LoggingRule) emaCrossUpRule.and(volumeMomentum).and(priceNearResistanceRule).and(candleStickRule);

        return (LoggingRule) trendWeakening1.or(trendWeakening2).or(reversalRejection).or(breakoutFailed);
    }
}
