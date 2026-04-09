package com.vut.mystrategy.service.testing;

import com.vut.mystrategy.entity.BackTestKlineData;
import com.vut.mystrategy.entity.BacktestDatum;
import com.vut.mystrategy.configuration.BarSeriesBeanBuilder;
import com.vut.mystrategy.helper.BarDurationHelper;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.StrategyRunningRequest;
import com.vut.mystrategy.model.binance.KlineData;
import com.vut.mystrategy.model.binance.KlineEvent;
import com.vut.mystrategy.repository.BackTestKlineDatumRepository;
import com.vut.mystrategy.repository.BacktestDatumRepository;
import com.vut.mystrategy.service.KlineEventService;
import com.vut.mystrategy.service.OrderService;
import com.vut.mystrategy.service.RedisClientService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.TradingRecord;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.vut.mystrategy.helper.Calculator.ROUNDING_MODE_HALF_UP;

@Slf4j
@Service
public class FeedDataService {
    private final BacktestDatumRepository backtestDatumRepository;
    private final BackTestKlineDatumRepository backTestKlineDatumRepository;
    private final KlineEventService klineEventService;
    private final OrderService orderService;
    private final RedisClientService redisClientService;
    private final Map<String, BarSeries> barSeriesMap;
    private final Map<String, TradingRecord> tradingRecordsdMap;

    @Value("${feed-data-websocket}")
    private boolean feedDataWebSocket;

    @Autowired
    public FeedDataService(BacktestDatumRepository backtestDatumRepository,
                           BackTestKlineDatumRepository backTestKlineDatumRepository,
                           KlineEventService klineEventService,
                           OrderService orderService,
                           RedisClientService redisClientService,
                           @Qualifier("barSeriesMap") Map<String, BarSeries> barSeriesMap,
                           @Qualifier("tradingRecordsdMap") Map<String, TradingRecord> tradingRecordsdMap) {
        this.backtestDatumRepository = backtestDatumRepository;
        this.backTestKlineDatumRepository = backTestKlineDatumRepository;
        this.klineEventService = klineEventService;
        this.orderService = orderService;
        this.redisClientService = redisClientService;
        this.barSeriesMap = barSeriesMap;
        this.tradingRecordsdMap = tradingRecordsdMap;
    }

    @SneakyThrows
    public void runStrategyTestingNew(StrategyRunningRequest request) {
        if(feedDataWebSocket) {
            log.info("Feed data from socket is enabled. Can not run strategy testing.");
            return;
        }

        long startedAt = System.currentTimeMillis();
        log.info("Starting runStrategyTestingNew for {} {} {} with maxBars={} and sleepMillis={}",
                request.getExchangeName(), request.getSymbol(), request.getKlineInterval(),
                request.getMaxBars(), request.getSleepMillis());
        resetBacktestState(request);

        Sort sort = Sort.by(Sort.Direction.ASC, "closeTime");
        List<BackTestKlineData> backTestData = backTestKlineDatumRepository.findByExchangeNameAndSymbolAndKlineInterval(request.getExchangeName(),
                request.getSymbol(), request.getKlineInterval(), sort);
        backTestData = limitBacktestBars(backTestData, request.getMaxBars());
        log.info("Total loaded {} BackTestDatum from database. Start generating KlineEvents...", backTestData.size());

        // convert back test data to kline event to keep the same logic when feeding data from websocket
        List<KlineEvent> klineEventList = generateKlineEventsFromBackTestKlineData(backTestData);
        log.info("Finished generating {} KlineEvents from BackTestDatum", klineEventList.size());
        klineEventList.sort(Comparator.comparing(KlineEvent::getEventTime));
        long sleepMillis = getSleepMillis(request);
        for(KlineEvent klineEvent : klineEventList) {
            sleepIfNeeded(sleepMillis);
            //Run strategy
            klineEventService.feedKlineEventSync(request.getMyStrategyMapKey(), request.getExchangeName(), klineEvent);
        }
        log.info("Finished runStrategyTestingNew for {} {} {}. processedBars={}, elapsedMs={}",
                request.getExchangeName(), request.getSymbol(), request.getKlineInterval(),
                klineEventList.size(), System.currentTimeMillis() - startedAt);
    }

    @SneakyThrows
    public void runStrategyTesting(StrategyRunningRequest request) {
        if(feedDataWebSocket) {
            log.info("Feed data from socket is enabled. Can not run strategy testing.");
            return;
        }

        long startedAt = System.currentTimeMillis();
        log.info("Starting runStrategyTesting for {} {} {} with maxBars={} and sleepMillis={}",
                request.getExchangeName(), request.getSymbol(), request.getKlineInterval(),
                request.getMaxBars(), request.getSleepMillis());
        resetBacktestState(request);

        Sort sort = Sort.by(Sort.Direction.ASC, "eventTime");
        List<BacktestDatum> backTestData = backtestDatumRepository.findByExchangeNameAndSymbolAndKlineInterval(request.getExchangeName(),
                request.getSymbol(), request.getKlineInterval(), sort);
        backTestData = limitBacktestBars(backTestData, request.getMaxBars());
        log.info("Total loaded {} BackTestDatum from database. Start generating KlineEvents...", backTestData.size());

        // convert back test data to kline event to keep the same logic when feeding data from websocket
        List<KlineEvent> klineEventList = generateKlineEvents(backTestData);
        log.info("Finished generating {} KlineEvents from BackTestDatum", klineEventList.size());
        klineEventList.sort(Comparator.comparing(KlineEvent::getEventTime));
        int index = 0;
        long sleepMillis = getSleepMillis(request);
        for(KlineEvent klineEvent : klineEventList) {
            BigDecimal takerBuyBaseVolume = fakeTakerBuyBaseVolume(klineEventList, index);
            klineEvent.getKlineData().setTakerBuyBaseVolume(takerBuyBaseVolume.toPlainString());
            index++;
            sleepIfNeeded(sleepMillis);
            //Run strategy
            klineEventService.feedKlineEventSync(request.getMyStrategyMapKey(), request.getExchangeName(), klineEvent);
        }
        log.info("Finished runStrategyTesting for {} {} {}. processedBars={}, elapsedMs={}",
                request.getExchangeName(), request.getSymbol(), request.getKlineInterval(),
                klineEventList.size(), System.currentTimeMillis() - startedAt);
    }

