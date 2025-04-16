package com.vut.mystrategy.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ta4j.core.Position;

import java.io.IOException;

@Slf4j
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
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
        log.info("Jackson ObjectMapper configured");
        return objectMapper;
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
