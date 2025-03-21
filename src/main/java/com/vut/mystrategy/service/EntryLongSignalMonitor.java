package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.DataFetcher;
import com.vut.mystrategy.entity.Order;
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
    public EntryLongSignalMonitor(Map<String, AbstractOrderService> orderServices,
                                  TradingSignalAnalyzer tradingSignalAnalyzer,
                                  RedisClientService redisClientService,
                                  AbstractOrderManager orderManager,
                                  @Qualifier("dataFetchersMap") Map<String, DataFetcher> dataFetchersMap) {
        super(orderServices, tradingSignalAnalyzer, redisClientService,
                orderManager, dataFetchersMap);
    }

    @Async("monitorEntryLongSignalAsync")
    @Override
    public void monitorSignal(DataFetcher dataFetcher) {
        //Nếu chưa có ENTRY-LONG order -> run monitorEntryLongSignalAsync to find entry long, else -> return
        String exchangeName = dataFetcher.getSymbolConfig().getExchangeName();
        String symbol = dataFetcher.getSymbolConfig().getSymbol();
        String longOrderRedisKey = KeyUtility.getLongOrderRedisKey(exchangeName, symbol);
        if(redisClientService.exists(longOrderRedisKey)) {
            log.info("LONG Order of Exchange {} - Symbol {} is existing. " +
                    "No need to monitor ENTRY-LONG signal.", exchangeName, symbol);
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
            LogMessage.printObjectLogMessage(log, tradeSignal);
            // trigger API to order BUY - LONG
            // TODO: call place Order via API - split profile here
            BaseOrderResponse placeOrderResponse = orderManager.placeOrder(tradeSignal, dataFetcher.getSymbolConfig());

            //Get instance order service based on exchangeName
            AbstractOrderService orderService = orderServices.get(exchangeName.toLowerCase());
            //save entryLongOrderRedisKey
            Order order = orderService.buildNewOrder(placeOrderResponse, dataFetcher.getSymbolConfig());
            redisClientService.saveDataAsSingle(longOrderRedisKey, order);
        }
    }
}
