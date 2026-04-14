package com.vut.mystrategy.configuration;

import jakarta.annotation.Nonnull;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

public class HttpErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(@Nonnull ClientHttpResponse response) {
        return false; // Luôn false để không throw Exception tự động
    }
}
