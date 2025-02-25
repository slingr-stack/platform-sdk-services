package io.slingr.services.configurations;

import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.JsonSource;

/**
 * Service properties found on environment variables, properties files, etc.
 *
 * <p>Created by lefunes on 23/03/18.
 */
public interface ServicesProperties extends JsonSource {

    /**
     * Sets the value of the default web services uri. This is used when the service works through a proxy, and we want
     * to override the URI used by the external services. This URI is used instead of proxy web services uri and the
     * service uri.
     *
     * @param defaultWebServicesUri defaults web services uri
     */
    void setDefaultWebServicesUri(String defaultWebServicesUri);

    /**
     * Sets true if the service is working though a proxy
     *
     * @param usingProxy true if the service is working though a proxy
     */
    void setUsingProxy(boolean usingProxy);

    /**
     * Gets the service name
     *
     * @return service name
     */
    String getServiceName();

    /**
     * Gets the application name
     *
     * @return application name
     */
    String getApplicationName();

    /**
     * Gets the environment name
     *
     * @return environment name
     */
    String getEnvironment();

    /**
     * Gets the POD identifier
     *
     * @return pod identifier
     */
    String getPodId();

    /**
     * Gets the deployment profile name configured for the service
     *
     * @return deployment profile name
     */
    String getProfile();

    /**
     * Gets the custom domain if it is defined
     *
     * @return custom domain
     */
    String getCustomDomain();

    /**
     * Gets the base domain used by the service
     *
     * @return base domain
     */
    String getBaseDomain();

    /**
     * Gets the number the port used for the web services defined on the service
     *
     * @return web services port number
     */
    int getWebServicesPort();

    /**
     * Gets the web services URI where the service receives requests.
     *
     * @return web services uri
     */
    String getWebServicesUri();

    /**
     * Gets the web services URI defined on the service. This method ignores the set uri with
     * {@link #setDefaultWebServicesUri(String)} }, used when the service works through a proxy.
     *
     * @return web services uri
     */
    String getOriginalWebServicesUri();

    /**
     * Returns true if the service is working through a proxy.
     *
     * @return true if the service is working through a proxy.
     */
    boolean isUsingProxy();

    /**
     * Gets the URL used by to Service Services
     *
     * @return Services services api
     */
    String getServicesApi();

    /**
     * Gets implemented Extension Broker  API version number
     *
     * @return service services api version
     */
    String getServicesApiVersion();

    /**
     * True if the service is executed in a local environment
     *
     * @return true if is executed on local environment
     */
    boolean isLocalDeployment();

    /**
     * Gets the service token used to perform communication with the Extension Broker app
     *
     * @return token to exchange information with the Extension Broker app
     */
    String getToken();

    /**
     * Gets the service specific properties
     *
     * @return service configuration
     */
    Json getServiceConfiguration();

    /**
     * True if the service shows information useful for debug
     *
     * @return true if the service shows information useful for debug
     */
    boolean isDebug();

    /**
     * True if the service is working in testing mode
     *
     * @return true if the service is working in testing mode
     */
    boolean isTestingMode();

    /**
     * True if the service is shared across different apps.
     *
     * @return true the service is shared across different apps.
     */
    boolean isShared();

    /**
     * Gets a map with the service properties. We will give priority to system environment properties
     * first, then to JVM properties, and finally we will use the indicated .properties file.
     *
     * @return json object of properties
     */
    @Override
    Json toJson();
}