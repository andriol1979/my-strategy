package com.vut.mystrategy.helper;

import com.vut.mystrategy.model.MyStrategyBaseBar;
import com.vut.mystrategy.model.SideEnum;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.ta4j.core.*;
import org.ta4j.core.criteria.*;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.num.Num;

public class LogMessage {

    @SneakyThrows
    public static void printTradeDebugMessage(Logger log, int index, Num closePrice,
                                              SideEnum sideEnum, Trade trade, boolean isShort) {
        StringBuilder message = new StringBuilder("************ ");
        if(isShort) {
            if(SideEnum.SIDE_SELL.getValue().equals(sideEnum.getValue())) {
                message.append("Open SHORT: ");
            }
            else {
                message.append("Close SHORT: ");
            }
        }
        else {
            if(SideEnum.SIDE_BUY.getValue().equals(sideEnum.getValue())) {
                message.append("Open LONG: ");
            }
            else {
                message.append("Close LONG: ");
            }
        }
        message.append(sideEnum.getValue()).append(" at Index: ").append(index).append(" - Price: ")
                .append(closePrice).append(" - Trade: ").append(trade);
        log.info("{} - Thread: {}", message, Thread.currentThread().getName());
    }

    @SneakyThrows
    public static void printBarDebugMessage(Logger log, int index, MyStrategyBaseBar bar, String barSeriesName) {
        BarLogging barLogging = new BarLogging(bar, index, barSeriesName);
        log.info("BAR debug: Index: {} - {} - Thread: {}", index,
                barLogging, Thread.currentThread().getName());
    }

    @SneakyThrows
    public static void printRuleDebugMessage(Logger log, int index, String message) {
        /*
        log.info("Rule debug: Index: {} - {} - Thread: {}", index,
                objectMapper.writeValueAsString(message), Thread.currentThread().getName());
        */
    }

    @SneakyThrows
    public static void printRuleMatchedMessage(Logger log, int index, String ruleName) {
        log.info(">>>>>>>>>>>> Rule matched: Index: {} - Rule triggered: {} - Thread: {}", index,
                ruleName, Thread.currentThread().getName());
    }

    public static void printStrategyAnalysis(Logger log, BarSeries series, TradingRecord tradingRecord) {
        /*
         * Analysis criteria
         */
        log.info("-------------------------------------------------------");
        log.info("Strategy Analysis: {} ---------------------", series.getName());
        // Total profit
        ReturnCriterion totalReturn = new ReturnCriterion();
        log.info("Total profit for the strategy: {}", totalReturn.calculate(series, tradingRecord));
        // Number of bars
        log.info("Number of bars: {}", new NumberOfBarsCriterion().calculate(series, tradingRecord));
        // Average profit (per bar)
        log.info(
                "Average return (per bar): {}", new AverageReturnPerBarCriterion().calculate(series, tradingRecord));
        // Number of positions
        log.info("Number of positions: {}", new NumberOfPositionsCriterion().calculate(series, tradingRecord));
        // Profitable position ratio
        log.info("Winning positions ratio: {}",
                new PositionsRatioCriterion(AnalysisCriterion.PositionFilter.PROFIT).calculate(series, tradingRecord));
        // Maximum drawdown
        log.info("Maximum drawdown: {}", new MaximumDrawdownCriterion().calculate(series, tradingRecord));
        // Reward-risk ratio
        log.info("Return over maximum drawdown: {}",
                new ReturnOverMaxDrawdownCriterion().calculate(series, tradingRecord));
        // Total transaction cost
        log.info("Total transaction cost (from $1000): {}",
                new LinearTransactionCostCriterion(1000, 0.002).calculate(series, tradingRecord));
        log.info("Details: ----------------------------------------------");
        tradingRecord.getTrades().forEach(t -> log.info("{}", t.toString()));
        log.info("-------------------------------------------------------");
    }

    static class BarLogging {
        private final MyStrategyBaseBar bar;
        private final int currentIndex;
        private final String barSeriesName;
        BarLogging(MyStrategyBaseBar bar, int currentIndex, String barSeriesName) {
            this.bar = bar;
            this.currentIndex = currentIndex;
            this.barSeriesName = barSeriesName;
        }

        @Override
        public String toString() {
            return "BarSeries: " + barSeriesName +
                    " - Index: " + currentIndex +
                    " - OpenPrice: " + bar.getOpenPrice() +
                    " - HighPrice: " + bar.getHighPrice() +
                    " - LowPrice: " + bar.getLowPrice() +
                    " - ClosePrice: " + bar.getClosePrice() +
                    " - Volume: " + bar.getVolume() +
                    " - TakerBuyVolume: " + bar.getTakerBuyVolume() +
                    " - TakerSellVolume: " + bar.getTakerSellVolume() +
                    " - EventTime: " + bar.getEndTime() +
                    " - IsClosed: " + bar.isClosed();
        }
    }
}
