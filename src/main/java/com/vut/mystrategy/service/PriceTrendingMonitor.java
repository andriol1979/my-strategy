package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.AveragePrice;
import com.vut.mystrategy.model.PriceTrend;
import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PriceTrendingMonitor {

    private final TradingConfigManager tradingConfigManager;
    private final RedisClientService redisClientService;

    private final Integer redisTradeEventMaxSize;
    private final Integer redisTradeEventGroupSize;

    @Autowired
    public PriceTrendingMonitor(TradingConfigManager tradingConfigManager,
            RedisClientService redisClientService,
            @Qualifier("redisTradeEventMaxSize") Integer redisTradeEventMaxSize,
            @Qualifier("redisTradeEventGroupSize") Integer redisTradeEventGroupSize) {
        this.tradingConfigManager = tradingConfigManager;
        this.redisClientService = redisClientService;
        this.redisTradeEventMaxSize = redisTradeEventMaxSize;
        this.redisTradeEventGroupSize = redisTradeEventGroupSize;
    }

    @Async("calculateAveragePriceAsync")
    public void calculateAveragePrice(String exchangeName, String symbol) {
        String tradeEventRedisKey = Utility.getTradeEventRedisKey(exchangeName, symbol);
        List<TradeEvent> groupTradeEventList = redisClientService.getDataList(tradeEventRedisKey,
                0, redisTradeEventGroupSize - 1, TradeEvent.class);

        BigDecimal avgPrice = Calculator.calculateTradeEventAveragePrice(groupTradeEventList, redisTradeEventGroupSize);
        if(avgPrice != null) {
            String averageKey = Utility.getTradeEventAveragePriceRedisKey(exchangeName, symbol);
            AveragePrice averagePrice = AveragePrice.builder()
                    .exchangeName(exchangeName)
                    .symbol(symbol)
                    .price(avgPrice)
                    .timestamp(System.currentTimeMillis())
                    .build();

            redisClientService.saveDataAsList(averageKey, averagePrice, redisTradeEventMaxSize - 1);
            LogMessage.printInsertRedisLogMessage(log, averageKey, averagePrice);
        }
    }

    @SneakyThrows
    @Async("priceTrendingMonitorAsync")
    public void calculatePriceTrend(String exchangeName, String symbol) {
        Optional<TradingConfig> optTradingConfig =  tradingConfigManager.getActiveConfigBySymbol(exchangeName, symbol);
        if (optTradingConfig.isEmpty()) {
            throw new BadRequestException("TradingConfig not found for symbol " + symbol + " and exchange " + exchangeName);
        }
        String averageKey = Utility.getTradeEventAveragePriceRedisKey(exchangeName, symbol);
        if(!redisClientService.exists(averageKey)) {
            return;
        }

        AveragePrice currAveragePrice = redisClientService.getDataByIndex(averageKey, 0, AveragePrice.class);
        AveragePrice prevAveragePrice = redisClientService.getDataByIndex(averageKey, 1, AveragePrice.class);
        if(currAveragePrice == null || prevAveragePrice == null) {
            return;
        }

        BigDecimal currAvg = currAveragePrice.getPrice();
        BigDecimal prevAvg = prevAveragePrice.getPrice();

        //Save price trend to redis
        PriceTrend priceTrend = calculateAndBuildPriceTrend(exchangeName, symbol, currAvg, prevAvg, optTradingConfig.get().getThreshold());
        String priceTrendRedisKey = Utility.getPriceTrendRedisKey(exchangeName, symbol);
        redisClientService.saveDataAsList(priceTrendRedisKey, priceTrend, redisTradeEventMaxSize);
        LogMessage.printInsertRedisLogMessage(log, priceTrendRedisKey, priceTrend);
    }

    private PriceTrend calculateAndBuildPriceTrend(String exchangeName, String symbol,
                                                   BigDecimal currAvg, BigDecimal prevAvg, BigDecimal threshold) {
        BigDecimal changePrice = currAvg.subtract(prevAvg);
        BigDecimal thresholdPrice = prevAvg.multiply(threshold);
        BigDecimal percentChange = Calculator.calculatePercentPriceChange(currAvg, prevAvg);

        String priceTrendRedisKey = Utility.getPriceTrendRedisKey(exchangeName, symbol);
        PriceTrend prevPriceTrend = redisClientService.getDataByIndex(priceTrendRedisKey, 0, PriceTrend.class);
        PriceTrend currPriceTrend = PriceTrend.buildSimplePriceTrend(exchangeName, symbol, currAvg);

        //first item
        if(prevPriceTrend == null) {
            return currPriceTrend.toBuilder()
                    .trend(Constant.PRICE_TREND_UNKNOWN)
                    .level(0)
                    .strength(BigDecimal.valueOf(0))
                    .suggestion(Constant.PRICE_TREND_SUGGESTION_UNKNOWN)
                    .build();
        }

        //price has changed, but still don't throughout threshold
        if(Math.abs(changePrice.doubleValue()) < thresholdPrice.doubleValue()) {
            return currPriceTrend.toBuilder()
                    .trend(Constant.PRICE_TREND_SIDEWAYS)
                    .level(0)
                    .strength(percentChange)
                    .suggestion(Constant.PRICE_TREND_SUGGESTION_SIDEWAYS)
                    .build();
        }

        //else -> price has changed, and go throughout threshold
        if(changePrice.compareTo(BigDecimal.ZERO) > 0) {
            currPriceTrend.setTrend(Constant.PRICE_TREND_UP);
        }
        else if(changePrice.compareTo(BigDecimal.ZERO) < 0) {
            currPriceTrend.setTrend(Constant.PRICE_TREND_DOWN);
        }
        else {
            currPriceTrend.setTrend(Constant.PRICE_TREND_SIDEWAYS);
        }

        //set level
        int currLevel = currPriceTrend.getTrend().equals(prevPriceTrend.getTrend()) ? prevPriceTrend.getLevel() + 1 : 1;
        currPriceTrend.setLevel(currLevel);

        String currSuggestion;
        if(currPriceTrend.getLevel() > 1) {
            currSuggestion = currPriceTrend.getTrend().equals(Constant.PRICE_TREND_UP)
                    ? Constant.PRICE_TREND_SUGGESTION_SELL
                    : Constant.PRICE_TREND_SUGGESTION_BUY;
        }
        else {
            currSuggestion = Constant.PRICE_TREND_SUGGESTION_SIDEWAYS;
        }
        currPriceTrend.setSuggestion(currSuggestion);

        return currPriceTrend;
    }
}
