package io.slingr.services.framework;

/**
 * Interface to implement by objects that waits for Service lifecycle events
 *
 */
public interface ServiceLifecycleListener {
    /**
     * This method is called when the service configuration process is done.
     * <p>The definitions and properties are ready to be used at this point.
     */
    void serviceConfigured();

    /**
     * This method is called when the Extension Broker API configuration process is done.
     * <p>The communication with the Extension Broker app is ready to be used at this point.
     */
    void extensionBrokerConfigured();

    /**
     * This method is called after to start the web services server.
     * <p>The web services server is ready at this point. The service can receive request from external services and
     * from the platform (via the Extension Broker app).
     */
    void webServicesConfigured();

    /**
     * This method is called after the service completes the started process.
     * <p>App loggers, service properties, data stores, etc. are initialized at this point. The service is ready to be
     * used.
     */
    void serviceStarted();

    /**
     * This method is called after the service receive a termination signal.
     *
     * @param cause cause of the termination
     */
    void serviceStopped(String cause);
}
