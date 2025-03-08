package com.vut.mystrategy.repository;

import com.vut.mystrategy.entity.MyStrategyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MyStrategyOrderRepository extends JpaRepository<MyStrategyOrder, Long> {
    List<MyStrategyOrder> findByOrderStatus(String orderStatus);
}
