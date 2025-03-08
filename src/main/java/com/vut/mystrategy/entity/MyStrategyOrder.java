package com.vut.mystrategy.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "my_strategy_orders")
public class MyStrategyOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "order_status", nullable = false)
    private String orderStatus;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "position_side")
    private String positionSide;

    @Column(name = "price")
    private double price;

    @Column(name = "quantity")
    private double quantity;

    @Column(name = "timestamp")
    private Long timestamp = System.currentTimeMillis();
}
