package com.canineai.backend.config.ai;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class AiGatewayConfig {

    @Value("${canineai.ai.gateway.api-key:canineaiInternalGatewayApiKeyPayload}")
    private String gatewayApiKey;

    @Bean
    public WebClient aiWebClient(WebClient.Builder builder) {
        log.info("Initializing non-blocking Spring WebClient pipeline for Colab ToothSeg communication (900s timeout, 128MB buffer)...");

        // Connection provider for ToothSeg async inference requests
        ConnectionProvider provider = ConnectionProvider.builder("ai-gateway-pool")
                .maxConnections(100)
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .maxIdleTime(Duration.ofSeconds(120))
                .build();

        // Allow up to 900 seconds (15 minutes) for large NIfTI CBCT uploads and 3D UNet ToothSeg inference
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 30s connect timeout
                .responseTimeout(Duration.ofSeconds(900)) // 900s (15 min) response timeout for Colab inference
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(900, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(900, TimeUnit.SECONDS)));

        // Configure 128 MB max in-memory buffer size for base64 slice images and JSON payloads
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(128 * 1024 * 1024))
                .build();

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader("X-Internal-Gateway-Key", gatewayApiKey)
                .filter((request, next) -> {
                    String correlationId = org.slf4j.MDC.get("correlationId");
                    if (correlationId == null) {
                        correlationId = java.util.UUID.randomUUID().toString();
                    }
                    org.springframework.web.reactive.function.client.ClientRequest filtered = 
                            org.springframework.web.reactive.function.client.ClientRequest.from(request)
                                    .header("X-Correlation-ID", correlationId)
                                    .build();
                    log.info("Outgoing AI Request: {} {}", filtered.method(), filtered.url());
                    return next.exchange(filtered).doOnNext(response -> {
                        log.info("Completed AI Request: {} {} - Status: {}", filtered.method(), filtered.url(), response.statusCode());
                    }).doOnError(ex -> {
                        log.error("AI Request Failed: {} {} - Error: {}", filtered.method(), filtered.url(), ex.getMessage(), ex);
                    });
                })
                .build();
    }
}
