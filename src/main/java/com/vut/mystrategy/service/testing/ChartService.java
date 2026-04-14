package com.vut.mystrategy.service.testing;

import com.vut.mystrategy.entity.BackTestKlineData;
import com.vut.mystrategy.helper.Utility;
import com.vut.mystrategy.repository.BackTestKlineDatumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ChartService {

    private final BackTestKlineDatumRepository backtestDatumRepository;

    @Autowired
    public ChartService(BackTestKlineDatumRepository backtestDatumRepository) {
        this.backtestDatumRepository = backtestDatumRepository;
    }

    public List<BackTestKlineData> getBacktestDataList(String symbol, String klineInterval, Instant start, Instant end) {
        return backtestDatumRepository.getPeriodBackTestData(symbol, klineInterval,
                Utility.getEpochMilliByInstant(start), Utility.getEpochMilliByInstant(end));
    }
}
