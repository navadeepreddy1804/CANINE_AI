package com.canineai.backend.service;

public interface ConfigurationManager {

    /**
     * Resolves key configurations for active providers.
     * Never stores keys on mobile clients. Parses credentials strictly on backend environment scopes.
     * @param configKey Key lookup string.
     * @return Parameter value.
     */
    String getConfigurationValue(String configKey);
}
