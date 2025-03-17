package com.vut.mystrategy.helper;

import java.util.UUID;

public class KeyUtility {
    public  static String generateOrderId() {
        return "my-strategy-" + UUID.randomUUID();
    }

    public  static String getTradeEventRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + Constant.TRADE_STREAM_NAME;
    }

    public  static String getTradeEventIdRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + Constant.TRADE_STREAM_NAME + "-id";
    }

    public  static String getSmaCounterRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@sma-counter";
    }

    public  static String getSmaPriceRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@sma-prices";
    }

    public  static String getShortEmaPriceRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@short-ema-prices";
    }

    public  static String getLongEmaPriceRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@long-ema-prices";
    }

    public  static String getVolumeRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@volumes";
    }

    public  static String getFutureLotSizeRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@future-lot-size";
    }

    public  static String getSmaTrendRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@sma-trend";
    }

    public  static String getVolumeTrendRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@volume-trend";
    }

    public  static String getTempSumVolumeRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@temp-sum-volume";
    }

    public  static String getTradingSignalRedisKey(String exchangeName, String symbol) {
        return exchangeName + "@" + symbol.toLowerCase() + "@trading-signal";
    }

    public  static String getSymbolConfigRedisKey() {
        return "rediskey@symbol-configs";
    }
}
