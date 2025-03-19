package com.vut.mystrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmaTrend implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String exchangeName;
    private String symbol;

    // 2 fields are calculated based on 5 SMA periods -> triggered by SMA calculator
    private BigDecimal resistancePrice;
    private BigDecimal supportPrice;
    private BigDecimal smaTrendLevel; //positive = UP - negative = DOWN
    private String smaTrendDirection; //UP/DOWN/SIDEWAYS
    private BigDecimal smaTrendStrength; //tỷ lệ thay đổi giữa SMA cũ nhất và SMA mới nhất

    private long timestamp;
}

/*
smaTrendLevel (Slope)

Slope = (SMA cuối - SMA đầu) / (Số khoảng cách giữa các điểm)
SMA cuối: Giá trị SMA Price mới nhất (index 0 trong danh sách).
SMA đầu: Giá trị SMA Price cũ nhất (index 4 trong danh sách 5 điểm).
Số khoảng cách: Với 5 điểm, có 4 khoảng cách (5 - 1 = 4).
Kết quả:
Slope > 0: Xu hướng Up (tăng).
Slope < 0: Xu hướng Down (giảm).
Slope gần 0 (trong một ngưỡng): Xu hướng Sideways (đi ngang).
 */
