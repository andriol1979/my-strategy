package com.vut.mystrategy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.ta4j.core.Bar;
import org.ta4j.core.num.Num;

import java.io.Serial;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.function.Function;

@Builder
@AllArgsConstructor
public class MyStrategyBaseBar implements Bar {

    @Serial
    private static final long serialVersionUID = 8038383777467488147L;

    /** The time period (e.g. 1 day, 15 min, etc.) of the bar. */
    private final Duration timePeriod;

    private final ZonedDateTime beginTime;
    private final ZonedDateTime endTime;

    /** The open price of the bar period. */
    private Num openPrice;

    /** The high price of the bar period. */
    private Num highPrice;

    /** The low price of the bar period. */
    private Num lowPrice;

    /** The close price of the bar period. */
    private Num closePrice;

    /** The total traded volume of the bar period. */
    private Num volume; //base volume.Ex: BTCUSDT -> BTC

    @Getter
    private Num takerBuyVolume; //taker buy base volume.Ex: BTCUSDT -> BTC

    @Getter
    private Num takerSellVolume; //taker sell base volume.Ex: BTCUSDT -> BTC

    @Getter
    private  boolean isClosed;

    /** The total traded amount of the bar period. */
    private Num amount;

    /** The number of trades of the bar period. */
    private long trades;

    public MyStrategyBaseBar(Duration timePeriod, ZonedDateTime endTime, Function<Number, Num> numFunction) {
        this.openPrice = null;
        this.highPrice = null;
        this.lowPrice = null;
        this.closePrice = null;
        this.trades = 0L;
        checkTimeArguments(timePeriod, endTime);
        this.timePeriod = timePeriod;
        this.endTime = endTime;
        this.beginTime = endTime.minus(timePeriod);
        this.volume = (Num)numFunction.apply(0);
        this.amount = (Num)numFunction.apply(0);
    }

    @Override
    public Duration getTimePeriod() {
        return timePeriod;
    }

    @Override
    public ZonedDateTime getBeginTime() {
        return beginTime;
    }

    @Override
    public ZonedDateTime getEndTime() {
        return endTime;
    }

    @Override
    public Num getOpenPrice() {
        return openPrice;
    }

    @Override
    public Num getHighPrice() {
        return highPrice;
    }

    @Override
    public Num getLowPrice() {
        return lowPrice;
    }

    @Override
    public Num getClosePrice() {
        return closePrice;
    }

    @Override
    public Num getVolume() {
        return volume;
    }

    @Override
    public Num getAmount() {
        return amount;
    }

    @Override
    public long getTrades() {
        return trades;
    }

    @Override
    public void addTrade(Num tradeVolume, Num tradePrice) {
        addPrice(tradePrice);

        volume = volume.plus(tradeVolume);
        amount = amount.plus(tradeVolume.multipliedBy(tradePrice));
        trades++;
    }

    @Override
    public void addPrice(Num price) {
        if (openPrice == null) {
            openPrice = price;
        }
        closePrice = price;
        if (highPrice == null || highPrice.isLessThan(price)) {
            highPrice = price;
        }
        if (lowPrice == null || lowPrice.isGreaterThan(price)) {
            lowPrice = price;
        }
    }

    public void setTakerSellVolume() {
        this.takerSellVolume = this.volume.minus(this.takerBuyVolume);
    }

    private static void checkTimeArguments(Duration timePeriod, ZonedDateTime endTime) {
        Objects.requireNonNull(timePeriod, "Time period cannot be null");
        Objects.requireNonNull(endTime, "End time cannot be null");
    }

    /**
     * @return {end time, close price, open price, low price, high price, volume}
     */
    @Override
    public String toString() {
        return String.format(
                "{end time: %1s, close price: %2s, open price: %3s, low price: %4s high price: %5s, volume: %6s}",
                endTime, closePrice, openPrice, lowPrice, highPrice, volume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beginTime, endTime, timePeriod, openPrice, highPrice, lowPrice, closePrice, volume, amount,
                trades);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof MyStrategyBaseBar other))
            return false;
        return Objects.equals(beginTime, other.beginTime) && Objects.equals(endTime, other.endTime)
                && Objects.equals(timePeriod, other.timePeriod) && Objects.equals(openPrice, other.openPrice)
                && Objects.equals(highPrice, other.highPrice) && Objects.equals(lowPrice, other.lowPrice)
                && Objects.equals(closePrice, other.closePrice) && Objects.equals(volume, other.volume)
                && Objects.equals(amount, other.amount) && trades == other.trades
                && Objects.equals(takerBuyVolume, other.takerBuyVolume)
                && Objects.equals(takerSellVolume, other.takerSellVolume)
                && Objects.equals(isClosed, other.isClosed);
    }
}
