package com.vut.mystrategy.configuration;

import com.vut.mystrategy.helper.KeyUtility;
import com.vut.mystrategy.model.*;
import com.vut.mystrategy.model.binance.TradeEvent;
import com.vut.mystrategy.service.RedisClientService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Slf4j
//Do NOT add spring annotation -> DataFetcher just is a normal class
public class DataFetcher implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final RedisClientService redisClientService;
    @Getter
    private final SymbolConfig symbolConfig;
    @Getter
    private MarketData marketData;

    private final String exchangeName;
    private final String symbol;

    public DataFetcher(RedisClientService redisClientService, SymbolConfig symbolConfig) {
        this.redisClientService = redisClientService;
        this.symbolConfig = symbolConfig;
        this.exchangeName = symbolConfig.getExchangeName();
        this.symbol = symbolConfig.getSymbol();
    }

    @Async("marketDataFetcherAsync")
    public void fetchMarketData() {
        log.info("Fetching market data of exchange {} and symbol {}...", exchangeName, symbol);
        //Gen redis keys
        String tradeEventRedisKey = KeyUtility.getTradeEventRedisKey(exchangeName, symbol);
        String smaTrendRedisKey = KeyUtility.getSmaTrendRedisKey(exchangeName, symbol);
        String volumeTrendRedisKey = KeyUtility.getVolumeTrendRedisKey(exchangeName, symbol);
        String shortEmaPriceRedisKey = KeyUtility.getShortEmaPriceRedisKey(exchangeName, symbol);
        String longEmaPriceRedisKey = KeyUtility.getLongEmaPriceRedisKey(exchangeName, symbol);

        MarketData data = new MarketData();
        //Collect data
        TradeEvent tradeEvent = redisClientService.getDataByIndex(tradeEventRedisKey, 0, TradeEvent.class);
        SmaTrend smaTrend = redisClientService.getDataAsSingle(smaTrendRedisKey, SmaTrend.class);
        VolumeTrend volumeTrend = redisClientService.getDataAsSingle(volumeTrendRedisKey, VolumeTrend.class);
        // get 2 short EMA
        List<EmaPrice> shortEmaPricesList = redisClientService.getDataList(shortEmaPriceRedisKey, 0, 1, EmaPrice.class);
        EmaPrice longEmaPrice = redisClientService.getDataByIndex(longEmaPriceRedisKey, 0, EmaPrice.class);
        if(smaTrend == null || volumeTrend == null ||
                shortEmaPricesList == null || shortEmaPricesList.size() < 2 ||
                longEmaPrice == null || !tradeEventDataIsNew(tradeEvent)) {
            log.info("Not enough data to monitor trading signal of exchange {} - symbol {}",exchangeName, symbol);
            this.marketData = null;
            return;
        }

        data.setTradeEvent(tradeEvent);
        data.setSmaTrend(smaTrend);
        data.setVolumeTrend(volumeTrend);
        data.setShortEmaPricesList(shortEmaPricesList);
        data.setLongEmaPrice(longEmaPrice);
        this.marketData = data;
        log.info("DataFetcher fetched market data: {}", data);
    }

    private boolean tradeEventDataIsNew(TradeEvent tradeEvent) {
        return tradeEvent != null &&
                (System.currentTimeMillis() - tradeEvent.getEventTime() < 5000);
        //event time phải nằm trong khoảng 5s ~ now
        // ngừa trường hợp web socket disconnect
    }
}
