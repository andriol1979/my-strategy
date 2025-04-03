package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.service.strategy.indicator.SchaffTrendCycleIndicator;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.bollinger.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.indicators.trend.DownTrendIndicator;
import org.ta4j.core.indicators.trend.UpTrendIndicator;
import org.ta4j.core.num.Num;

public class TrendDetector {

    private final Indicator<Num> closePrice;
    private final BollingerBandsUpperIndicator upperBand;
    private final BollingerBandsLowerIndicator lowerBand;
    private final SchaffTrendCycleIndicator stc;
    private final EMAIndicator ema200;
    private final UpTrendIndicator upTrend;
    private final DownTrendIndicator downTrend;

    public TrendDetector(BarSeries series) {
        upTrend = new UpTrendIndicator(series, 21);
        downTrend = new DownTrendIndicator(series, 21);

        this.closePrice = new ClosePriceIndicator(series);
        EMAIndicator avg14 = new EMAIndicator(closePrice, 20);
        BollingerBandsMiddleIndicator middleBand = new BollingerBandsMiddleIndicator(avg14);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, 20);
        this.upperBand = new BollingerBandsUpperIndicator(middleBand, stdDev);
        this.lowerBand = new BollingerBandsLowerIndicator(middleBand, stdDev);

        this.stc = new SchaffTrendCycleIndicator(series);
        this.ema200 = new EMAIndicator(closePrice, 200);
    }

    public String detectTrend(int index, Num tolerance) {
        Num price = closePrice.getValue(index);
        Num upper = upperBand.getValue(index);
        Num lower = lowerBand.getValue(index);

        boolean isUptrend = price.isGreaterThan(upper)
                && stc.getValue(index).isGreaterThan(closePrice.numOf(75))
                && ema200.getValue(index).isGreaterThan(closePrice.getValue(index))
                && upTrend.getValue(index);

        boolean isDowntrend = price.isLessThan(lower)
                && stc.getValue(index).isLessThan(closePrice.numOf(25))
                && ema200.getValue(index).isLessThan(closePrice.getValue(index))
                && downTrend.getValue(index);

        boolean isNotUpTrendAndDownTrend = !upTrend.getValue(index) && !downTrend.getValue(index);
        boolean stcIsSideways = stc.getValue(index).isGreaterThanOrEqual(closePrice.numOf(30)) &&
                stc.getValue(index).isLessThanOrEqual(closePrice.numOf(70));
        boolean isSideways = isNotUpTrendAndDownTrend && stcIsSideways;


        if (isUptrend) return "📈 Uptrend";
        if (isDowntrend) return "📉 Downtrend";
        if (isSideways) return "➖ Sideways";

        return "❓ Undefined Trend";
    }
}

