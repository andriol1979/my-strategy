package com.vut.mystrategy.helper;

import java.util.UUID;

public class Utility {
    public  static String generateOrderId() {
        return "my-strategy-" + UUID.randomUUID();
    }

    public  static String getTradeEventRedisKey(String symbol) {
        return symbol.toLowerCase() + Constant.STREAM_NAME;
    }

    public  static String getFutureLotSizeRedisKey(String symbol) {
        return symbol.toLowerCase() + "@future-lot-size";
    }
}
