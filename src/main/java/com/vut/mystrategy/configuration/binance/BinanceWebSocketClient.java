package com.vut.mystrategy.configuration.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.entity.TradingConfig;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.model.binance.TradeEvent;
import com.vut.mystrategy.service.RedisClientService;
import com.vut.mystrategy.service.TradingConfigManager;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class BinanceWebSocketClient {

    @Value("${binance.websocket.url}")
    private String binanceWebSocketUrl;

    private final RedisClientService redisClientService;
    private final TradingConfigManager tradingConfigManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private WebSocketSession session;
    private final List<WebSocketConnectionManager> managers = new ArrayList<>();

    @Autowired
    public BinanceWebSocketClient(RedisClientService redisClientService,
                                  TradingConfigManager tradingConfigManager) {
        this.redisClientService = redisClientService;
        this.tradingConfigManager = tradingConfigManager;
    }

    private void addConnection(String symbol, int delayMillisecond) {
        WebSocketClient client = new StandardWebSocketClient();
        TextWebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                BinanceWebSocketClient.this.session = session;
                String subscriptionJson = buildSubscriptionJson(symbol);
                session.sendMessage(new TextMessage(subscriptionJson));
                log.info("Connected to binance websocket with symbol [{}] and delay in [{}]", symbol, delayMillisecond);
            }
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                String rawMessage = message.getPayload();
                try {
                    if(rawMessage.contains("result")) {
                        log.info("Received subscription result: {}", rawMessage);
                        return;
                    }
                    TradeEvent tradeEvent = mapper.readValue(rawMessage, TradeEvent.class);
                    redisClientService.saveTradeEvent(Constant.EXCHANGE_NAME_BINANCE, tradeEvent.getSymbol(), tradeEvent);
                    Thread.sleep(delayMillisecond);
                }
                catch (Exception e) {
                    log.error(e.getMessage());
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

        // Sử dụng WebSocketConnectionManager
        WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(
                client,
                handler,
                binanceWebSocketUrl
        );
        connectionManager.setOrigin(symbol);
        connectionManager.setAutoStartup(true); // Tự động kết nối khi ứng dụng khởi động
        connectionManager.start();
        managers.add(connectionManager);
    }

    @PostConstruct
    public void connectToBinance() {
        List<TradingConfig> tradingConfigs = tradingConfigManager.getActiveConfigs(Constant.EXCHANGE_NAME_BINANCE);
        tradingConfigs.forEach(tradingConfig -> {
            addConnection(tradingConfig.getSymbol(), tradingConfig.getDelayMillisecond());
        });
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
            managers.forEach(connectionManager -> {
                if (connectionManager != null) {
                    connectionManager.stop();
                    log.info("WebSocket client {} stopped", connectionManager.getOrigin());
                }
            });
        }
        catch (Exception e) {
            log.error("Error stopping WebSocket client: {}", e.getMessage());
        }
    }

    private String buildSubscriptionJson(String symbol) {
        String jsonStr = """
                        {
                          "method": "SUBSCRIBE",
                          "params": [
                            "%s"
                          ],
                          "id": 1
                        }
                        """;
        String subscriptionJson = String.format(jsonStr, symbol + Constant.STREAM_NAME);
        log.info("Subscription json: {}", subscriptionJson);
        return subscriptionJson;
    }
}
