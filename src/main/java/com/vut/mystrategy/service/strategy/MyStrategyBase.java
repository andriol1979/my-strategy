package com.vut.mystrategy.service.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;

public abstract class MyStrategyBase {
    protected BarSeries barSeries;
    protected TradingRecord tradingRecord;

    public MyStrategyBase(BarSeries barSeries, TradingRecord tradingRecord) {
        this.barSeries = barSeries;
        this.tradingRecord = tradingRecord;
    }

    public abstract Strategy buildLongStrategy(BarSeries barSeries);
    public abstract Strategy buildShortStrategy();
}
