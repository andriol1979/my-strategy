package com.vut.mystrategy.configuration.feeddata.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.binance.KlineEvent;
import com.vut.mystrategy.service.KlineEventService;
import com.vut.mystrategy.configuration.SymbolConfigManager;
import com.vut.mystrategy.service.strategy.EMACrossOverStrategy;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class BinanceWebSocketClient {

    @Value("${binance.websocket.url}")
    private String binanceWebSocketUrl;

    @Value("${feed-data-from-socket}")
    private boolean feedDataFromSocket;

    @Value("${warm-up-bar-size}")
    private int warmUpBarSize;

    private final KlineEventService klineEventService;
    private final SymbolConfigManager symbolConfigManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private WebSocketConnectionManager connectionManager;

    @Autowired
    public BinanceWebSocketClient(KlineEventService klineEventService,
                                  SymbolConfigManager symbolConfigManager) {
        this.klineEventService = klineEventService;
        this.symbolConfigManager = symbolConfigManager;
    }

    @PostConstruct
    public void connectToBinance() {
        if(!feedDataFromSocket) {
            log.info("Feed data from socket is disabled");
            return;
        }
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
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                String rawMessage = message.getPayload();
                try {
                    if (rawMessage.contains("result")) {
                        log.info("Received subscription result: {}", rawMessage);
                        log.info("Start receiving KlineEvent data. Warm up time in: {} kline events before running strategy", warmUpBarSize);
                        return;
                    }
                    KlineEvent klineEvent = mapper.readValue(rawMessage, KlineEvent.class);
                    klineEventService.feedKlineEvent(EMACrossOverStrategy.class.getSimpleName(),
                            Constant.EXCHANGE_NAME_BINANCE, klineEvent);
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
        Set<String> socketParams = new HashSet<>();
        for (SymbolConfig config : configs) {
            for(String klineInterval : config.getFeedKlineIntervals()) {
                String param = "\"" + config.getSymbol().toLowerCase() + Constant.KLINE_STREAM_NAME + klineInterval + "\"";
                socketParams.add(param);
            }
        }
        String combinedParams = String.join(",", socketParams);
        String jsonStr = """
                {
                  "method": "SUBSCRIBE",
                  "params": [%s],
                  "id": 1
                }
                """;
        String subscriptionJson = String.format(jsonStr, combinedParams);
        log.info("Subscription json: {}", subscriptionJson);
        return subscriptionJson;
    }
}
