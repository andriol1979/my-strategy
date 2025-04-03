package com.vut.mystrategy.service.strategy.indicator;

import com.vut.mystrategy.model.MyStrategyBaseBar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class TakerBuyVolumeIndicator extends CachedIndicator<Num> {
    private final int barCount;

    public TakerBuyVolumeIndicator(BarSeries series) {
        this(series, 1);
    }

    public TakerBuyVolumeIndicator(BarSeries series, int barCount) {
        super(series);
        this.barCount = barCount;
    }

    protected Num calculate(int index) {
        int startIndex = Math.max(0, index - this.barCount + 1);
        Num sumOfVolume = this.zero();

        for(int i = startIndex; i <= index; ++i) {
            MyStrategyBaseBar myStrategyBaseBar = (MyStrategyBaseBar) this.getBarSeries().getBar(i);
            sumOfVolume = sumOfVolume.plus(myStrategyBaseBar.getTakerBuyVolume());
        }

        return sumOfVolume;
    }

    public int getUnstableBars() {
        return this.barCount;
    }
}
