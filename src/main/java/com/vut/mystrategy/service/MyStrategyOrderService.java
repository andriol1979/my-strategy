package com.vut.mystrategy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vut.mystrategy.entity.MyStrategyOrder;
import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.Calculator;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.mapper.MyStrategyOrderMapper;
import com.vut.mystrategy.model.LotSizeResponse;
import com.vut.mystrategy.model.MyStrategyOrderRequest;
import com.vut.mystrategy.model.binance.BinanceFutureLotSizeResponse;
import com.vut.mystrategy.model.binance.TradeEvent;
import com.vut.mystrategy.repository.MyStrategyOrderRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
public class MyStrategyOrderService {

    private final TradingConfigManager tradingConfigManager;
    private final TradeEventService tradeEventService;
    private final MyStrategyOrderRepository myStrategyOrderRepository;

    @Autowired
    public MyStrategyOrderService(TradingConfigManager tradingConfigManager,
                                  TradeEventService tradeEventService,
                                  MyStrategyOrderRepository myStrategyOrderRepository) {
        this.tradingConfigManager = tradingConfigManager;
        this.tradeEventService = tradeEventService;
        this.myStrategyOrderRepository = myStrategyOrderRepository;
    }

    @SneakyThrows
    public MyStrategyOrder addWaitOrder(MyStrategyOrderRequest request) {
        MyStrategyOrder myStrategyOrder = buildNewMyStrategyOrder(request);
        log.info("MyStrategyOrder: {}", myStrategyOrder);
        return myStrategyOrderRepository.save(myStrategyOrder);
    }

    private MyStrategyOrder buildNewMyStrategyOrder(MyStrategyOrderRequest request) throws BadRequestException, JsonProcessingException {
        Optional<TradingConfig> optionalTradingConfig =  tradingConfigManager.getActiveConfigBySymbol(request.getExchangeName(), request.getSymbol());
        if (optionalTradingConfig.isEmpty()) {
            throw new BadRequestException("TradingConfig not found for symbol " + request.getSymbol() + " and exchange " + request.getExchangeName());
        }
        Optional<TradeEvent> tradeEvent = tradeEventService.getNewestTradeEvent(request.getExchangeName(), request.getSymbol());
        if (tradeEvent.isEmpty()) {
            throw new BadRequestException("TradeEvent not found for symbol " + request.getSymbol() + " and exchange " + request.getExchangeName());
        }
        LotSizeResponse lotSizeResponse = getLotSizeResponseFromOrderRequest(request);

        BigDecimal amount = new BigDecimal(optionalTradingConfig.get().getDefaultAmount());
        BigDecimal quantity = new BigDecimal(Calculator.calculateQuantity(lotSizeResponse,
                amount, tradeEvent.get().getPriceAsBigDecimal()));

        return MyStrategyOrderMapper.INSTANCE.toEntity(request)
                .toBuilder()
                .orderId(Utility.generateOrderId())
                .orderStatus(Constant.ORDER_STATUS_WAIT)
                .type(Constant.ORDER_TYPE_MARKET)
                .amount(amount)
                .quantity(quantity)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private LotSizeResponse getLotSizeResponseFromOrderRequest(MyStrategyOrderRequest request) throws JsonProcessingException, BadRequestException {
        if(request.getExchangeName().equalsIgnoreCase(Constant.EXCHANGE_NAME_BINANCE)){
            Optional<BinanceFutureLotSizeResponse> optional = tradeEventService.getBinanceFutureLotSizeFilter(request.getSymbol());
            if (optional.isEmpty()) {
                throw new BadRequestException("BinanceFutureLotSizeResponse not found for symbol " + request.getSymbol() + " and exchange " + request.getExchangeName());
            }

            return optional.get();
        }

        return new LotSizeResponse();
    }
}