    private <T> List<T> limitBacktestBars(List<T> backTestData, Integer maxBars) {
        if (backTestData == null || backTestData.isEmpty() || maxBars == null || maxBars <= 0 || backTestData.size() <= maxBars) {
            return backTestData;
        }
        return new ArrayList<>(backTestData.subList(backTestData.size() - maxBars, backTestData.size()));
    }

    private long getSleepMillis(StrategyRunningRequest request) {
        if (request.getSleepMillis() == null) {
            return 100L;
        }
        return Math.max(request.getSleepMillis(), 0L);
    }

    private void sleepIfNeeded(long sleepMillis) throws InterruptedException {
        if (sleepMillis > 0) {
            Thread.sleep(sleepMillis);
        }
    }

    private void resetBacktestState(StrategyRunningRequest request) {
        String barSeriesMapKey = KeyUtility.getBarSeriesMapKey(request.getExchangeName(),
                request.getSymbol(), request.getKlineInterval());
        String orderStorageRedisKey = KeyUtility.getOrderResponseStorageRedisKey(request.getExchangeName(),
                request.getSymbol(), request.getKlineInterval());

        barSeriesMap.put(barSeriesMapKey, BarSeriesBeanBuilder.buildBarSeries(barSeriesMapKey));
        tradingRecordsdMap.put(barSeriesMapKey, new BaseTradingRecord());
        redisClientService.deleteDataByKey(orderStorageRedisKey);
        orderService.deleteOrdersByBacktestScope(request.getExchangeName(), request.getSymbol(), request.getKlineInterval());
        log.info("Reset backtest state for {}", barSeriesMapKey);
    }

    private List<KlineEvent> generateKlineEvents(List<BacktestDatum> backtestData) {
        List<KlineEvent> klineEvents = new ArrayList<>();
        for (BacktestDatum backtestDatum : backtestData) {
            Duration barDuration = BarDurationHelper.getDurationByValue(backtestDatum.getKlineInterval());
            long closeTime = Utility.getEpochMilliByInstant(backtestDatum.getEventTime());
            KlineEvent klineEvent = KlineEvent.builder()
                    .symbol(backtestDatum.getSymbol())
                    .eventType("kline")
                    .eventTime(closeTime)
                    .klineData(
                        KlineData.builder()
                                .startTime(closeTime - barDuration.toMillis())
                                .closeTime(closeTime)
                                .openPrice(backtestDatum.getOpen().toPlainString())
                                .highPrice(backtestDatum.getHigh().toPlainString())
                                .lowPrice(backtestDatum.getLow().toPlainString())
                                .closePrice(backtestDatum.getClose().toPlainString())
                                .baseVolume(backtestDatum.getVolume().toPlainString())
                                .interval(backtestDatum.getKlineInterval())
                                .isClosed(backtestDatum.isClosed())
                                .build()
                    )
                    .build();
            klineEvents.add(klineEvent);
        }
        return klineEvents;
    }

    private BigDecimal fakeTakerBuyBaseVolume(List<KlineEvent> klineEventList, int currentIndex) {
        int avgPeriod = 10;
        double random;
        BigDecimal currentVolume = new BigDecimal(klineEventList.get(currentIndex).getKlineData().getBaseVolume());
        if(currentIndex < avgPeriod) {
            random = ThreadLocalRandom.current().nextDouble(0.45, 0.55);
        }
        else {
            BigDecimal totalVolume = BigDecimal.ZERO;
            for(KlineEvent klineEvent : klineEventList.subList(currentIndex - avgPeriod, currentIndex)) {
                totalVolume = totalVolume.add(new BigDecimal(klineEvent.getKlineData().getBaseVolume()));
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

    private List<KlineEvent> generateKlineEventsFromBackTestKlineData(List<BackTestKlineData> backtestData) {
        List<KlineEvent> klineEvents = new ArrayList<>();
        for (BackTestKlineData backtestDatum : backtestData) {
            KlineEvent klineEvent = KlineEvent.builder()
                    .symbol(backtestDatum.getSymbol())
                    .eventType("kline")
                    .eventTime(backtestDatum.getCloseTime())
                    .klineData(
                            KlineData.builder()
                                    .startTime(backtestDatum.getOpenTime())
                                    .closeTime(backtestDatum.getCloseTime())
                                    .openPrice(backtestDatum.getOpen().toPlainString())
                                    .highPrice(backtestDatum.getHigh().toPlainString())
                                    .lowPrice(backtestDatum.getLow().toPlainString())
                                    .closePrice(backtestDatum.getClose().toPlainString())
                                    .baseVolume(backtestDatum.getVolume().toPlainString())
                                    .quoteVolume(backtestDatum.getQuoteVolume().toPlainString())
                                    .takerBuyBaseVolume(backtestDatum.getTakerBuyVolume().toPlainString())
                                    .takerBuyQuoteVolume(backtestDatum.getTakerBuyQuoteVolume().toPlainString())
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
