package com.canineai.backend.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceResolver {

    private final ServiceDiscovery serviceDiscovery;

    /**
     * Translates logic targets to absolute physical path URLs.
     */
    public String resolveUrl(String serviceName, String subPath) {
        String base = serviceDiscovery.discoverService(serviceName);
        String finalUrl = base.endsWith("/") ? base + subPath : base + "/" + subPath;
        log.debug("Resolved integration dispatch path: {} -> {}", serviceName, finalUrl);
        return finalUrl;
    }
}
