package com.vut.mystrategy.configuration.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.binance.KlineEvent;
import com.vut.mystrategy.service.RedisClientService;
import com.vut.mystrategy.service.KlineEventService;
import com.vut.mystrategy.configuration.SymbolConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

@Slf4j
@Component
public class BinanceWebSocketClient {

    @Value("${binance.websocket.url}")
    private String binanceWebSocketUrl;

    private final KlineEventService klineEventService;
    private final SymbolConfigManager symbolConfigManager;
    private final RedisClientService redisClientService;
    private final ObjectMapper mapper = new ObjectMapper();
    private WebSocketConnectionManager connectionManager;

    @Autowired
    public BinanceWebSocketClient(RedisClientService redisClientService,
                                  KlineEventService klineEventService, SymbolConfigManager symbolConfigManager) {
        this.klineEventService = klineEventService;
        this.symbolConfigManager = symbolConfigManager;
        this.redisClientService = redisClientService;
    }

    @PostConstruct
    public void connectToBinance() {
        List<SymbolConfig> symbolConfigs = symbolConfigManager.getActiveSymbolConfigsListByExchangeName(Constant.EXCHANGE_NAME_BINANCE);
        if (symbolConfigs.isEmpty()) {
            log.warn("No active trading configs found for Binance");
            return;
        }

        // Tạo combined stream cho tất cả symbol
        String combinedStream = buildCombinedSubscriptionJson(symbolConfigs);

        WebSocketClient client = new StandardWebSocketClient();
        TextWebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                session.sendMessage(new TextMessage(combinedStream));
                log.info("Connected to Binance WebSocket with streams: {}", combinedStream);

                //reset counter
                redisClientService.resetCounter(symbolConfigs);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                String rawMessage = message.getPayload();
                try {
                    if (rawMessage.contains("result")) {
                        log.info("Received subscription result: {}", rawMessage);
                        return;
                    }
                    KlineEvent klineEvent = mapper.readValue(rawMessage, KlineEvent.class);
                    klineEventService.saveKlineEvent(Constant.EXCHANGE_NAME_BINANCE, klineEvent.getSymbol(), klineEvent);
                } catch (Exception e) {
                    log.error("Error parsing message: {}", e.getMessage());
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                log.error("[BinanceWebSocketClient]: {}", exception.getMessage());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
                log.info("[BinanceWebSocketClient] Binance WebSocket is closed: {}", status);
            }
        };

        // Sử dụng 1 connection manager duy nhất
        connectionManager = new WebSocketConnectionManager(client, handler, binanceWebSocketUrl);
        connectionManager.setOrigin("BinanceCombinedStream");
        connectionManager.setAutoStartup(true);
        connectionManager.start();
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (connectionManager != null) {
                connectionManager.stop();
                log.info("WebSocket client stopped");
            }
        }
        catch (Exception e) {
            log.error("Error stopping WebSocket client: {}", e.getMessage());
        }
    }

    private String buildCombinedSubscriptionJson(List<SymbolConfig> configs) {
        StringBuilder params = new StringBuilder();
        for (SymbolConfig config : configs) {
            if (!params.isEmpty()) {
                params.append(",");
            }
            for(String klineInterval : config.getFeedKlineIntervals()) {
                if(params.indexOf(Constant.KLINE_STREAM_NAME) >= 0) {
                    params.append(",");
                }
                params.append("\"").append(config.getSymbol().toLowerCase())
                        .append(Constant.KLINE_STREAM_NAME).append(klineInterval).append("\"");
            }
        }
        String jsonStr = """
                {
                  "method": "SUBSCRIBE",
                  "params": [%s],
                  "id": 1
                }
                """;
        String subscriptionJson = String.format(jsonStr, params);
        log.info("Subscription json: {}", subscriptionJson);
        return subscriptionJson;
    }
}
