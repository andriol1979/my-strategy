package com.vut.mystrategy.helper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.ta4j.core.BarSeries;

import java.io.File;
import java.util.Date;

@Slf4j
public class ChartBuilderUtility {

    private static final String backTestChartFolder = "/Users/vutran/IdeaProjects/my-strategy/backtest-charts/";

    @SneakyThrows
    public static void createCandlestickChart(BarSeries series,
                                       String exchangeName, String symbol, String klineInterval) {
        String chartName = exchangeName + "-" + symbol + "-" + klineInterval;

        // Chuẩn bị dữ liệu cho JFreeChart
        int barCount = series.getBarCount();
        Date[] dates = new Date[barCount];
        double[] high = new double[barCount];
        double[] low = new double[barCount];
        double[] open = new double[barCount];
        double[] close = new double[barCount];
        double[] volume = new double[barCount];

        for (int i = 0; i < barCount; i++) {
            dates[i] = Date.from(series.getBar(i).getEndTime().toInstant());
            high[i] = series.getBar(i).getHighPrice().doubleValue();
            low[i] = series.getBar(i).getLowPrice().doubleValue();
            open[i] = series.getBar(i).getOpenPrice().doubleValue();
            close[i] = series.getBar(i).getClosePrice().doubleValue();
            volume[i] = series.getBar(i).getVolume().doubleValue();
        }

        DefaultHighLowDataset dataset = new DefaultHighLowDataset(
                "binance-btcusd-5m", dates, high, low, open, close, volume);

        JFreeChart chart = ChartFactory.createCandlestickChart(
                "Candlestick Chart", "Time", "Price", dataset, true);

        // Tùy chỉnh font và giao diện
        XYPlot plot = chart.getXYPlot();
        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        domainAxis.setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        rangeAxis.setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        chart.getTitle().setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));

        // Lưu biểu đồ với kích thước mới
        ChartUtils.saveChartAsPNG(new File(backTestChartFolder + chartName + ".png"), chart, 1920, 1080);
    }
}
