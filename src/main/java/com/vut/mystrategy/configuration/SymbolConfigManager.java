package com.vut.mystrategy.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.model.SymbolConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SymbolConfigManager {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<SymbolConfig> symbolConfigs;

    @PostConstruct
    public void loadSymbolConfigs() throws IOException {
        // Load file từ resources
        ClassPathResource resource = new ClassPathResource("data/symbols.json");
        try (InputStream inputStream = resource.getInputStream()) {
            // Chuyển JSON thành List<SymbolConfig>
            symbolConfigs = objectMapper.readValue(inputStream, new TypeReference<List<SymbolConfig>>() {});
        }
        System.out.println("Loaded symbol configs: " + symbolConfigs);
    }

    public List<SymbolConfig> gettAllSymbolConfigsList() {
        return symbolConfigs;
    }

    public List<SymbolConfig> getActiveSymbolConfigsList() {
        return symbolConfigs.stream()
                .filter(SymbolConfig::isActive)
                .collect(Collectors.toList());
    }

    public List<SymbolConfig> getActiveSymbolConfigsListByExchangeName(String exchangeName) {
        return symbolConfigs.stream()
                .filter(config -> config.isActive() &&
                        config.getExchangeName().equalsIgnoreCase(exchangeName))
                .collect(Collectors.toList());
    }

    public SymbolConfig getSymbolConfig(String exchangeName, String symbol) {
        return symbolConfigs.stream()
                .filter(config -> config.getSymbol() != null &&
                        config.getSymbol().equalsIgnoreCase(symbol) &&
                        config.getExchangeName().equalsIgnoreCase(exchangeName))
                .findFirst()
                .orElse(null);
    }
}
