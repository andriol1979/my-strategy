package com.vut.mystrategy.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.ta4j.core.Position;

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
    public static void printObjectLogMessage(Logger log, Object object) {
        log.info("Debugging data: Type: {} - Value: {} - Thread: {}",
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
}
