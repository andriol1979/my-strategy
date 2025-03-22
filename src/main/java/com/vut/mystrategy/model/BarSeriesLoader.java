package com.vut.mystrategy.model;

import com.vut.mystrategy.model.binance.KlineEvent;
import org.ta4j.core.*;
import org.ta4j.core.num.DoubleNum;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

public class BarSeriesLoader {

    public static BarSeries loadFromKlineEvents(List<KlineEvent> klineEvents) {
        BarSeries series = new BaseBarSeriesBuilder().build();
        klineEvents.forEach(klineEvent -> {
            Bar bar = BaseBar.builder()
                    .openPrice(DoubleNum.valueOf(klineEvent.getKlineData().getOpenPrice()))
                    .closePrice(DoubleNum.valueOf(klineEvent.getKlineData().getClosePrice()))
                    .highPrice(DoubleNum.valueOf(klineEvent.getKlineData().getHighPrice()))
                    .lowPrice(DoubleNum.valueOf(klineEvent.getKlineData().getLowPrice()))
                    .endTime(ZonedDateTime.from(Instant.ofEpochMilli(klineEvent.getEventTime())))
                    .build();
            series.addBar(bar);
        });

        return series;
    }
}
