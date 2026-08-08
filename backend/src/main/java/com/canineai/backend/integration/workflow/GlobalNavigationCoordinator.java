package com.canineai.backend.integration.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GlobalNavigationCoordinator {

    private final Map<String, String> navigationRoutes = new ConcurrentHashMap<>();

    public GlobalNavigationCoordinator() {
        // Initialize client routing parameters
        navigationRoutes.put("splash", "/splash");
        navigationRoutes.put("login", "/login");
        navigationRoutes.put("dashboard", "/dashboard");
        navigationRoutes.put("patients", "/patients");
        navigationRoutes.put("upload", "/upload");
        navigationRoutes.put("analysis", "/analysis");
        navigationRoutes.put("reports", "/reports");
        navigationRoutes.put("settings", "/settings");
    }

    public String getRoutePath(String screenKey) {
        String route = navigationRoutes.get(screenKey.toLowerCase());
        log.info("Resolving client navigation route: {} -> {}", screenKey, route);
        return route;
    }
}
