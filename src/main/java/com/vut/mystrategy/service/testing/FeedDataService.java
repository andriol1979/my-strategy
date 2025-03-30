package com.vut.mystrategy.service.testing;

import com.vut.mystrategy.entity.BacktestDatum;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.KlineIntervalEnum;
import com.vut.mystrategy.model.binance.KlineData;
import com.vut.mystrategy.model.binance.KlineEvent;
import com.vut.mystrategy.repository.BacktestDatumRepository;
import com.vut.mystrategy.service.KlineEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FeedDataService {
    private final BacktestDatumRepository backtestDatumRepository;
    private final KlineEventService klineEventService;

    @Autowired
    public FeedDataService(BacktestDatumRepository backtestDatumRepository,
                           KlineEventService klineEventService) {
        this.backtestDatumRepository = backtestDatumRepository;
        this.klineEventService = klineEventService;
    }

    public void runStrategyTesting(String exchangeName, String symbol, String klineInterval) {
        Sort sort = Sort.by(Sort.Direction.ASC, "eventTime");
        List<BacktestDatum> backtestData = backtestDatumRepository.findByExchangeNameAndSymbolAndKlineInterval(exchangeName, symbol, klineInterval, sort);
        log.info("Total loaded {} BacktestDatum from database", backtestData.size());
        // convert backtest data to kline event to keep the same logic when feeding data from websocket
        List<KlineEvent> klineEventList = generateKlineEvents(backtestData);

        for(KlineEvent klineEvent : klineEventList) {
            //Run strategy
            klineEventService.saveKlineEvent(exchangeName, klineEvent.getSymbol(), klineEvent);
        }


    }

    private List<KlineEvent> generateKlineEvents(List<BacktestDatum> backtestData) {
        List<KlineEvent> klineEvents = new ArrayList<>();
        for (BacktestDatum backtestDatum : backtestData) {
            KlineEvent klineEvent = KlineEvent.builder()
                    .symbol(backtestDatum.getSymbol())
                    .eventType("kline")
                    .eventTime(backtestDatum.getEventTime().getEpochSecond())
                    .klineData(
                        KlineData.builder()
                                .closeTime(backtestDatum.getEventTime().getEpochSecond())
                                .openPrice(backtestDatum.getOpen().toPlainString())
                                .highPrice(backtestDatum.getHigh().toPlainString())
                                .lowPrice(backtestDatum.getLow().toPlainString())
                                .closePrice(backtestDatum.getClose().toPlainString())
                                .baseVolume(backtestDatum.getVolume().toPlainString())
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
