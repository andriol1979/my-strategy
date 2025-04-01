package com.vut.mystrategy.service.testing;

import com.vut.mystrategy.entity.BacktestDatum;
import com.vut.mystrategy.helper.ChartBuilderUtility;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.StrategyRunningRequest;
import com.vut.mystrategy.model.binance.KlineData;
import com.vut.mystrategy.model.binance.KlineEvent;
import com.vut.mystrategy.repository.BacktestDatumRepository;
import com.vut.mystrategy.service.KlineEventService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FeedDataService {
    private final BacktestDatumRepository backtestDatumRepository;
    private final KlineEventService klineEventService;
    private final Map<String, BarSeries> barSeriesMap;

    @Value("${feed-data-from-socket}")
    private boolean feedDataFromSocket;

    @Autowired
    public FeedDataService(BacktestDatumRepository backtestDatumRepository,
                           KlineEventService klineEventService,
                           @Qualifier("barSeriesMap") Map<String, BarSeries> barSeriesMap) {
        this.backtestDatumRepository = backtestDatumRepository;
        this.klineEventService = klineEventService;
        this.barSeriesMap = barSeriesMap;
    }

    @SneakyThrows
    public void runStrategyTesting(StrategyRunningRequest request) {
        if(feedDataFromSocket) {
            log.info("Feed data from socket is enabled. Can not run strategy testing.");
            return;
        }

        Sort sort = Sort.by(Sort.Direction.ASC, "eventTime");
        List<BacktestDatum> backTestData = backtestDatumRepository.findByExchangeNameAndSymbolAndKlineInterval(request.getExchangeName(),
                request.getSymbol(), request.getKlineInterval(), sort);
        backTestData = backTestData.subList(1000, 10000);
        log.info("Total loaded {} BackTestDatum from database", backTestData.size());

        // convert back test data to kline event to keep the same logic when feeding data from websocket
        List<KlineEvent> klineEventList = generateKlineEvents(backTestData);
        klineEventList.sort(Comparator.comparing(KlineEvent::getEventTime));
        for(KlineEvent klineEvent : klineEventList) {
            Thread.sleep(250);
            //Run strategy
            klineEventService.feedKlineEvent(request.getMyStrategyMapKey(), request.getExchangeName(), klineEvent);
        }
        //Export chart
/*
        KlineEvent klineEvent = klineEventList.get(0);
        String mapKey = KeyUtility.getBarSeriesMapKey(request.getExchangeName(), klineEvent.getSymbol(),
                klineEvent.getKlineData().getInterval());
        ChartBuilderUtility.createCandlestickChart(barSeriesMap.get(mapKey),
                request.getExchangeName(), klineEvent.getSymbol(),
                klineEvent.getKlineData().getInterval());
*/
    }

    private List<KlineEvent> generateKlineEvents(List<BacktestDatum> backtestData) {
        List<KlineEvent> klineEvents = new ArrayList<>();
        for (BacktestDatum backtestDatum : backtestData) {
            KlineEvent klineEvent = KlineEvent.builder()
                    .symbol(backtestDatum.getSymbol())
                    .eventType("kline")
                    .eventTime(Utility.getEpochMilliByInstant(backtestDatum.getEventTime()))
                    .klineData(
                        KlineData.builder()
                                .closeTime(backtestDatum.getEventTime().getEpochSecond())
                                .openPrice(backtestDatum.getOpen().toPlainString())
                                .highPrice(backtestDatum.getHigh().toPlainString())
                                .lowPrice(backtestDatum.getLow().toPlainString())
                                .closePrice(backtestDatum.getClose().toPlainString())
                                .quoteVolume(backtestDatum.getVolume().toPlainString())
                                .interval(backtestDatum.getKlineInterval())
                                .isClosed(true)
                                .build()
                    )
                    .build();
            klineEvents.add(klineEvent);
        }
        return klineEvents;
    }
}
