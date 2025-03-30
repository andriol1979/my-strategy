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

    public static String getBarSeriesMapKey(String exchangeName, String symbol, String klineInterval) {
        return getExchangeSymbolAsKey(exchangeName, symbol)
                .append("@")
                .append(klineInterval).toString();
    }

    public static String getShortOrderFlag(String exchangeName, String symbol, String strategyName) {
        return getExchangeSymbolAsKey(exchangeName, symbol)
                .append("@")
                .append(strategyName).toString();
    }

    public static String getFutureLotSizeMapKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).toString();
    }

    public static String getOrderRedisKey(String exchangeName, String symbol) {
        return getExchangeSymbolAsKey(exchangeName, symbol).append("@order").toString();
    }
}
