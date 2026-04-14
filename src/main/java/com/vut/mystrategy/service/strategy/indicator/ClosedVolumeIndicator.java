package com.vut.mystrategy.service.strategy.indicator;

import com.vut.mystrategy.model.MyStrategyBaseBar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class ClosedVolumeIndicator  extends CachedIndicator<Num> {

    private final int barCount;

    /**
     * Constructor with {@code barCount} = 1.
     *
     * @param series the bar series
     */
    public ClosedVolumeIndicator(BarSeries series) {
        this(series, 1);
    }

    /**
     * Constructor.
     *
     * @param series   the bar series
     * @param barCount the time frame
     */
    public ClosedVolumeIndicator(BarSeries series, int barCount) {
        super(series);
        this.barCount = barCount;
    }

    @Override
    protected Num calculate(int index) {
        int startIndex = Math.max(0, index - this.barCount + 1);
        Num sumOfVolume = this.zero();
        for(int i = startIndex; i <= index; ++i) {
            MyStrategyBaseBar myStrategyBaseBar = (MyStrategyBaseBar) this.getBarSeries().getBar(i);
            if(myStrategyBaseBar.isClosed()) {
                sumOfVolume = sumOfVolume.plus(myStrategyBaseBar.getVolume());
            }
        }
        return sumOfVolume;
    }

    @Override
    public int getUnstableBars() {
        return this.barCount;
    }
}
