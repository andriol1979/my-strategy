package com.vut.mystrategy.configuration;

import com.vut.mystrategy.entity.MyStrategyOrder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationData {

    private ApplicationData() {
    }

    public final static Map<String, MyStrategyOrder> MY_STRATEGY_WAIT_ORDER_MAP = new ConcurrentHashMap<>();
}
