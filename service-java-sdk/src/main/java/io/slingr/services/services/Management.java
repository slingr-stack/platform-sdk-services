package io.slingr.services.services;

/**
 * Provides management services for the service.
 *
 * <p>Created by dgaviola on 11/01/18.
 */
public class Management {

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