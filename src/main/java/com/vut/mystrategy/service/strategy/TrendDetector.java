package com.vut.mystrategy.service.strategy;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.adx.MinusDIIndicator;
import org.ta4j.core.indicators.adx.PlusDIIndicator;
import org.ta4j.core.indicators.bollinger.*;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;

public class TrendDetector {

    private final Indicator<Num> closePrice;
    private final BollingerBandsUpperIndicator upperBand;
    private final BollingerBandsLowerIndicator lowerBand;
    private final MACDIndicator macd;
    private final EMAIndicator signalLine;
    private final StochasticOscillatorKIndicator stc;
    private final EMAIndicator ema50;
    private final EMAIndicator ema200;
    private final ADXIndicator adx;
    private final PlusDIIndicator plusDI;
    private final MinusDIIndicator minusDI;

    public TrendDetector(BarSeries series) {
        this.closePrice = new ClosePriceIndicator(series);
        EMAIndicator avg14 = new EMAIndicator(closePrice, 20);
        BollingerBandsMiddleIndicator middleBand = new BollingerBandsMiddleIndicator(avg14);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, 20);
        this.upperBand = new BollingerBandsUpperIndicator(middleBand, stdDev);
        this.lowerBand = new BollingerBandsLowerIndicator(middleBand, stdDev);

        this.macd = new MACDIndicator(closePrice, 12, 26);
        this.signalLine = new EMAIndicator(macd, 9);

        this.stc = new StochasticOscillatorKIndicator(series, 14);

        this.ema50 = new EMAIndicator(closePrice, 50);
        this.ema200 = new EMAIndicator(closePrice, 200);

        this.adx = new ADXIndicator(series, 14);
        this.plusDI = new PlusDIIndicator(series, 14);
        this.minusDI = new MinusDIIndicator(series, 14);
    }

    public String detectTrend(int index) {
        Num price = closePrice.getValue(index);
        Num upper = upperBand.getValue(index);
        Num lower = lowerBand.getValue(index);

        boolean isUptrend = price.isGreaterThan(upper)
                && macd.getValue(index).isGreaterThan(signalLine.getValue(index))
                && stc.getValue(index).isGreaterThan(closePrice.numOf(75))
                && ema50.getValue(index).isGreaterThan(ema200.getValue(index))
                && adx.getValue(index).isGreaterThan(closePrice.numOf(25))
                && plusDI.getValue(index).isGreaterThan(minusDI.getValue(index));

        boolean isDowntrend = price.isLessThan(lower)
                && macd.getValue(index).isLessThan(signalLine.getValue(index))
                && stc.getValue(index).isLessThan(closePrice.numOf(25))
                && ema50.getValue(index).isLessThan(ema200.getValue(index))
                && adx.getValue(index).isGreaterThan(closePrice.numOf(25))
                && minusDI.getValue(index).isGreaterThan(plusDI.getValue(index));

        boolean isSideways = adx.getValue(index).isLessThan(closePrice.numOf(20))
                || (macd.getValue(index).isBetween(signalLine.getValue(index).minus(closePrice.numOf(0.1)),
                signalLine.getValue(index).plus(closePrice.numOf(0.1))))
                || (stc.getValue(index).isBetween(closePrice.numOf(40), closePrice.numOf(60)));

        if (isUptrend) return "📈 Uptrend";
        if (isDowntrend) return "📉 Downtrend";
        if (isSideways) return "➖ Sideways";

        return "❓ Undefined Trend";
    }
}

