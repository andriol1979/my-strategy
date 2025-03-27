package com.vut.mystrategy.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.criteria.*;
import org.ta4j.core.criteria.pnl.ReturnCriterion;

import java.io.IOException;

public class LogMessage {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Bỏ qua các thuộc tính null khi serialize
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Tùy chọn: Bỏ qua lỗi khi serialize các bean rỗng
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        SimpleModule module = new SimpleModule();
        module.addSerializer(Position.class, new PositionSerializer());
        objectMapper.registerModule(module);
        objectMapper.registerModule(new JavaTimeModule());
        // Tùy chọn: Tắt tính năng ghi thời gian dưới dạng timestamp (nếu cần)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @SneakyThrows
    public static void printInsertRedisLogMessage(Logger log, String redisKey, Object object) {
        log.info("Inserted {} to Redis. Key: {} - Value: {} - Thread: {}",
                object.getClass().getSimpleName(), redisKey,
                objectMapper.writeValueAsString(object), Thread.currentThread().getName());
    }

    @SneakyThrows
    public static void printUpdateRedisLogMessage(Logger log, String redisKey, Object object) {
        log.info("Updated {} to Redis. Key: {} - Value: {} - Thread: {}",
                object.getClass().getSimpleName(), redisKey,
                objectMapper.writeValueAsString(object), Thread.currentThread().getName());
    }

    @SneakyThrows
    public static void printObjectLogMessage(Logger log, Object object, String customMessage) {
        log.info("Debugging data: Type: {} - Value: {} - Thread: {}." + customMessage,
                object.getClass().getSimpleName(),
                objectMapper.writeValueAsString(object), Thread.currentThread().getName());
    }

    static class PositionSerializer extends StdSerializer<Position> {
        public PositionSerializer() {
            this(null);
        }

        public PositionSerializer(Class<Position> t) {
            super(t);
        }

        @Override
        public void serialize(Position position, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("entry", position.getEntry());
            jsonGenerator.writeObjectField("exit", position.getExit());
            if (position.getExit() != null) {
                jsonGenerator.writeNumberField("grossProfit", position.getGrossProfit().doubleValue());
            }
            jsonGenerator.writeEndObject();
        }
    }

    public static void printStrategyAnalysis(Logger log, BarSeries series, TradingRecord tradingRecord) {
        /*
         * Analysis criteria
         */

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
        // Buy-and-hold
        log.info("Buy-and-hold return: {}",
                new EnterAndHoldCriterion(new ReturnCriterion()).calculate(series, tradingRecord));
        // Total profit vs buy-and-hold
        log.info("Custom strategy return vs buy-and-hold strategy return: {}",
                new VersusEnterAndHoldCriterion(totalReturn).calculate(series, tradingRecord));
    }
}
