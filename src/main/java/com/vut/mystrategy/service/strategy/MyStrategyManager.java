package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.model.binance.KlineEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.num.DecimalNum;

@Slf4j
@Service
public class MyStrategyManager {

    @Async("myStrategyManagerAsync")
    public void runStrategy(BarSeries barSeries, SymbolConfig symbolConfig, KlineEvent klineEvent) {
        //build and run strategy
        if(!klineEvent.getKlineData().getInterval().equalsIgnoreCase(symbolConfig.getEmaKlineInterval())) {
            return;
        }
        // Building the trading strategy - EMACrossOver
        //If you want to change strategy -> just need to replace your strategy here
        //----------------------------------------------------------------------------
        Strategy strategy = EMACrossOverStrategy.buildStrategy(barSeries);

        //----------------------------------------------------------------------------

        // Running the strategy
        BarSeriesManager seriesManager = new BarSeriesManager(barSeries);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        LogMessage.printObjectLogMessage(log, tradingRecord, " BarSeries name: " + barSeries.getName());

        int endIndex = barSeries.getEndIndex(); // Lấy chỉ số của bar cuối cùng
        Bar endBar = barSeries.getBar(endIndex); // lấy Bar của index cuối cùng
        if (strategy.shouldEnter(endIndex)) {
            // Our strategy should enter
            log.info("Strategy should ENTER on {}", endIndex);
            boolean entered = tradingRecord.enter(endIndex, endBar.getClosePrice(), DecimalNum.valueOf(symbolConfig.getOrderVolume()));
            if (entered) {
                Trade entry = tradingRecord.getLastEntry();
                log.info("Entered on {} (price={}, amount={})", entry.getIndex(), entry.getNetPrice().doubleValue(), entry.getAmount().doubleValue());
            }
        }
        else if (strategy.shouldExit(endIndex)) {
            // Our strategy should exit
            log.info("Strategy should EXIT on {}", endIndex);
            boolean exited = tradingRecord.exit(endIndex, endBar.getClosePrice(), DecimalNum.valueOf(symbolConfig.getOrderVolume()));
            if (exited) {
                Trade exit = tradingRecord.getLastExit();
                log.info("Exited on {} (price={}, amount={})", exit.getIndex(), exit.getNetPrice().doubleValue(), exit.getAmount().doubleValue());
            }
        }

        //print strategy
        LogMessage.printStrategyAnalysis(log, barSeries, tradingRecord);
    }
}
