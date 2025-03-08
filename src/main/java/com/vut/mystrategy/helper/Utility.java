package com.vut.mystrategy.helper;

import java.util.UUID;

public class Utility {
    public  static String generateOrderId() {
        return "my-strategy-" + UUID.randomUUID();
    }

    public  static String getTradeEventRedisKey(String symbol) {
        return symbol + Constant.STREAM_NAME;
    }
}
