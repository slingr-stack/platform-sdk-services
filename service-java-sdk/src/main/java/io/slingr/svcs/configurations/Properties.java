package io.slingr.svcs.configurations;

import io.slingr.svcs.utils.Json;
import org.apache.commons.lang3.StringUtils;

/**
 * Service properties found on environment variables, properties files, etc.
 *
 * <p>Created by lefunes on 02/05/16.
 */
public class Properties implements SvcsProperties {

    // properties
    private final String svcName;
    private final String applicationName;
    private final String environment;
    private final String podId;
    private final String profile;
    private final String customDomain;
    private final String baseDomain;
    private final int webServicesPort;
    private String defaultWebServicesUri;
    private final String webServicesUri;
    private final String extensionBrokerApi;
    private final String extensionBrokerApiVersion;
    private final boolean localDeployment;
    private final String token;
    private final Json svcConfiguration;
    private final boolean debug;
    private final boolean testingMode;
    private final boolean shared;
    private boolean usingProxy = false;

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // initialization
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Builds a Properties instance
     *
     * @param svcName service name
     * @param applicationName application name
     * @param environment environment name
     * @param podId pod identifier
     * @param profile deployment profile name
     * @param customDomain custom domain
     * @param baseDomain base domain
     * @param webServicesPort web services port number
     * @param webServicesUri web services uri
     * @param extensionBrokerApi extension broker api
     * @param extensionBrokerApiVersion extension broker api version
     * @param localDeployment true if is executed on local environment
     * @param token token to exchange information with the Extension Broker app
     * @param svcConfiguration service configuration
     * @param debug true if the service shows information useful for debug
     * @param testingMode true if the service is working in testing mode
     */
    Properties(String svcName, String applicationName, String environment, String podId, String profile,
               String customDomain, String baseDomain, int webServicesPort, String webServicesUri, String extensionBrokerApi, String extensionBrokerApiVersion,
               boolean localDeployment, String token, Json svcConfiguration, boolean debug, boolean testingMode, boolean shared) {
        this.svcName = svcName;
        this.applicationName = applicationName;
        this.environment = environment;
        this.podId = podId;
        this.profile = profile;
        this.customDomain = customDomain;
        this.baseDomain = baseDomain;
        this.webServicesPort = webServicesPort;
        this.webServicesUri = webServicesUri;
        this.extensionBrokerApi = extensionBrokerApi;
        this.extensionBrokerApiVersion = extensionBrokerApiVersion;
        this.localDeployment = localDeployment;
        this.token = token;
        this.svcConfiguration = svcConfiguration;
        this.debug = debug;
        this.testingMode = testingMode;
        this.shared = shared;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // setters
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void setDefaultWebServicesUri(String defaultWebServicesUri) {
        this.defaultWebServicesUri = defaultWebServicesUri;
    }

    @Override
    public void setUsingProxy(boolean usingProxy) {
        this.usingProxy = usingProxy;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // getters
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String getSvcName() {
        return svcName;
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public String getEnvironment() {
        return environment;
    }

    @Override
    public String getPodId() {
        return podId;
    }

    @Override
    public String getProfile() {
        return profile;
    }

    @Override
    public String getCustomDomain() {
        return customDomain;
    }

    @Override
    public String getBaseDomain() {
        return baseDomain;
    }

    @Override
    public int getWebServicesPort() {
        return webServicesPort;
    }

    @Override
    public String getWebServicesUri() {
        if(StringUtils.isNotBlank(defaultWebServicesUri)){
            return defaultWebServicesUri;
        } else {
            return webServicesUri;
        }
    }

    @Override
    public String getOriginalWebServicesUri() {
        return webServicesUri;
    }

    @Override
    public boolean isUsingProxy() {
        return usingProxy;
    }

    @Override
    public String getSvcsServicesApi() {
        return extensionBrokerApi;
    }

    @Override
    public String getSvcsServicesApiVersion() {
        return extensionBrokerApiVersion;
    }

    @Override
    public boolean isLocalDeployment() {
        return localDeployment;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public Json getSvcConfiguration() {
        return svcConfiguration;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean isShared() {
        return shared;
    }

    @Override
    public boolean isTestingMode() {
        return testingMode;
    }

    @Override
    public Json toJson() {
        return Json.map()
                .set(Configuration.PROPERTY_SVC_NAME, getSvcName())
                .set(Configuration.PROPERTY_APPLICATION_NAME, getApplicationName())
                .set(Configuration.PROPERTY_ENVIRONMENT, getEnvironment())
                .set(Configuration.PROPERTY_POD_ID, getPodId())
                .set(Configuration.PROPERTY_PROFILE, getProfile())
                .set(Configuration.PROPERTY_CUSTOM_DOMAIN, getCustomDomain())
                .set(Configuration.PROPERTY_BASE_DOMAIN, getBaseDomain())
                .set(Configuration.PROPERTY_WEBSERVICES_PORT, getWebServicesPort())
                .set(Configuration.PROPERTY_WEBSERVICES_URI, getWebServicesUri())
                .set(Configuration.PROPERTY_EXTENSION_BROKER_API, getSvcsServicesApi())
                .set(Configuration.PROPERTY_EXTENSION_BROKER_VERSION, getSvcsServicesApiVersion())
                .set(Configuration.PROPERTY_LOCAL_DEPLOYMENT, isLocalDeployment())
                .set(Configuration.PROPERTY_TOKEN, getToken())
                .set(Configuration.PROPERTY_SVC_CONFIG, getSvcConfiguration())
                .set(Configuration.PROPERTY_DEBUG, isDebug())
                .set(Configuration.PROPERTY_IS_SHARED, isShared())
                .set(Configuration.PROPERTY_ORIGINAL_WEBSERVICES_URI, getOriginalWebServicesUri())
                .set(Configuration.PROPERTY_TESTING_MODE, isTestingMode())
                .set(Configuration.PROPERTY_USING_PROXY, isUsingProxy());
    }
}
