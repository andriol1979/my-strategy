package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.DataFetcher;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ExitLongSignalMonitor extends AbstractSignalMonitor {

    @Autowired
    public ExitLongSignalMonitor(TradingSignalAnalyzer tradingSignalAnalyzer,
                                 RedisClientService redisClientService,
                                 @Qualifier("dataFetchersMap") Map<String, DataFetcher> dataFetchersMap) {
        super(tradingSignalAnalyzer, redisClientService, dataFetchersMap);
    }

    @Async("monitorExitLongSignalAsync")
    @Override
    public void monitorSignal(DataFetcher dataFetcher) {
        if(dataFetcher.getMarketData() == null) {
            log.warn("MarketData is null in DataFetcher - Exchange {} - Symbol {}",
                    dataFetcher.getSymbolConfig().getExchangeName(), dataFetcher.getSymbolConfig().getSymbol());
            return;
        }

        if(tradingSignalAnalyzer.isExitLong(dataFetcher.getMarketData(), dataFetcher.getSymbolConfig())) {
            //EXIT LONG
            TradeSignal tradeSignal = TradeSignal.builder()
                    .exchangeName(dataFetcher.getSymbolConfig().getExchangeName())
                    .symbol(dataFetcher.getSymbolConfig().getSymbol())
                    .side(SideEnum.SIDE_SELL.getValue())
                    .positionSide(PositionSideEnum.POSITION_SIDE_LONG.getValue())
                    .price(dataFetcher.getMarketData().getTradeEvent().getPriceAsBigDecimal())
                    .stopLoss(dataFetcher.getMarketData().getSmaTrend().getSupportPrice()) //support in SMA Trend
                    .takeProfit(dataFetcher.getMarketData().getSmaTrend().getResistancePrice()) //resistance in SMA trend
                    .action("EXIT-LONG")
                    .timestamp(System.currentTimeMillis())
                    .build();
            // save to redis -> or trigger API to order SELL - LONG
            // Always create new trading signal and save to redis (new or override)
            String exitLongSignalRedisKey = KeyUtility.getExitLongSignalRedisKey(
                    dataFetcher.getSymbolConfig().getExchangeName(),
                    dataFetcher.getSymbolConfig().getSymbol());
            redisClientService.saveDataAsSingle(exitLongSignalRedisKey, tradeSignal);
            LogMessage.printInsertRedisLogMessage(log, exitLongSignalRedisKey, tradeSignal);
        }
    }
}
