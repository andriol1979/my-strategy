package com.vut.mystrategy.helper;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.vut.mystrategy.entity.BacktestDatum;
import com.vut.mystrategy.model.BarDuration;
import com.vut.mystrategy.model.KlineIntervalEnum;
import com.vut.mystrategy.model.binance.KlineEvent;
import com.vut.mystrategy.repository.BacktestDatumRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class BarSeriesLoader {

    public static BarSeries loadFromKlineEvents(List<KlineEvent> klineEvents) {
        BarSeries series = new BaseBarSeriesBuilder().build();
        klineEvents.forEach(klineEvent -> {
            KlineIntervalEnum klineEnum = KlineIntervalEnum.fromValue(klineEvent.getKlineData().getInterval());
            Bar bar = BaseBar.builder()
                    .openPrice(DecimalNum.valueOf(klineEvent.getKlineData().getOpenPrice()))
                    .closePrice(DecimalNum.valueOf(klineEvent.getKlineData().getClosePrice()))
                    .highPrice(DecimalNum.valueOf(klineEvent.getKlineData().getHighPrice()))
                    .lowPrice(DecimalNum.valueOf(klineEvent.getKlineData().getLowPrice()))
                    .endTime(Utility.getZonedDateTimeByEpochMilli(klineEvent.getEventTime()))
                    .timePeriod(new BarDuration(klineEnum).getDuration())
                    .volume(DecimalNum.valueOf(klineEvent.getKlineData().getQuoteVolume()))
                    .build();
            series.addBar(bar);
        });

        return series;
    }

    @SneakyThrows
    public static BarSeries loadFromCsv(String csvFileName) {
        var stream = BarSeriesLoader.class.getClassLoader().getResourceAsStream(csvFileName);
        var series = new BaseBarSeriesBuilder().withName("back_test_bars").build();

        try {
            assert stream != null;
            try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                    .withSkipLines(1)
                    .build()) {
                String[] line;
                List<Bar> bars = new ArrayList<>();
                while ((line = csvReader.readNext()) != null) {
                    log.info("Line 0: {}", line[0]);
                    Bar bar = BaseBar.builder()
                            .openPrice(DecimalNum.valueOf(line[3]))
                            .closePrice(DecimalNum.valueOf(line[6]))
                            .highPrice(DecimalNum.valueOf(line[4]))
                            .lowPrice(DecimalNum.valueOf(line[5]))
                            .endTime(Utility.getZonedDateTimeByEpochMilli(Long.parseLong(line[0])))
                            .timePeriod(new BarDuration(KlineIntervalEnum.ONE_HOUR).getDuration())
                            .volume(DecimalNum.valueOf(line[7]))
                            .amount(DecimalNum.valueOf(8))
                            .build();
                    bars.add(bar);
                }
                bars.sort(Comparator.comparing(Bar::getEndTime));
                bars.forEach(series::addBar);
            }
            catch (CsvValidationException e) {
                log.error("Unable to load bars from CSV. File is not valid csv.", e);
            }
        }
        catch (IOException ioe) {
            log.error("Unable to load bars from CSV", ioe);
        }
        catch (NumberFormatException nfe) {
            log.error("Error while parsing value", nfe);
        }
        return series;
    }

    public static BarSeries loadFromDatabase(String exchangeName, String symbol, KlineIntervalEnum klineEnum) {
        // Tạo EntityManagerFactory từ persistence unit
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("my-persistence-unit");
        EntityManager em = emf.createEntityManager();

        // Tạo JpaRepository từ EntityManager
        RepositoryFactorySupport factory = new JpaRepositoryFactory(em);
        BacktestDatumRepository repository = factory.getRepository(BacktestDatumRepository.class);

        // Dùng JpaRepository
        Sort sort = Sort.by(Sort.Direction.ASC, "eventTime");
        List<BacktestDatum> backtestData = repository.findByExchangeNameAndSymbolAndKlineInterval(exchangeName, symbol, klineEnum.getValue(), sort);
        log.info("Total loaded {} BacktestDatum from database", backtestData.size());
        //load bar series
        BarSeries series = new BaseBarSeriesBuilder().build();
        backtestData.forEach(datum -> {
            Bar bar = BaseBar.builder()
                    .openPrice(DecimalNum.valueOf(datum.getOpen()))
                    .closePrice(DecimalNum.valueOf(datum.getClose()))
                    .highPrice(DecimalNum.valueOf(datum.getHigh()))
                    .lowPrice(DecimalNum.valueOf(datum.getLow()))
                    .endTime(Utility.getZonedDateTimeByInstant(datum.getEventTime()))
                    .timePeriod(new BarDuration(klineEnum).getDuration())
                    .volume(DecimalNum.valueOf(datum.getVolume()))
                    .build();
            series.addBar(bar);
        });

        // Đóng tài nguyên
        em.close();
        emf.close();
        log.info("Total loaded {} Bar(s) to BarSeries", series.getBarCount());
        return series;
    }
}
