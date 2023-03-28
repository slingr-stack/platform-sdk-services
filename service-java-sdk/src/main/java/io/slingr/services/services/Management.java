package io.slingr.services.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides management services for the service.
 *
 * <p>Created by dgaviola on 11/01/18.
 */
public class Management {
    private static final Logger logger = LoggerFactory.getLogger(Management.class);

    private final ExtensionBrokerApi api;

    /**
     * Constructor only can be called by Extension Broker class
     *
     * @param api extension broker implementation
     */
    public Management(ExtensionBrokerApi api) {
        this.api = api;
    }

    /**
     * Clears the cache of the app.
     */
    public void clearCache() {
        api.clearCache();
    }
}
