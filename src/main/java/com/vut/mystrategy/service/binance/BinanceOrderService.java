package com.vut.mystrategy.service.binance;

import com.vut.mystrategy.configuration.feeddata.binance.BinanceExchangeInfoConfig;
import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.repository.OrderRepository;
import com.vut.mystrategy.service.AbstractOrderService;
import com.vut.mystrategy.service.RedisClientService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;

import java.math.BigDecimal;

@Slf4j
@Service
@Qualifier("binance")
public class BinanceOrderService extends AbstractOrderService {
    private final OrderRepository orderRepository;
    private final BinanceExchangeInfoConfig binanceExchangeInfoConfig;
    private final RedisClientService redisClientService;

    @Autowired
    public BinanceOrderService(OrderRepository orderRepository,
                               BinanceExchangeInfoConfig binanceExchangeInfoConfig,
                               RedisClientService redisClientService) {
        super();
        this.orderRepository = orderRepository;
        this.binanceExchangeInfoConfig = binanceExchangeInfoConfig;
        this.redisClientService = redisClientService;
    }

    @Override
    public void saveOrderToDb(Order order) {
        orderRepository.save(order);
        log.info("BinanceOrderService created order: {}", order);
    }

    @Override
    public Order buildOrder(TradingRecord tradingRecord, SymbolConfig symbolConfig, boolean isShort) {
        if(!tradingRecord.isClosed() || tradingRecord.getTrades().size() <= 1) {
            log.warn("Order has not yet closed or not enough trading records to build order");
            return null;
        }

        Trade enterTrade = tradingRecord.getLastEntry();
        Trade exitTrade = tradingRecord.getLastExit();

        String orderRedisKey = KeyUtility.getOrderRedisKey(symbolConfig.getExchangeName(), symbolConfig.getSymbol());
        Order orderInRedis = redisClientService.getDataAsSingle(orderRedisKey, Order.class);
        //Check entry_index & exit_index
        if(orderInRedis != null && orderInRedis.getEntryIndex() == enterTrade.getIndex() &&
                orderInRedis.getExitIndex() == exitTrade.getIndex()) {
            return null;
        }

        BinanceFutureLotSizeResponse lotSize = binanceExchangeInfoConfig.getLotSizeBySymbol(symbolConfig.getSymbol());
        Pair<BigDecimal, BigDecimal> calculatedLotSize = Calculator.calculateLotSize(lotSize, symbolConfig.getOrderVolume(), enterTrade.getPricePerAsset().bigDecimalValue());

        Order order = new Order();
        order.setOrderId(KeyUtility.generateOrderId());
        order.setClientOrderId(KeyUtility.generateClientOrderId());
        order.setExchangeName(symbolConfig.getExchangeName());
        order.setSymbol(symbolConfig.getSymbol());
        order.setSide(isShort ? SideEnum.SIDE_SELL.getValue() : SideEnum.SIDE_BUY.getValue());
        order.setPositionSide(isShort ? PositionSideEnum.POSITION_SIDE_SHORT.getValue() : PositionSideEnum.POSITION_SIDE_LONG.getValue());
        order.setEntryPrice(enterTrade.getPricePerAsset().bigDecimalValue());
        order.setEntryIndex(enterTrade.getIndex());

        order.setExitPrice(exitTrade.getPricePerAsset().bigDecimalValue());
        order.setExitIndex(exitTrade.getIndex());

        order.setExecutedQty(calculatedLotSize.getLeft());
        order.setCumQuote(calculatedLotSize.getRight());
        order.setLeverage(symbolConfig.getLeverage());
        order.setSlippage(symbolConfig.getSlippage());
        order.setStatus("CLOSED");
        order.setType(TypeOrderEnum.TYPE_ORDER_MARKET.getValue());

        order.setCreatedAt(System.currentTimeMillis());
        order.setUpdatedAt(System.currentTimeMillis());
        order.setClosedAt(System.currentTimeMillis());
        order.setPnl(calculatePnL(order));

        //save new order to redis
        redisClientService.saveDataAsSingle(orderRedisKey, order);

        return order;
    }
}
