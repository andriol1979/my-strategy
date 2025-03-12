package com.vut.mystrategy.configuration.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.configuration.AsyncModuleManager;
import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.binance.TradeEvent;
import com.vut.mystrategy.service.TradeEventService;
import com.vut.mystrategy.service.TradingConfigManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class BinanceWebSocketClient {

    @Value("${binance.websocket.url}")
    private String binanceWebSocketUrl;
    @Value("${binance.websocket.delay-time}")
    private int binanceWebSocketDelayTime;

    private final AsyncModuleManager asyncModuleManager;
    private final TradeEventService tradeEventService;
    private final TradingConfigManager tradingConfigManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<TradeEvent> latestTradeEvent = new AtomicReference<>();
    private ScheduledExecutorService scheduler;
    private WebSocketConnectionManager connectionManager;

    @Autowired
    public BinanceWebSocketClient(AsyncModuleManager asyncModuleManager,
            TradeEventService tradeEventService, TradingConfigManager tradingConfigManager) {
        this.asyncModuleManager = asyncModuleManager;
        this.tradeEventService = tradeEventService;
        this.tradingConfigManager = tradingConfigManager;
    }

    @PostConstruct
    public void connectToBinance() {
        List<TradingConfig> tradingConfigs = tradingConfigManager.getActiveConfigs(Constant.EXCHANGE_NAME_BINANCE);
        if (tradingConfigs.isEmpty()) {
            log.warn("No active trading configs found for Binance");
            return;
        }

        // Tạo combined stream cho tất cả symbol
        String combinedStream = buildCombinedSubscriptionJson(tradingConfigs);

        WebSocketClient client = new StandardWebSocketClient();
        TextWebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                session.sendMessage(new TextMessage(combinedStream));
                log.info("Connected to Binance WebSocket with streams: {}", combinedStream);
                asyncModuleManager.markWebSocketReady();

                // Start scheduler
                scheduler = Executors.newSingleThreadScheduledExecutor();
                //trigger tradeEventService.saveTradeEvent
                scheduler.scheduleAtFixedRate(() -> processLatestTradeEvent(), 0,
                        binanceWebSocketDelayTime, TimeUnit.MILLISECONDS);
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                String rawMessage = message.getPayload();
                try {
                    if (rawMessage.contains("result")) {
                        log.info("Received subscription result: {}", rawMessage);
                        return;
                    }
                    TradeEvent tradeEvent = mapper.readValue(rawMessage, TradeEvent.class);
                    latestTradeEvent.set(tradeEvent); // Cập nhật TradeEvent mới nhất
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
                asyncModuleManager.markWebSocketDisconnected();
                if (scheduler != null) {
                    scheduler.shutdown();
                }
            }
        };

        // Sử dụng 1 connection manager duy nhất
        connectionManager = new WebSocketConnectionManager(client, handler, binanceWebSocketUrl);
        connectionManager.setOrigin("BinanceCombinedStream");
        connectionManager.setAutoStartup(true);
        connectionManager.start();
    }

    @SneakyThrows
    private void processLatestTradeEvent() {
        TradeEvent event = latestTradeEvent.get();
        if (event != null) {
            tradeEventService.saveTradeEvent(Constant.EXCHANGE_NAME_BINANCE, event.getSymbol(), event);
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (connectionManager != null) {
                connectionManager.stop();
                log.info("WebSocket client stopped");
            }
            if (scheduler != null) {
                scheduler.shutdown();
                log.info("Scheduler stopped");
            }
        } catch (Exception e) {
            log.error("Error stopping WebSocket client: {}", e.getMessage());
        }
    }

    private String buildCombinedSubscriptionJson(List<TradingConfig> configs) {
        StringBuilder params = new StringBuilder();
        for (TradingConfig config : configs) {
            if (!params.isEmpty()) params.append(",");
            params.append("\"").append(config.getSymbol().toLowerCase())
                    .append(Constant.TRADE_STREAM_NAME).append("\"");
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
