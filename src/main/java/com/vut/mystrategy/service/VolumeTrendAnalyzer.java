package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.SumVolume;
import com.vut.mystrategy.model.VolumeSpikeEnum;
import com.vut.mystrategy.model.VolumeTrend;
import com.vut.mystrategy.model.VolumeTrendEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class VolumeTrendAnalyzer {

    private final RedisClientService redisClientService;
    private final Integer baseTrendDivergenceVolumePeriod;

    @Autowired
    public VolumeTrendAnalyzer(RedisClientService redisClientService,
                               @Qualifier("baseTrendDivergenceVolumePeriod") Integer baseTrendDivergenceVolumePeriod) {
        this.redisClientService = redisClientService;
        this.baseTrendDivergenceVolumePeriod = baseTrendDivergenceVolumePeriod;
    }

    @Async("analyzeVolumeTrendAsync")
    public void analyzeVolumeTrend(String exchangeName, String symbol, TradingConfig tradingConfig) {
        //Get SMA based on base-trend-sma-period
        String volumeRedisKey = KeyUtility.getVolumeRedisKey(exchangeName, symbol);
        // Always get 2 sum volumes
        List<SumVolume> sumVolumeList = redisClientService.getDataList(volumeRedisKey, 0, baseTrendDivergenceVolumePeriod - 1, SumVolume.class);
        if(Utility.invalidDataList(sumVolumeList, baseTrendDivergenceVolumePeriod)) {
            return;
        }
        boolean newVolumeTrend = false;
        String volumeTrendRedisKey = KeyUtility.getVolumeTrendRedisKey(exchangeName, symbol);
        VolumeTrend volumeTrend = redisClientService.getDataAsSingle(volumeTrendRedisKey, VolumeTrend.class);
        if(volumeTrend == null) {
            newVolumeTrend = true;
            volumeTrend = VolumeTrend.builder()
                    .exchangeName(exchangeName)
                    .symbol(symbol)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
        //Set previous values by current values
        volumeTrend.setPrevTrendDirection(volumeTrend.getCurrTrendDirection());
        volumeTrend.setPrevDivergence(volumeTrend.getCurrDivergence());
        volumeTrend.setPrevTrendStrength(volumeTrend.getCurrTrendStrength());

        // calculate volume trend
        SumVolume newSumVolume = sumVolumeList.get(0);
        SumVolume prevSumVolume = sumVolumeList.get(sumVolumeList.size() - 1);
        BigDecimal newTotalVolume = newSumVolume.getBullVolume().add(newSumVolume.getBearVolume());
        BigDecimal prevTotalVolume = prevSumVolume.getBullVolume().add(prevSumVolume.getBearVolume());
        BigDecimal newDivergence = newSumVolume.getBullBearVolumeDivergence();
        String trendDirection = analyzeTrendDirection(newDivergence, prevSumVolume.getBullBearVolumeDivergence());
        BigDecimal trendStrength = Calculator.calculateVolumeTrendStrength(newTotalVolume, prevTotalVolume,
                newDivergence, tradingConfig.getDivergenceThreshold());
        String volumeSpike = analyzeVolumeSpike(newSumVolume, tradingConfig.getVolumeThreshold());
        //save to redis
        volumeTrend.setCurrTrendDirection(trendDirection);
        volumeTrend.setCurrDivergence(newDivergence);
        volumeTrend.setCurrTrendStrength(trendStrength);
        volumeTrend.setVolumeSpike(volumeSpike);
        volumeTrend.setTimestamp(System.currentTimeMillis());

        // save to redis
        redisClientService.saveDataAsSingle(volumeTrendRedisKey, volumeTrend);
        if(newVolumeTrend) {
            LogMessage.printInsertRedisLogMessage(log, volumeTrendRedisKey, volumeTrend);
        }
        else {
            LogMessage.printUpdateRedisLogMessage(log, volumeTrendRedisKey, volumeTrend);
        }
    }

    private String analyzeTrendDirection(BigDecimal newDivergence, BigDecimal prevDivergence) {
        // Xác định hướng xu hướng
        String trendDirection;
        if (newDivergence.compareTo(BigDecimal.ZERO) > 0 && prevDivergence.compareTo(BigDecimal.ZERO) >= 0) {
            trendDirection = VolumeTrendEnum.UP.getValue(); // Bullish tiếp diễn
        }
        else if (newDivergence.compareTo(BigDecimal.ZERO) < 0 && prevDivergence.compareTo(BigDecimal.ZERO) <= 0) {
            trendDirection = VolumeTrendEnum.DOWN.getValue(); // Bearish tiếp diễn
        }
        else if (newDivergence.compareTo(BigDecimal.ZERO) > 0 && prevDivergence.compareTo(BigDecimal.ZERO) < 0) {
            trendDirection = VolumeTrendEnum.UP.getValue(); // Đảo chiều từ bearish sang bullish
        }
        else if (newDivergence.compareTo(BigDecimal.ZERO) < 0 && prevDivergence.compareTo(BigDecimal.ZERO) > 0) {
            trendDirection = VolumeTrendEnum.DOWN.getValue(); // Đảo chiều từ bullish sang bearish
        }
        else {
            trendDirection = VolumeTrendEnum.NEUTRAL.getValue(); // Không rõ xu hướng
        }

        return trendDirection;
    }

    private String analyzeVolumeSpike(SumVolume newSumVolume, BigDecimal volumeThreshold) {
        if(newSumVolume.getBullVolume().compareTo(newSumVolume.getBearVolume().multiply(volumeThreshold)) > 0) {
            return VolumeSpikeEnum.BULL.getValue();
        }
        else if(newSumVolume.getBearVolume().compareTo(newSumVolume.getBullVolume().multiply(volumeThreshold)) > 0) {
            return VolumeSpikeEnum.BEAR.getValue();
        }
        else {
            return VolumeSpikeEnum.FLAT.getValue();
        }
    }
}
