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
public class EntryLongSignalMonitor extends AbstractSignalMonitor {

    @Autowired
    public EntryLongSignalMonitor(TradingSignalAnalyzer tradingSignalAnalyzer,
                                  RedisClientService redisClientService,
                                  AbstractOrderManager orderManager,
                                  @Qualifier("dataFetchersMap") Map<String, DataFetcher> dataFetchersMap) {
        super(tradingSignalAnalyzer, redisClientService, orderManager, dataFetchersMap);
    }

    @Async("monitorEntryLongSignalAsync")
    @Override
    public void monitorSignal(DataFetcher dataFetcher) {
        //Nếu chưa có ENTRY-LONG order -> run monitorEntryLongSignalAsync to find entry long, else -> return
        String exchangeName = dataFetcher.getSymbolConfig().getExchangeName();
        String symbol = dataFetcher.getSymbolConfig().getSymbol();
        String entryLongOrderRedisKey = KeyUtility.getEntryLongOrderRedisKey(exchangeName, symbol);
        if(redisClientService.exists(entryLongOrderRedisKey)) {
            log.info("ENTRY-LONG Order of Exchange {} - Symbol {} is existing. No need to monitor ENTRY-LONG signal.", exchangeName, symbol);
            return;
        }
        if(dataFetcher.getMarketData() == null) {
            log.warn("MarketData is null in DataFetcher - Exchange {} - Symbol {}", exchangeName, symbol);
            return;
        }

        if(tradingSignalAnalyzer.isEntryLong(dataFetcher.getMarketData(), dataFetcher.getSymbolConfig())) {
            //ENTRY LONG
            TradeSignal tradeSignal = TradeSignal.builder()
                    .exchangeName(exchangeName)
                    .symbol(symbol)
                    .side(SideEnum.SIDE_BUY.getValue())
                    .positionSide(PositionSideEnum.POSITION_SIDE_LONG.getValue())
                    .price(dataFetcher.getMarketData().getTradeEvent().getPriceAsBigDecimal())
                    .stopLoss(dataFetcher.getMarketData().getSmaTrend().getSupportPrice()) //support in SMA Trend
                    .takeProfit(dataFetcher.getMarketData().getSmaTrend().getResistancePrice()) //resistance in SMA trend
                    .action("ENTRY-LONG")
                    .timestamp(System.currentTimeMillis())
                    .build();
            // trigger API to order BUY - LONG
            // TODO: call create Order from binance API

            // save order to postgres
            // TODO: split profile -> dev -> fake BinanceOrderResponse -> save Order to db
            orderManager.placeOrder(tradeSignal, dataFetcher.getSymbolConfig());

            // save to redis (new or override)
            //TODO: maybe remove save entryLongSignalRedisKey in the next time
            String entryLongSignalRedisKey = KeyUtility.getEntryLongSignalRedisKey(
                    dataFetcher.getSymbolConfig().getExchangeName(),
                    dataFetcher.getSymbolConfig().getSymbol());
            redisClientService.saveDataAsSingle(entryLongSignalRedisKey, tradeSignal);
            LogMessage.printInsertRedisLogMessage(log, entryLongSignalRedisKey, tradeSignal);
        }
    }
}
