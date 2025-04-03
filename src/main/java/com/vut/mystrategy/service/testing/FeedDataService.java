package com.vut.mystrategy.service.testing;

import com.vut.mystrategy.entity.BacktestDatum;
import com.vut.mystrategy.helper.ChartBuilderUtility;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
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

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.vut.mystrategy.helper.Calculator.ROUNDING_MODE_HALF_UP;

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
        backTestData = backTestData.subList(2000, 10000);
        log.info("Total loaded {} BackTestDatum from database. Start generating KlineEvents...", backTestData.size());

        // convert back test data to kline event to keep the same logic when feeding data from websocket
        List<KlineEvent> klineEventList = generateKlineEvents(backTestData);
        log.info("Finished generating {} KlineEvents from BackTestDatum", klineEventList.size());
        klineEventList.sort(Comparator.comparing(KlineEvent::getEventTime));
        int index = 0;
        for(KlineEvent klineEvent : klineEventList) {
            BigDecimal takerBuyQuoteVolume = fakeTakerBuyQuoteVolume(klineEventList, index);
            klineEvent.getKlineData().setTakerBuyQuoteVolume(takerBuyQuoteVolume.toPlainString());
            index++;
            Thread.sleep(100);
            //Run strategy

//            LogMessage.printObjectDebugMessage(log, klineEvent);
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

    private BigDecimal fakeTakerBuyQuoteVolume(List<KlineEvent> klineEventList, int currentIndex) {
        int avgPeriod = 10;
        double random;
        BigDecimal currentVolume = new BigDecimal(klineEventList.get(currentIndex).getKlineData().getQuoteVolume());
        if(currentIndex < 10) {
            random = ThreadLocalRandom.current().nextDouble(0.45, 0.55);
        }
        else {
            BigDecimal totalVolume = BigDecimal.ZERO;
            for(KlineEvent klineEvent : klineEventList.stream().skip(currentIndex).limit(avgPeriod).toList()) {
                totalVolume = totalVolume.add(new BigDecimal(klineEvent.getKlineData().getQuoteVolume()));
            }
            BigDecimal avgVolumeInPeriod = totalVolume.divide(new BigDecimal(avgPeriod), ROUNDING_MODE_HALF_UP);
            if(currentVolume.compareTo(avgVolumeInPeriod) > 0) {
                // volume hiện tại cao hơn volume trung bình
                random = ThreadLocalRandom.current().nextDouble(0.45, 0.8);
            }
            else {
                random = ThreadLocalRandom.current().nextDouble(0.2, 0.55);
            }
        }
        return currentVolume.multiply(new BigDecimal(random)).setScale(2, ROUNDING_MODE_HALF_UP);
    }
}
