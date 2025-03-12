package com.vut.mystrategy.helper;

import java.util.UUID;

public class Utility {
    public  static String generateOrderId() {
        return "my-strategy-" + UUID.randomUUID();
    }

    public  static String getTradeEventRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + Constant.TRADE_STREAM_NAME;
    }

    public  static String getTradeEventAverageCounterRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@average-counter";
    }

    public  static String getTradeEventAveragePriceRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@average-prices";
    }

    public  static String getFutureLotSizeRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@future-lot-size";
    }

    public  static String getPriceTrendRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@price-trends";
    }

    public  static String getVolumeTrendRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@volume-trends";
    }
}
