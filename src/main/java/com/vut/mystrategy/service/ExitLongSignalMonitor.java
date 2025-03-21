package com.vut.mystrategy.service;

import com.vut.mystrategy.configuration.DataFetcher;
import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ExitLongSignalMonitor extends AbstractSignalMonitor {

    @Autowired
    public ExitLongSignalMonitor(Map<String, AbstractOrderService> orderServices,
                                 TradingSignalAnalyzer tradingSignalAnalyzer,
                                 RedisClientService redisClientService,
                                 AbstractOrderManager orderManager) {
        super(orderServices, tradingSignalAnalyzer, redisClientService, orderManager);
    }

    @Async("monitorExitLongSignalAsync")
    @Override
    public void monitorSignal(DataFetcher dataFetcher) {
        //Nếu đã có ENTRY-LONG order -> run monitorExitLongSignalAsync to find exit long, else -> return
        String exchangeName = dataFetcher.getSymbolConfig().getExchangeName();
        String symbol = dataFetcher.getSymbolConfig().getSymbol();
        String longOrderRedisKey = KeyUtility.getLongOrderRedisKey(exchangeName, symbol);
        if(!redisClientService.exists(longOrderRedisKey)) {
            log.info("LONG Order of Exchange {} - Symbol {} does NOT exist. " +
                    "No need to monitor EXIT-LONG signal.", exchangeName, symbol);
            return;
        }
        if(dataFetcher.getMarketData() == null) {
            log.warn("MarketData is null in DataFetcher - Exchange {} - Symbol {}", exchangeName, symbol);
            return;
        }

        if(tradingSignalAnalyzer.isExitLong(dataFetcher.getMarketData(), dataFetcher.getSymbolConfig())) {
            //EXIT LONG
            TradeSignal tradeSignal = TradeSignal.builder()
                    .exchangeName(exchangeName)
                    .symbol(symbol)
                    .side(SideEnum.SIDE_SELL.getValue())
                    .positionSide(PositionSideEnum.POSITION_SIDE_LONG.getValue())
                    .price(dataFetcher.getMarketData().getTradeEvent().getPriceAsBigDecimal())
                    .stopLoss(dataFetcher.getMarketData().getSmaTrend().getSupportPrice()) //support in SMA Trend
                    .takeProfit(dataFetcher.getMarketData().getSmaTrend().getResistancePrice()) //resistance in SMA trend
                    .action("EXIT-LONG")
                    .timestamp(System.currentTimeMillis())
                    .build();
            LogMessage.printObjectLogMessage(log, tradeSignal);

            // TODO: call close Order via API - split profile here
            BaseOrderResponse closeOrderResponse = orderManager.closeOrder(tradeSignal, dataFetcher.getSymbolConfig());

            //Get long order + delete key from redis and update
            Order redisOrder = redisClientService.getDataAndDeleteAsSingle(longOrderRedisKey, Order.class);
            //Get instance order service based on exchangeName
            AbstractOrderService orderService = orderServices.get(exchangeName.toLowerCase());
            Order completedOrder = orderService.buildUpdatedOrder(redisOrder, closeOrderResponse);

            //save completed order to postgres
            orderService.saveOrderToDb(completedOrder);
        }
    }
}
