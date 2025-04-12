package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.KlineIntervalEnum;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.backtest.BarSeriesManager;

import static com.vut.mystrategy.service.strategy.EMACrossOverStrategy.buildStrategy;


@Slf4j
public class BackTestingMainService {

    public static void main(String[] args) {
        BarSeries series = BarSeriesLoader.loadFromDatabase(Constant.EXCHANGE_NAME_BINANCE, "btcusdt", KlineIntervalEnum.FIFTEEN_MINUTES);
        /* Replace strategy you want to test here */
        Strategy strategy = buildStrategy(series);
        /* --------------------------------------------------*/

        BarSeriesManager seriesManager = new BarSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        //print strategy
        LogMessage.printStrategyAnalysis(log, series, tradingRecord);
        //Export chart image
//        ChartBuilderUtility.createCandlestickChart(series, Constant.EXCHANGE_NAME_BINANCE, "linkusdt", KlineIntervalEnum.FIVE_MINUTES.getValue());
    }
}
