package com.vut.mystrategy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Builder(toBuilder = true)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_name", nullable = false)
    private String exchangeName; // Ví dụ: "binance"

    @Column(name = "symbol", nullable = false)
    private String symbol; // Ví dụ: "BTCUSDT"

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId; // ID lệnh từ Binance

    @Column(name = "client_order_id")
    private String clientOrderId; // ID tùy chỉnh (nếu có)

    @Column(name = "side", nullable = false)
    private String side; // BUY hoặc SELL

    @Column(name = "type", nullable = false)
    private String type; // MARKET, LIMIT, TRAILING_STOP_MARKET, etc.

    @Column(name = "executed_qty", nullable = false, precision = 18, scale = 8)
    private BigDecimal executedQty; // Số lượng

    @Column(name = "cum_quote", precision = 18, scale = 8)
    private BigDecimal cumQuote; // Tổng giá trị khớp -> USDT

    @Column(name = "entry_price", precision = 18, scale = 8)
    private BigDecimal entryPrice; // Giá vào (avgPrice khi mở)

    @Column(name = "exit_price", precision = 18, scale = 8)
    private BigDecimal exitPrice; // Giá ra (avgPrice khi đóng)

    @Column(name = "slippage", precision = 18, scale = 8)
    private BigDecimal slippage; // trượt giá

    @Column(name = "leverage", nullable = false)
    private Integer leverage; // Đòn bẩy (mặc định 5)

    @Column(name = "pnl", precision = 18, scale = 8)
    private BigDecimal pnl; // Lãi/lỗ thực hiện

    @Column(name = "status", nullable = false)
    private String status; // NEW, FILLED, CANCELED, etc.

    @Column(name = "position_side", nullable = false)
    private String positionSide; // BOTH, LONG, SHORT

    @Column(name = "created_at", nullable = false)
    private Long createdAt; // Thời gian mở lệnh

    @Column(name = "closed_at")
    private Long closedAt; // Thời gian đóng lệnh

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt; // Thời gian cập nhật cuối
}
