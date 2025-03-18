package com.vut.mystrategy.model;

import com.vut.mystrategy.model.binance.TradeEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private TradeEvent tradeEvent;              // current market price
    private SmaTrend smaTrend;                  // SMA trend to get resistance & support
    private VolumeTrend volumeTrend;
    private List<EmaPrice> shortEmaPricesList;
    private EmaPrice longEmaPrice;
}
