package com.vut.mystrategy.service;

import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.SmaTrend;
import com.vut.mystrategy.model.SmaPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class SmaTrendAnalyzer {

    private final RedisClientService redisClientService;
    private final Integer baseTrendSmaPeriod;

    @Autowired
    public SmaTrendAnalyzer(RedisClientService redisClientService,
                            @Qualifier("baseTrendSmaPeriod") Integer baseTrendSmaPeriod) {
        this.redisClientService = redisClientService;
        this.baseTrendSmaPeriod = baseTrendSmaPeriod;
    }

    @Async("analyzeSmaTrendAsync")
    public void analyzeSmaTrend(String exchangeName, String symbol) {
        //Get SMA based on base-trend-sma-period
        String smaPriceRedisKey = KeyUtility.getSmaPriceRedisKey(exchangeName, symbol);
        List<SmaPrice> smaPriceList = redisClientService.getDataList(smaPriceRedisKey, 0, baseTrendSmaPeriod - 1, SmaPrice.class);
        if(Utility.invalidDataList(smaPriceList, baseTrendSmaPeriod)) {
            return;
        }
        boolean newSmaTrend = false;
        String smaTrendRedisKey = KeyUtility.getSmaTrendRedisKey(exchangeName, symbol);
        SmaTrend smaTrend = redisClientService.getDataAsSingle(smaTrendRedisKey, SmaTrend.class);
        if(smaTrend == null) {
            newSmaTrend = true;
            smaTrend = SmaTrend.builder()
                    .exchangeName(exchangeName)
                    .symbol(symbol)
                    .smaTrendStrength(BigDecimal.ZERO)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        // calculate SMA trend
        BigDecimal resistance = Calculator.getMaxPrice(smaPriceList, SmaPrice::getTopPrice);
        BigDecimal support = Calculator.getMinPrice(smaPriceList, SmaPrice::getBottomPrice);
        int level = analyzeSmaTrendLevel(smaPriceList);
        // Tính độ mạnh: SMA(0) - SMA(9) (mới nhất - cũ nhất)
        BigDecimal newestSmaPrice = smaPriceList.get(0).getPrice();
        BigDecimal oldestSmaPrice = smaPriceList.get(smaPriceList.size() - 1).getPrice();
        BigDecimal strength = Calculator.getRateChange(newestSmaPrice, oldestSmaPrice);
        //save to redis
        smaTrend.setResistancePrice(resistance);
        smaTrend.setSupportPrice(support);
        smaTrend.setSmaTrendLevel(level);
        smaTrend.setSmaTrendStrength(strength);
        smaTrend.setTimestamp(System.currentTimeMillis());

        // save to redis
        redisClientService.saveDataAsSingle(smaTrendRedisKey, smaTrend);
        if(newSmaTrend) {
            LogMessage.printInsertRedisLogMessage(log, smaTrendRedisKey, smaTrend);
        }
        else {
            LogMessage.printUpdateRedisLogMessage(log, smaTrendRedisKey, smaTrend);
        }
    }

    private int analyzeSmaTrendLevel(List<SmaPrice> smaPriceList) {
        // Tính Level (Up - Down)
        int upCount = 0;
        int downCount = 0;
        for (int i = 1; i < smaPriceList.size(); i++) {
            BigDecimal diff = smaPriceList.get(i - 1).getPrice().subtract(smaPriceList.get(i).getPrice());
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                upCount++; // SMA trước (mới hơn) > SMA sau (cũ hơn) -> Up
            }
            else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                downCount++; // SMA trước < SMA sau -> Down
            }
        }
        return upCount - downCount;
    }

}
