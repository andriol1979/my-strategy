package com.vut.mystrategy.component.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vut.mystrategy.component.RestApiHelper;
import com.vut.mystrategy.model.binance.BinanceOrderRequest;
import com.vut.mystrategy.model.binance.BinanceOrderResponse;
import com.vut.mystrategy.model.binance.ListenKeyResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Slf4j
@Component
public class BinanceFutureRestApiClient {

    @Value("${binance.api.key}")
    private String apiKey;
    @Value("${binance.api.secret}")
    private String secretKey;
    @Value("${binance.api.url}")
    private String baseUrl;

    @Value("${place-order-via-api}")
    private boolean placeOrderViaAPI;

    private final String apiVersion = "/fapi/v1";
    private final String orderEndPoint = apiVersion + "/order";
    private final String headerAPIKey = "X-MBX-APIKEY";

    private final RestApiHelper restApiHelper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public BinanceFutureRestApiClient(RestApiHelper restApiHelper, RestTemplate restTemplate,
                                      ObjectMapper objectMapper) {
        this.restApiHelper = restApiHelper;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    public ListenKeyResponse receiveListenKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(headerAPIKey, apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String uri = baseUrl + apiVersion + "/listenKey";

        ResponseEntity<String> response = restTemplate.exchange(
                uri, HttpMethod.POST, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return objectMapper.readValue(response.getBody(), ListenKeyResponse.class);
        }
        log.warn("❌ ReceiveListenKey from Binance failed: {}", response.getBody());
        return null;
    }

    @SneakyThrows
    public BinanceOrderResponse placeOrder(BinanceOrderRequest request) {
        if(!placeOrderViaAPI) {
            log.info("Place order via API is disabled. Should use dev profile to fake BinanceOrderResponse");
            return null;
        }
        //Build param + generate sign from secret and query string
        MultiValueMap<String, String> params = getStringStringMultiValueMap(request);
        String queryString = restApiHelper.buildQueryString(params);
        String signature = restApiHelper.sign(queryString, secretKey);
        String uri = baseUrl + orderEndPoint + "?signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set(headerAPIKey, apiKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                uri, HttpMethod.POST, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return objectMapper.readValue(response.getBody(), BinanceOrderResponse.class);
        }
        log.warn("❌ PlaceOrder from Binance failed: {}", response.getBody());
        return null;
    }

    private static MultiValueMap<String, String> getStringStringMultiValueMap(BinanceOrderRequest request) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("symbol", request.getSymbol().toUpperCase());
        params.add("side", request.getSide().toUpperCase());
        params.add("type", request.getType().toUpperCase());
        params.add("quantity", request.getQuantity());
        params.add("positionSide", request.getPositionSide());
        params.add("timestamp", String.valueOf(System.currentTimeMillis()));
        params.add("newClientOrderId", request.getNewClientOrderId());
        return params;
    }
}
