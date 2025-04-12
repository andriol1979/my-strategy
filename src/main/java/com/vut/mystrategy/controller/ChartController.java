package com.vut.mystrategy.controller;

import com.vut.mystrategy.entity.BacktestDatum;
import com.vut.mystrategy.helper.ApiUrlConstant;
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

    public ChartController(ChartService chartService) {
        this.chartService = chartService;
    }

    @GetMapping("/chart")
    public String showChartPage() {
        return "chart"; // Render template chart.html
    }

    @GetMapping(ApiUrlConstant.TESTING_URL + "/chart")
    @ResponseBody
    public List<BacktestDatum> getChartData(
            @RequestParam(value = "startDate", defaultValue = "2023-03-10") String startDate,
            @RequestParam(value = "endDate", defaultValue = "2023-03-12") String endDate) {

        Instant start = LocalDate.parse(startDate).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = LocalDate.parse(endDate).atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant();
        return chartService.getBacktestDataList(start, end);
    }
}
