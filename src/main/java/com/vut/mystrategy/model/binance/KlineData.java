package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KlineData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("t")
    private long startTime;           // 1672515780000 (timestamp)

    @JsonProperty("T")
    private long closeTime;           // 1672515839999 (timestamp)

    @JsonProperty("s")
    private String symbol;            // "BNBBTC"

    @JsonProperty("i")
    private String interval;          // "1m"

    @JsonProperty("f")
    private long firstTradeId;        // 100

    @JsonProperty("L")
    private long lastTradeId;         // 200

    @JsonProperty("o")
    private String openPrice;         // "0.0010" (String để giữ precision)

    @JsonProperty("c")
    private String closePrice;        // "0.0020"

    @JsonProperty("h")
    private String highPrice;         // "0.0025"

    @JsonProperty("l")
    private String lowPrice;          // "0.0015"

    @JsonProperty("v")
    private String baseVolume;        // "1000"

    @JsonProperty("n")
    private int numberOfTrades;       // 100

    @JsonProperty("x")
    private boolean isClosed;         // false

    @JsonProperty("q")
    private String quoteVolume;       // "1.0000"

    @JsonProperty("V")
    private String takerBuyBaseVolume; // "500"

    @JsonProperty("Q")
    private String takerBuyQuoteVolume; // "0.500"

    @JsonProperty("B")
    private String ignore;            // "123456"
}
