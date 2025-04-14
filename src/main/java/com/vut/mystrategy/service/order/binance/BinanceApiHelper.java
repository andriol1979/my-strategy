package com.vut.mystrategy.service.order.binance;

import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class BinanceApiHelper {
    public String sign(String query, String secretKey) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secretKeySpec);
            byte[] hash = sha256_HMAC.doFinal(query.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }

    public String buildQueryString(MultiValueMap<String, String> params) {
        return params.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(v -> entry.getKey() + "=" + v))
                .collect(Collectors.joining("&"));
    }
}
