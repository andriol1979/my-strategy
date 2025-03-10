package com.vut.mystrategy.service;

import com.vut.mystrategy.entity.MyStrategyOrder;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.mapper.MyStrategyOrderMapper;
import com.vut.mystrategy.model.MyStrategyOrderRequest;
import com.vut.mystrategy.repository.MyStrategyOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyStrategyOrderService {

    private final MyStrategyOrderRepository myStrategyOrderRepository;

    @Autowired
    public MyStrategyOrderService(MyStrategyOrderRepository myStrategyOrderRepository) {
        this.myStrategyOrderRepository = myStrategyOrderRepository;
    }

    public MyStrategyOrder addWaitOrder(MyStrategyOrderRequest request) {
        MyStrategyOrder myStrategyOrder = buildNewMyStrategyOrder(request);
        return myStrategyOrderRepository.save(myStrategyOrder);
    }

    private MyStrategyOrder buildNewMyStrategyOrder(MyStrategyOrderRequest request) {
        MyStrategyOrder myStrategyOrder = MyStrategyOrderMapper.INSTANCE.toEntity(request);
        myStrategyOrder.toBuilder()
                .orderId(Utility.generateOrderId())
                .orderStatus(Constant.ORDER_STATUS_WAIT)
                .amount(null)
                .quantity(null)
                .build();
        return myStrategyOrder;
    }
}
