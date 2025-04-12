package com.vut.mystrategy.helper;

public class Constant {
    public static final String EXCHANGE_NAME_BINANCE = "binance";

    public static final String KLINE_STREAM_NAME = "@kline_"; //kline

    public static final String ORDER_STATUS_WAIT = "WAIT"; //wait entry to order
    public static final String ORDER_STATUS_ORDERED = "ORDERED"; //important status -> ordered to binance
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED"; //cancelled to binance -> will be deleted
    public static final String ORDER_STATUS_CLOSED = "CLOSED"; //closed to binance -> will be deleted
}
