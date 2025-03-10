package com.vut.mystrategy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "my_strategy_orders")
public class MyStrategyOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_name")
    private String exchangeName;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "order_status", nullable = false)
    private String orderStatus;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "type")
    private String type;

    @Column(name = "side", nullable = false)
    private String side;

    @Column(name = "position_side")
    private String positionSide;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "timestamp")
    private Long timestamp;
}
