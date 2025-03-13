package com.vut.mystrategy.helper;

import java.util.UUID;

public class Utility {
    public  static String generateOrderId() {
        return "my-strategy-" + UUID.randomUUID();
    }

    public  static String getTradeEventRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + Constant.TRADE_STREAM_NAME;
    }

    public  static String getTradeEventIdRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + Constant.TRADE_STREAM_NAME + "-id";
    }

    public  static String getVolumeCounterRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@volume-counter";
    }

    public  static String getSmaCounterRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@sma-counter";
    }

    public  static String getSmaPriceRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@sma-prices";
    }

    public  static String getEmaPriceRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@ema-prices";
    }

    public  static String getVolumeRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@volumes";
    }

    public  static String getFutureLotSizeRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@future-lot-size";
    }

    public  static String getPriceTrendRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@price-trends";
    }

    public  static String getTempSumVolumeRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@temp-sum-volume";
    }
}
