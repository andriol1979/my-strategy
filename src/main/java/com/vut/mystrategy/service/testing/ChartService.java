package com.vut.mystrategy.service.testing;

import com.vut.mystrategy.entity.BacktestDatum;
import com.vut.mystrategy.repository.BacktestDatumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChartService {

    private final BacktestDatumRepository backtestDatumRepository;

    @Autowired
    public ChartService(BacktestDatumRepository backtestDatumRepository) {
        this.backtestDatumRepository = backtestDatumRepository;
    }

    public List<BacktestDatum> getBacktestDataList(Instant start, Instant end) {
        List<BacktestDatum> backTestData = backtestDatumRepository.getPeriodBackTestData(
                "btcusdt", "15m", start, end);
        return backTestData;
    }
}
