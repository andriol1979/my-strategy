package com.vut.mystrategy.model.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @JsonProperty("e")
    private String eventType;       // "e": Loại sự kiện (e.g., "trade")

    @JsonProperty("E")
    private long eventTime;         // "E": Thời gian sự kiện (Unix timestamp, ms)

    @JsonProperty("s")
    private String symbol;          // "s": Cặp tiền (e.g., "BNBUSDT")
}
