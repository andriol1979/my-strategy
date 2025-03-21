package com.vut.mystrategy.service;

import com.vut.mystrategy.model.*;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class VolumeTrendAnalyzer {

    private final RedisClientService redisClientService;

    @Autowired
    public VolumeTrendAnalyzer(RedisClientService redisClientService) {
        this.redisClientService = redisClientService;
    }

    @Async("analyzeVolumeTrendAsync")
    public void analyzeVolumeTrend(String exchangeName, String symbol, SymbolConfig symbolConfig) {
        //Get sum volumes base on base-trend-divergence-volume-period
        String volumeRedisKey = KeyUtility.getVolumeRedisKey(exchangeName, symbol);
        // Get a list of sum volumes
        List<SumVolume> sumVolumeList = redisClientService.getDataList(volumeRedisKey, 0,
                symbolConfig.getBaseTrendDivergenceVolumePeriod() - 1, SumVolume.class);
        if(Utility.invalidDataList(sumVolumeList, symbolConfig.getBaseTrendDivergenceVolumePeriod())) {
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

        // calculate volume trend
        SumVolume newSumVolume = sumVolumeList.get(0); //newest sum volume in list
        SumVolume prevSumVolume = sumVolumeList.get(sumVolumeList.size() - 1); //oldest sum volume in list
        BigDecimal newTotalVolume = newSumVolume.getBullVolume().add(newSumVolume.getBearVolume());
        BigDecimal prevTotalVolume = prevSumVolume.getBullVolume().add(prevSumVolume.getBearVolume());
        BigDecimal newBullBearDivergence = newSumVolume.getBullBearVolumeDivergence();
        String trendDirection = analyzeTrendDirection(newBullBearDivergence, prevSumVolume.getBullBearVolumeDivergence());
        String volumeSpike = analyzeVolumeSpike(newSumVolume, symbolConfig.getVolumeThreshold());
        //save to redis
        volumeTrend.setCurrTrendDirection(trendDirection);
        volumeTrend.setCurrDivergence(newBullBearDivergence);
        volumeTrend.setVolumeSpike(volumeSpike);
        volumeTrend.setNewTotalVolume(newTotalVolume);
        volumeTrend.setPrevTotalVolume(prevTotalVolume);
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
            trendDirection = Utility.concatVolumeTrendDirection(VolumeTrendEnum.BULL, VolumeTrendEnum.BULL); // Bullish tiếp diễn
        }
        else if (newDivergence.compareTo(BigDecimal.ZERO) < 0 && prevDivergence.compareTo(BigDecimal.ZERO) <= 0) {
            trendDirection =  Utility.concatVolumeTrendDirection(VolumeTrendEnum.BEAR, VolumeTrendEnum.BEAR); // Bearish tiếp diễn
        }
        else if (newDivergence.compareTo(BigDecimal.ZERO) > 0 && prevDivergence.compareTo(BigDecimal.ZERO) < 0) {
            trendDirection = Utility.concatVolumeTrendDirection(VolumeTrendEnum.BEAR, VolumeTrendEnum.BULL); // Đảo chiều từ bearish sang bullish
        }
        else if (newDivergence.compareTo(BigDecimal.ZERO) < 0 && prevDivergence.compareTo(BigDecimal.ZERO) > 0) {
            trendDirection = Utility.concatVolumeTrendDirection(VolumeTrendEnum.BULL, VolumeTrendEnum.BEAR); // Đảo chiều từ bullish sang bearish
        }
        else {
            trendDirection = VolumeTrendEnum.SIDEWAYS.getValue(); // Không rõ xu hướng
        }

        return trendDirection;
    }

    private String analyzeVolumeSpike(SumVolume newSumVolume, BigDecimal volumeThreshold) {
        BigDecimal bullVolume = newSumVolume.getBullVolume();
        BigDecimal bearVolume = newSumVolume.getBearVolume();
        //volume spike = BULL if bull_volume > bear_volume && bear/bull <= volumeThreshold (0.65)
        if(bullVolume.compareTo(bearVolume) > 0 &&
                Calculator.calculateRatio(bullVolume, bearVolume).compareTo(volumeThreshold) <= 0) {
            return VolumeSpikeEnum.BULL.getValue();
        }
        //volume spike = BEAR if bull_volume < bear_volume && bull/bear <= volumeThreshold (0.65)
        else if(bullVolume.compareTo(bearVolume) < 0 &&
                Calculator.calculateRatio(bullVolume, bearVolume).compareTo(volumeThreshold) <= 0) {
            return VolumeSpikeEnum.BEAR.getValue();
        }
        else {
            return VolumeSpikeEnum.FLAT.getValue();
        }
    }
}
