package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.SymbolConfigManager;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.model.PriceTrendEnum;
import com.vut.mystrategy.model.SmaTrend;
import com.vut.mystrategy.model.SmaPrice;
import com.vut.mystrategy.model.SymbolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
public class SmaTrendAnalyzer {

    private final SymbolConfigManager symbolConfigManager;
    private final RedisClientService redisClientService;

    @Autowired
    public SmaTrendAnalyzer(SymbolConfigManager symbolConfigManager,
                            RedisClientService redisClientService) {
        this.symbolConfigManager = symbolConfigManager;
        this.redisClientService = redisClientService;
    }

    @Async("analyzeSmaTrendAsync")
    public void analyzeSmaTrend(String exchangeName, String symbol) {
        SymbolConfig symbolConfig = symbolConfigManager.getSymbolConfig(exchangeName, symbol);
        //Get SMA based on base-trend-sma-period
        String smaPriceRedisKey = KeyUtility.getSmaIndicatorRedisKey(exchangeName, symbol);
        List<SmaPrice> smaPriceList = redisClientService.getDataList(smaPriceRedisKey, 0,
                symbolConfig.getBaseTrendSmaPeriod() - 1, SmaPrice.class);
        if(Utility.invalidDataList(smaPriceList, symbolConfig.getBaseTrendSmaPeriod())) {
            return;
        }

        // calculate SMA trend
        BigDecimal resistance = Calculator.getMaxPrice(smaPriceList, SmaPrice::getTopPrice);
        BigDecimal support = Calculator.getMinPrice(smaPriceList, SmaPrice::getBottomPrice);
        // Tính độ mạnh: SMA(0) - SMA(9) (mới nhất - cũ nhất)

        BigDecimal smaTrendLevel = analyzeSmaTrendLevelBySlope(smaPriceList);
        String smaTrendDirection = analyzeSmaTrendDirection(smaTrendLevel, symbolConfig.getSmaThreshold());
        BigDecimal smaTrendStrength = analyzeSmaTrendStrength(resistance, support);
        //save to redis
        String smaTrendRedisKey = KeyUtility.getSmaTrendRedisKey(exchangeName, symbol);
        SmaTrend smaTrend = SmaTrend.builder()
                .exchangeName(exchangeName)
                .symbol(symbol)
                .resistancePrice(resistance)
                .supportPrice(support)
                .smaTrendLevel(smaTrendLevel)
                .smaTrendDirection(smaTrendDirection)
                .smaTrendStrength(smaTrendStrength)
                .timestamp(System.currentTimeMillis())
                .build();

        // save to redis
        redisClientService.saveDataAsSingle(smaTrendRedisKey, smaTrend);
        LogMessage.printInsertRedisLogMessage(log, smaTrendRedisKey, smaTrend);
    }

    private BigDecimal analyzeSmaTrendLevelBySlope(List<SmaPrice> smaPriceList) {
        // Tính Level (Up - Down)
        BigDecimal newestSmaPrice = smaPriceList.get(0).getPrice();
        BigDecimal oldestSmaPrice = smaPriceList.get(smaPriceList.size() - 1).getPrice();

        return newestSmaPrice.subtract(oldestSmaPrice)
                .divide(BigDecimal.valueOf(smaPriceList.size() - 1), 4, RoundingMode.HALF_UP);
        /*
        positive: UP
        negative: DOWN
         */
    }

    private String analyzeSmaTrendDirection(BigDecimal smaTrendLevel, BigDecimal smaThreshold) {
        if (smaTrendLevel.abs().compareTo(smaThreshold) < 0) {
            return PriceTrendEnum.SIDEWAYS.getValue();
        }
        else if (smaTrendLevel.compareTo(BigDecimal.ZERO) > 0) {
            return PriceTrendEnum.UP.getValue();
        }
        else {
            return PriceTrendEnum.DOWN.getValue();
        }
    }

    private BigDecimal analyzeSmaTrendStrength(BigDecimal resistance, BigDecimal support) {
        if (support.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; //avoid divide 0
        }
        return resistance.subtract(support).divide(support, 4, RoundingMode.HALF_UP);
        //always positive
    }
}
