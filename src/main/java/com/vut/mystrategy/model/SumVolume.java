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
public class SumVolume implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String exchangeName;
    private String symbol;                          // BNBUSDT
    private BigDecimal bullVolume;                  // Bull = Buyer Taker = Buy Volume:
    private BigDecimal bearVolume;                  // Bear = Seller Taker = Sell Volume
    /*
        - Nếu bull > bear: % chênh lệch = ((bull - bear) / bear) * 100.
        - Nếu bear > bull: % chênh lệch = ((bear - bull) / bull) * 100.
        - Nếu bằng nhau: % chênh lệch = 0.
        - Ex: 10% - 20%...
     */
    private BigDecimal bullBearVolumeDivergence;
    private Long timestamp;
}
