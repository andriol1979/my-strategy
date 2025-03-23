package com.vut.mystrategy.model;

import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.binance.KlineEvent;
import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;

import java.util.List;

public class BarSeriesLoader {

    public static BarSeries loadFromKlineEvents(List<KlineEvent> klineEvents) {
        BarSeries series = new BaseBarSeriesBuilder().build();
        klineEvents.forEach(klineEvent -> {
            KlineIntervalEnum klineEnum = KlineIntervalEnum.fromValue(klineEvent.getKlineData().getInterval());
            Bar bar = BaseBar.builder()
                    .openPrice(DecimalNum.valueOf(klineEvent.getKlineData().getOpenPrice()))
                    .closePrice(DecimalNum.valueOf(klineEvent.getKlineData().getClosePrice()))
                    .highPrice(DecimalNum.valueOf(klineEvent.getKlineData().getHighPrice()))
                    .lowPrice(DecimalNum.valueOf(klineEvent.getKlineData().getLowPrice()))
                    .endTime(Utility.getZonedDateTimeByEpochMilli(klineEvent.getEventTime()))
                    .timePeriod(new BarDuration(klineEnum).getDuration())
                    .build();
            series.addBar(bar);
        });

        return series;
    }
}
