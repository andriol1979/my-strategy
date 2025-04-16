package com.vut.mystrategy.configuration;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class RetryableRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private final RetryTemplate retryTemplate;

    public RetryableRestTemplateInterceptor() {
        this.retryTemplate = createRetryTemplate();
    }

    @Override
    @Nonnull
    public ClientHttpResponse intercept(@Nonnull HttpRequest request, @Nonnull byte[] body,
                                        @Nonnull ClientHttpRequestExecution execution) throws IOException {
        return retryTemplate.execute(context -> {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                if (shouldRetry(response)) {
                    throw new RetryableException("Retryable response from Binance");
                }
                return response;
            } catch (IOException ex) {
                log.warn("IOException on retryable request: {}", ex.getMessage());
                throw ex;
            }
        });
    }

    private boolean shouldRetry(ClientHttpResponse response) throws IOException {
        int statusCode = response.getStatusCode().value();
        if (statusCode >= 500) return true; // Retry 5xx

        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.contains("\"code\":-1021") || body.contains("\"code\":-2010")) {
            log.warn("Binance API retryable error: {}", body);
            return true;
        }

        return false;
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        SimpleRetryPolicy policy = new SimpleRetryPolicy(3, Map.of(
                RetryableException.class, true,
                IOException.class, true
        ));

        FixedBackOffPolicy backoff = new FixedBackOffPolicy();
        backoff.setBackOffPeriod(2000);

        template.setRetryPolicy(policy);
        template.setBackOffPolicy(backoff);
        return template;
    }

    static class RetryableException extends RuntimeException {
        public RetryableException(String message) {
            super(message);
        }
    }
}
