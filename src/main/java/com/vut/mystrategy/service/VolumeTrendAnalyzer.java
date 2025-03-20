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
        // Always get 2 sum volumes
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
        volumeTrend.setPrevTrendStrength(volumeTrend.getCurrTrendStrength());

        // calculate volume trend
        SumVolume newSumVolume = sumVolumeList.get(0);
        SumVolume prevSumVolume = sumVolumeList.get(sumVolumeList.size() - 1);
        BigDecimal newTotalVolume = newSumVolume.getBullVolume().add(newSumVolume.getBearVolume());
        BigDecimal prevTotalVolume = prevSumVolume.getBullVolume().add(prevSumVolume.getBearVolume());
        BigDecimal newDivergence = newSumVolume.getBullBearVolumeDivergence();
        String trendDirection = analyzeTrendDirection(newDivergence, prevSumVolume.getBullBearVolumeDivergence());
        BigDecimal trendStrength = calculateVolumeTrendStrength(newTotalVolume, prevTotalVolume,
                newDivergence, symbolConfig.getDivergenceThreshold());
        String volumeSpike = analyzeVolumeSpike(newSumVolume, symbolConfig.getVolumeThreshold());
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
            trendDirection = VolumeTrendEnum.BULL.getValue(); // Bullish tiếp diễn
        }
        else if (newDivergence.compareTo(BigDecimal.ZERO) < 0 && prevDivergence.compareTo(BigDecimal.ZERO) <= 0) {
            trendDirection = VolumeTrendEnum.BEAR.getValue(); // Bearish tiếp diễn
        }
        else if (newDivergence.compareTo(BigDecimal.ZERO) > 0 && prevDivergence.compareTo(BigDecimal.ZERO) < 0) {
            trendDirection = VolumeTrendEnum.BULL.getValue(); // Đảo chiều từ bearish sang bullish
        }
        else if (newDivergence.compareTo(BigDecimal.ZERO) < 0 && prevDivergence.compareTo(BigDecimal.ZERO) > 0) {
            trendDirection = VolumeTrendEnum.BEAR.getValue(); // Đảo chiều từ bullish sang bearish
        }
        else {
            trendDirection = VolumeTrendEnum.SIDEWAYS.getValue(); // Không rõ xu hướng
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

    private BigDecimal calculateVolumeTrendStrength(BigDecimal newTotalVolume, BigDecimal prevTotalVolume,
                                                          BigDecimal newDivergence, BigDecimal divergenceThreshold) {
        if (newTotalVolume == null || prevTotalVolume == null || newDivergence == null || divergenceThreshold == null) {
            return BigDecimal.ZERO;
        }
        // strength = newDivergence / 100 because newDivergence is %. Ex: 10%, 20%...
        BigDecimal strength = newDivergence.abs().divide(Calculator.ONE_HUNDRED, Calculator.SCALE, Calculator.ROUNDING_MODE_HALF_UP);
        if (prevTotalVolume.compareTo(BigDecimal.ZERO) == 0) {
            return strength; // Giữ nguyên strength nếu prevTotalVolume = 0
        }
        BigDecimal volumeChangeRate = newTotalVolume.subtract(prevTotalVolume)
                .divide(prevTotalVolume, Calculator.SCALE, Calculator.ROUNDING_MODE_HALF_UP);
        BigDecimal adjustmentFactor = BigDecimal.ONE.add(volumeChangeRate);
        if (volumeChangeRate.abs().compareTo(divergenceThreshold) > 0) { // Ngưỡng 10%
            strength = strength.multiply(adjustmentFactor.max(new BigDecimal("0.5")).min(new BigDecimal("1.5")));
        }

        return strength;
    }
}
