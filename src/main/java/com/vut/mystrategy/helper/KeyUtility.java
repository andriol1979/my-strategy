package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.KlineIntervalEnum;

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

    public static String getBarSeriesMapKey(String exchangeName, String symbol, String klineInterval) {
        return getExchangeSymbolAsKey(exchangeName, symbol)
                .append(Constant.KLINE_STREAM_NAME)
                .append(klineInterval).toString();
    }

    public static String getKlineRedisKey(String exchangeName, String symbol, KlineIntervalEnum klineEnum) {
        return getExchangeSymbolAsKey(exchangeName, symbol)
                .append(Constant.KLINE_STREAM_NAME)
                .append(klineEnum.getValue()).toString();
    }

    public static String getIndicatorPeriodCounterRedisKey(String indicatorRedisKey) {
        return indicatorRedisKey + "@period-counter";
    }

    public static String getSmaIndicatorRedisKey(String exchangeName, String symbol, int smaPeriod) {
        return getExchangeSymbolAsKey(exchangeName, symbol)
                .append("@sma-indicator_")
                .append(smaPeriod).toString();
    }

    public static String getEmaIndicatorRedisKey(String exchangeName, String symbol, int emaPeriod) {
        return getExchangeSymbolAsKey(exchangeName, symbol)
                .append("@ema_indicator_")
                .append(emaPeriod).toString();
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

    //-----------------------------Order keys-------------------

    public static String getLongOrderRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@long-order").toString();
    }

    public static String getShortOrderRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@short-order").toString();
    }
}
