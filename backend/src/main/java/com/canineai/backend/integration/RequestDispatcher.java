package com.canineai.backend.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestDispatcher {

    private final WebClient aiWebClient;
    private final ServiceResolver resolver;
    private final RetryManager retryManager;
    private final IntegrationMetrics metrics;

    /**
     * Dispatches POST requests asynchronously.
     */
    public <T, R> Mono<R> dispatchPost(String serviceName, String subPath, T body, Class<R> responseType, Map<String, String> headers) {
        String url = resolver.resolveUrl(serviceName, subPath);
        metrics.incrementDispatch(serviceName);

        WebClient.RequestBodySpec requestSpec = aiWebClient.post()
                .uri(url);

        if (headers != null) {
            headers.forEach(requestSpec::header);
        }

        return requestSpec.bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .retryWhen(retryManager.getRetrySpec(serviceName, 2, 2))
                .doOnError(err -> {
                    metrics.incrementError(serviceName);
                    log.error("Failed executing integration request dispatch to {}: {}", serviceName, err.getMessage());
                });
    }

    /**
     * Dispatches GET requests asynchronously.
     */
    public <R> Mono<R> dispatchGet(String serviceName, String subPath, Class<R> responseType, Map<String, String> headers) {
        String url = resolver.resolveUrl(serviceName, subPath);
        metrics.incrementDispatch(serviceName);

        WebClient.RequestHeadersSpec<?> requestSpec = aiWebClient.get()
                .uri(url);

        if (headers != null) {
            headers.forEach(requestSpec::header);
        }

        return requestSpec.retrieve()
                .bodyToMono(responseType)
                .retryWhen(retryManager.getRetrySpec(serviceName, 2, 2))
                .doOnError(err -> {
                    metrics.incrementError(serviceName);
                    log.error("Failed executing integration request dispatch to {}: {}", serviceName, err.getMessage());
                });
    }
}
