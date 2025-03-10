package com.vut.mystrategy.helper;

public class Constant {
    public static final String EXCHANGE_NAME_BINANCE = "binance";

    public static final String STREAM_NAME = "@trade";

    public static final String ORDER_STATUS_WAIT = "WAIT"; //wait entry to order
    public static final String ORDER_STATUS_ORDERED = "ORDERED"; //important status -> ordered to binance
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED"; //cancelled to binance -> will be deleted
    public static final String ORDER_STATUS_CLOSED = "CLOSED"; //closed to binance -> will be deleted

    public static final String ORDER_TYPE_LIMIT = "LIMIT";
    public static final String ORDER_TYPE_MARKET = "MARKET";

    public static final String ORDER_POSITION_SIDE_LONG = "LONG";
    public static final String ORDER_POSITION_SIDE_SHORT = "SHORT";

    public static final String PRICE_TREND_UP = "UP";
    public static final String PRICE_TREND_DOWN = "DOWN";
    public static final String PRICE_TREND_SIDEWAYS = "SIDEWAYS";
}
