package com.vut.mystrategy.component.binance.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.component.binance.BinanceFutureRestApiClient;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.binance.KlineEvent;
import com.vut.mystrategy.service.KlineEventService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class BinanceWebSocketClient {
    @Value("${binance.websocket.url}")
    private String binanceWebSocketUrl;

    @Value("${connect-websocket}")
    private boolean connectWebsocket;
    @Value("${feed-data-websocket}")
    private boolean feedDataWebSocket;

    private final KlineEventService klineEventService;
    private final SymbolConfigManager symbolConfigManager;
    private final BinanceFutureRestApiClient binanceFutureRestApiClient;
    private final ObjectMapper objectMapper;

    private WebSocketConnectionManager connectionManager;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private final WebSocketClient client = new StandardWebSocketClient();

    @Autowired
    public BinanceWebSocketClient(KlineEventService klineEventService,
                                  SymbolConfigManager symbolConfigManager,
                                  BinanceFutureRestApiClient binanceFutureRestApiClient,
                                  ObjectMapper objectMapper) {
        this.klineEventService = klineEventService;
        this.symbolConfigManager = symbolConfigManager;
        this.binanceFutureRestApiClient = binanceFutureRestApiClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void connectToBinance() {
        if (!connectWebsocket) {
            log.info("Disabled connecting to WebSocket");
            return;
        }
        connect();
        log.info("Connecting to Binance WebSocket...");
    }

    private TextWebSocketHandler initWebSocketHandler(String combinedStream) {
        return new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(@Nonnull WebSocketSession session) throws Exception {
                log.info("Connected to Binance WebSocket");
                if (!feedDataWebSocket) {
                    log.info("Feed data from socket is disabled");
                }
                else {
                    // Send text message to WebSocket to subscribe Kline stream
                    session.sendMessage(new TextMessage(combinedStream));
                    isConnected.set(true);
                    log.info("Subscribed to Binance WebSocket with streams: {}", combinedStream);
                }
            }

            @Override
            protected void handleTextMessage(@Nonnull WebSocketSession session, @Nonnull TextMessage message) {
                String rawMessage = message.getPayload();
                log.info("Received text message: {}", rawMessage);
                try {
                    if (rawMessage.contains("result")) {
                        log.info("Received subscription result: {}", rawMessage);
                        return;
                    }
                    KlineEvent klineEvent = objectMapper.readValue(rawMessage, KlineEvent.class);
                    klineEventService.feedKlineEvent(null,
                            Constant.EXCHANGE_NAME_BINANCE, klineEvent);
                }
                catch (Exception e) {
                    log.error("Error parsing message: {}", e.getMessage());
                }
            }

            @Override
            public void handleTransportError(@Nonnull WebSocketSession session, @Nonnull Throwable exception) {
                log.error("[BinanceWebSocketClient] Transport error: {}", exception.getMessage());
                isConnected.set(false);
                scheduleReconnect();
            }

            @Override
            public void afterConnectionClosed(@Nonnull WebSocketSession session, @Nonnull CloseStatus status) {
                log.info("[BinanceWebSocketClient] Binance WebSocket is closed: {}", status);
                isConnected.set(false);
                scheduleReconnect();
            }
        };
    }

    private void initializeConnection() {
        List<SymbolConfig> symbolConfigs = symbolConfigManager.getActiveSymbolConfigsListByExchangeName(Constant.EXCHANGE_NAME_BINANCE);
        if (symbolConfigs.isEmpty()) {
            log.warn("No active trading configs found for Binance");
            return;
        }
        String combinedStream = buildCombinedSubscriptionJson(symbolConfigs);
        log.info("Subscription json: {}", combinedStream);
        TextWebSocketHandler handler = initWebSocketHandler(combinedStream);
        String listenKey = binanceFutureRestApiClient.receiveListenKey().getListenKey();
        connectionManager = new WebSocketConnectionManager(client, handler, binanceWebSocketUrl + "/" + listenKey);
        connectionManager.setOrigin("BinanceCombinedStream");
        connectionManager.setAutoStartup(false);
    }

    private void connect() {
        if (!isConnected.get() && connectionManager != null) {
            connectionManager.stop();
        }
        initializeConnection();
        connectionManager.start();
    }

    private void scheduleReconnect() {
        if (!isConnected.get()) {
            log.info("Scheduling reconnect to Binance WebSocket...");
            reconnectScheduler.schedule(() -> {
                try {
                    connect();
                    log.info("Reconnecting to Binance WebSocket...");
                }
                catch (Exception e) {
                    log.error("Reconnect failed: {}", e.getMessage());
                    scheduleReconnectWithBackoff(); // Retry với backoff nếu thất bại
                }
            }, 5, TimeUnit.SECONDS);
        }
    }

    private void scheduleReconnectWithBackoff() {
        if (!isConnected.get()) {
            log.info("Scheduling reconnect with backoff to Binance WebSocket...");
            reconnectScheduler.schedule(() -> {
                try {
                    connect();
                    log.info("Reconnecting to Binance WebSocket with backoff...");
                }
                catch (Exception e) {
                    log.error("Reconnect failed again: {}", e.getMessage());
                    scheduleReconnectWithBackoff(); // Tiếp tục retry
                }
            }, 10, TimeUnit.SECONDS); // Tăng thời gian chờ lên 10 giây
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (connectionManager != null) {
                connectionManager.stop();
                reconnectScheduler.shutdown();
                log.info("WebSocket client stopped");
            }
        } catch (Exception e) {
            log.error("Error stopping WebSocket client: {}", e.getMessage());
        }
    }

    private String buildCombinedSubscriptionJson(List<SymbolConfig> configs) {
        Set<String> socketParams = new HashSet<>();
        for (SymbolConfig config : configs) {
            for (String klineInterval : config.getFeedKlineIntervals()) {
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

        return String.format(jsonStr, combinedParams);
    }
}