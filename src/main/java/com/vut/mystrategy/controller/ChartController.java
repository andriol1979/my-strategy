package com.vut.mystrategy.controller;

import com.vut.mystrategy.entity.BackTestKlineData;
import com.vut.mystrategy.entity.Order;
import com.vut.mystrategy.helper.ApiUrlConstant;
import com.vut.mystrategy.model.BacktestEquityPoint;
import com.vut.mystrategy.model.BacktestSummaryResponse;
import com.vut.mystrategy.service.testing.BacktestReportService;
import com.vut.mystrategy.service.testing.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Controller
@Validated
public class ChartController {

    private final ChartService chartService;
    private final BacktestReportService backtestReportService;

    public ChartController(ChartService chartService, BacktestReportService backtestReportService) {
        this.chartService = chartService;
        this.backtestReportService = backtestReportService;
    }

    @GetMapping("/chart")
    public String showChartPage() {
        return "chart"; // Render template chart.html
    }

    @GetMapping(ApiUrlConstant.TESTING_URL + "/chart")
    @ResponseBody
    public List<BackTestKlineData> getChartData(
            @RequestParam(value = "symbol", defaultValue = "bnbusdt") String symbol,
            @RequestParam(value = "klineInterval", defaultValue = "5m") String klineInterval,
            @RequestParam(value = "startDate", defaultValue = "2025-01-01") String startDate,
            @RequestParam(value = "endDate", defaultValue = "2025-01-15") String endDate) {

        Instant start = LocalDate.parse(startDate).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = LocalDate.parse(endDate).atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant();
        return chartService.getBacktestDataList(symbol, klineInterval, start, end);
    }

    @GetMapping(ApiUrlConstant.TESTING_URL + "/orders")
    @ResponseBody
    public List<Order> getBacktestOrders(
            @RequestParam(value = "exchangeName", defaultValue = "binance") String exchangeName,
            @RequestParam(value = "symbol", defaultValue = "bnbusdt") String symbol,
            @RequestParam(value = "klineInterval", defaultValue = "5m") String klineInterval) {
        return backtestReportService.getOrders(exchangeName, symbol, klineInterval);
    }

    @GetMapping(ApiUrlConstant.TESTING_URL + "/orders/summary")
    @ResponseBody
    public BacktestSummaryResponse getBacktestOrdersSummary(
            @RequestParam(value = "exchangeName", defaultValue = "binance") String exchangeName,
            @RequestParam(value = "symbol", defaultValue = "bnbusdt") String symbol,
            @RequestParam(value = "klineInterval", defaultValue = "5m") String klineInterval) {
        return backtestReportService.getSummary(exchangeName, symbol, klineInterval);
    }

    @GetMapping(ApiUrlConstant.TESTING_URL + "/orders/equity-curve")
    @ResponseBody
    public List<BacktestEquityPoint> getBacktestEquityCurve(
            @RequestParam(value = "exchangeName", defaultValue = "binance") String exchangeName,
            @RequestParam(value = "symbol", defaultValue = "bnbusdt") String symbol,
            @RequestParam(value = "klineInterval", defaultValue = "5m") String klineInterval) {
        return backtestReportService.getEquityCurve(exchangeName, symbol, klineInterval);
    }
}
