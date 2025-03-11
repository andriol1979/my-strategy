package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeEvent {
    @JsonProperty("e")
    private String eventType;       // "e": Loại sự kiện (e.g., "trade")

    @JsonProperty("E")
    private long eventTime;         // "E": Thời gian sự kiện (Unix timestamp, ms)

    @JsonProperty("s")
    private String symbol;          // "s": Cặp tiền (e.g., "BNBUSDT")

    @JsonProperty("t")
    private long tradeId;           // "t": ID giao dịch

    @JsonProperty("p")
    private String price;           // "p": Giá giao dịch (chuỗi để giữ precision)

    @JsonProperty("q")
    private String quantity;        // "q": Số lượng giao dịch (chuỗi để giữ precision)

    @JsonProperty("T")
    private long tradeTime;         // "T": Thời gian giao dịch (Unix timestamp, ms)

    @JsonProperty("m")
    private boolean isBuyerMaker;   // "m": Bên mua là market maker? (true = sell, false = buy)

    @JsonProperty("M")
    private boolean isBestMatch;    // "M": Có phải là best match không

    public BigDecimal getPriceAsBigDecimal() {
        return new BigDecimal(price);
    }

    public Date getTradeTimeAsDate() {
        return new Date(tradeTime);
    }
}
