package com.vut.mystrategy.helper;

import java.util.UUID;

public class KeyUtility {
    private static StringBuilder getExchangeSymbolAsKey(String exchangeName, String symbol) {
        return new StringBuilder()
                .append(exchangeName.toLowerCase())
                .append("@")
                .append(symbol.toLowerCase());
    }

    public static long generateOrderId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits() + System.currentTimeMillis());
    }

    public static String generateClientOrderId() {
        return UUID.randomUUID().toString();
    }

    public static String getTradeEventRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append(Constant.TRADE_STREAM_NAME).toString();
    }

    public static String getTradeEventIdRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append(Constant.TRADE_STREAM_NAME).append("-id").toString();
    }

    public static String getSmaCounterRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@sma-counter").toString();
    }

    public static String getSmaPriceRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@sma-prices").toString();
    }

    public static String getShortEmaPriceRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@short-ema-prices").toString();
    }

    public static String getLongEmaPriceRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@long-ema-prices").toString();
    }

    public static String getVolumeRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@volumes").toString();
    }

    public static String getFutureLotSizeRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@future-lot-size").toString();
    }

    public static String getSmaTrendRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@sma-trend").toString();
    }

    public static String getVolumeTrendRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@volume-trend").toString();
    }

    public static String getTempSumVolumeRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@temp-sum-volume").toString();
    }

    public static String getDataFetcherHashMapKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).toString();
    }

    //-------------------------Trading signal keys-------------------

    public static String getEntryLongSignalRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@entry-long-signal").toString();
    }

    public static String getExitLongSignalRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@exit-long-signal").toString();
    }

    public static String getEntryShortSignalRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@entry-short-signal").toString();
    }

    public static String getExitShortSignalRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@exit-short-signal").toString();
    }

    //-----------------------------Order keys-------------------

    public static String getLongOrderRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@long-order").toString();
    }

    public static String getExitLongOrderRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@exit-long-order").toString();
    }
}
