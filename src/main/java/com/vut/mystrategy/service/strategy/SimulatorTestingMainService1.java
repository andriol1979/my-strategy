package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.BarSeriesLoader;
import com.vut.mystrategy.helper.ChartBuilderUtility;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.KlineIntervalEnum;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;

import java.util.List;

@Slf4j
public class SimulatorTestingMainService1 {
    public static void main(String[] args) {
        // Giả sử mày có cách lấy danh sách bars từ database
        List<Bar> allBars = BarSeriesLoader.loadListBarFromDatabase(Constant.EXCHANGE_NAME_BINANCE, "btcusdt", KlineIntervalEnum.FIFTEEN_MINUTES);
        allBars = allBars.subList(0, 5000);
        BarSeries series = BarSeriesLoader.buildBarSeries(Constant.EXCHANGE_NAME_BINANCE, "btcusdt", KlineIntervalEnum.FIFTEEN_MINUTES);
        TradingRecord tradingRecord = new BaseTradingRecord();

        boolean isShort = false; // Theo dõi trạng thái vị thế
        // Thêm từng bar để mô phỏng websocket
        for (Bar bar : allBars) {
            series.addBar(bar);
            log.info("Index: {} - Bar: {}", series.getEndIndex(), bar);
            Strategy strategy = DarvasBoxStrategy.buildStrategy(series);
            int endIndex = series.getEndIndex();
            Bar newBar = series.getBar(endIndex);

            log.info("Index: {} - TradingRecord status: {}", endIndex, tradingRecord.isClosed() ? "Closed" : "Open");

/*
            if (!tradingRecord.isClosed()) { // Đã có vị thế mở
                if (isShort && shouldExitShort(series)) {
                    tradingRecord.exit(endIndex, newBar.getClosePrice(), series.numOf(1)); // BUY để đóng short
                    log.info("Close SHORT: BUY at index: {} - Price: {}", endIndex, newBar.getClosePrice());
                    isShort = false;
                } else if (!isShort && strategy.shouldExit(endIndex)) {
                    tradingRecord.exit(endIndex, newBar.getClosePrice(), series.numOf(1)); // SELL để đóng long
                    log.info("Close LONG: SELL at index: {} - Price: {}", endIndex, newBar.getClosePrice());
                }
            }
            else { // Chưa có vị thế
                if (shouldShort(series)) { // Điều kiện bán khống (mày tự định nghĩa)
                    tradingRecord.enter(endIndex, newBar.getClosePrice(), series.numOf(1)); // SELL để mở short
                    log.info("Open SHORT: SELL at index: {} - Price: {}", endIndex, newBar.getClosePrice());
                    isShort = true;
                }
                else if (strategy.shouldEnter(endIndex)) {
                    tradingRecord.enter(endIndex, newBar.getClosePrice(), series.numOf(1)); // BUY để mở long
                    log.info("Open LONG: BUY at index: {} - Price: {}", endIndex, newBar.getClosePrice());
                    isShort = false;
                }
            }
*/





            if (strategy.shouldEnter(endIndex)) {
                tradingRecord.enter(endIndex, series.getBar(endIndex).getClosePrice(), series.numOf(1));
                tradingRecord.getLastTrade()
                log.info("Open LONG: BUY at index: {} - Price: {}", endIndex, newBar.getClosePrice());
            } else if (strategy.shouldExit(endIndex)) {
                tradingRecord.exit(endIndex, series.getBar(endIndex).getClosePrice(), series.numOf(1));
                log.info("Close LONG: SELL at index: {} - Price: {}", endIndex, newBar.getClosePrice());
            }
        }

        // Phân tích kết quả
        LogMessage.printStrategyAnalysis(log, series, tradingRecord);
        ChartBuilderUtility.createCandlestickChart(series,
                Constant.EXCHANGE_NAME_BINANCE, "btcusdt", KlineIntervalEnum.FIFTEEN_MINUTES.getValue());
    }
}
