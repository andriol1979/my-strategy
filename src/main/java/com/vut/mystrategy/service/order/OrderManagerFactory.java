package com.vut.mystrategy.service.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderManagerFactory {
    private final Map<String, AbstractOrderManager> orderManagerMap;

    @Autowired
    public OrderManagerFactory(List<AbstractOrderManager> orderManagers) {
        this.orderManagerMap = new HashMap<>();
        for (AbstractOrderManager manager : orderManagers) {
            String beanName = manager.getClass().getAnnotation(Service.class).value(); // lấy tên tag như "binance-prod"
            orderManagerMap.put(beanName, manager);
        }
    }

    public AbstractOrderManager getOrderManager(String exchangeName, String profile) {
        String key = exchangeName.toLowerCase() + "-" + profile.toLowerCase();
        AbstractOrderManager manager = orderManagerMap.get(key);
        if (manager == null) {
            throw new IllegalArgumentException("No OrderManager found for key: " + key);
        }
        return manager;
    }
}
