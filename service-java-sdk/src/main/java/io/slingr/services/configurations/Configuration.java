package io.slingr.services.configurations;

import io.slingr.services.configurations.sources.*;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.services.exchange.ApiVersion;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.JsonSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that builds configuration objects from files, environment variables, etc.
 *
 */
public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    // properties name
    public static final String PROPERTY_SERVICE_NAME = "_service_name";
    public static final String PROPERTY_APPLICATION_NAME = "_app_name";
    public static final String PROPERTY_ENVIRONMENT = "_environment";
    public static final String PROPERTY_POD_ID = "_pod_id";
    public static final String PROPERTY_PROFILE = "_profile";
    public static final String PROPERTY_CUSTOM_DOMAIN = "_custom_domain";
    public static final String PROPERTY_BASE_DOMAIN = "_base_domain";
    public static final String PROPERTY_WEBSERVICES_PORT = "_webservices_port";
    public static final String PROPERTY_WEBSERVICES_URI = "_webservices_uri";
    public static final String PROPERTY_ORIGINAL_WEBSERVICES_URI = "_original_webservices_uri";
    public static final String PROPERTY_EXTENSION_BROKER_API = "_extension_broker_api";
    public static final String PROPERTY_EXTENSION_BROKER_VERSION = "_extension_broker_version";
    public static final String PROPERTY_LOCAL_DEPLOYMENT = "_local_deployment";
    public static final String PROPERTY_TOKEN = "_token";
    public static final String PROPERTY_SERVICE_CONFIG = "_service_config";
    public static final String PROPERTY_DEBUG = "_debug";
    public static final String PROPERTY_IS_SHARED = "_shared";
    public static final String PROPERTY_USING_PROXY = "_using_proxy";
    public static final String PROPERTY_TESTING_MODE = "_testing_mode";

    // definitions name
    public static final String DEFINITION_LABEL = "label";
    public static final String DEFINITION_TYPE = "name";
    public static final String DEFINITION_API_VERSION = "apiVersion";
    public static final String DEFINITION_INTERNAL = "internal";
    public static final String DEFINITION_CONFIGURATION_TYPE = "configurationType";
    public static final String DEFINITION_CONFIGURATION_TYPE_PER_USER = "PER_USER";
    public static final String DEFINITION_CONFIGURATION_TYPE_GLOBAL = "GLOBAL";
    public static final String DEFINITION_DEPLOYMENT = "deployment";
    public static final String DEFINITION_DEPLOYMENT_PROFILES = "profiles";
    public static final String DEFINITION_DEPLOYMENT_PROFILES_NAME = "name";
    public static final String DEFINITION_DEPLOYMENT_ALLOW_INSTANCES = "allowMultipleInstances";
    public static final String DEFINITION_ICON = "icon48";
    public static final String DEFINITION_HELP_URL = "configurationHelpUrl";
    public static final String DEFINITION_STATUS = "status";
    public static final String DEFINITION_STATUS_ACTIVE = "ACTIVE";
    public static final String DEFINITION_STATUS_DISABLED = "DISABLED";
    public static final String DEFINITION_DATA_STORES = "stores";
    public static final String DEFINITION_DATA_STORES_NAME = "name";
    public static final String DEFINITION_EVENTS = "events";
    public static final String DEFINITION_EVENTS_NAME = "name";
    public static final String DEFINITION_FUNCTIONS = "functions";
    public static final String DEFINITION_FUNCTIONS_NAME = "name";
    public static final String DEFINITION_UI_CONFIGURATION = "configuration";
    public static final String DEFINITION_UI_USER_CONFIGURATION = "userConfiguration";
    public static final String DEFINITION_UI_USER_CONFIGURATION_BUTTONS = "userConfigurationButtons";

    // definition file: appService.json by default
    private final String definitionsFile;

    // list of sources of configuration
    private final List<PropertySource> sources = new ArrayList<>();

    /**
     * Builds the configuration instance using the given parameters.
     * <p>
     * The service properties support the following sources with the indicated precedence:
     * <ul>
     *     <li>Json (used primary on tests)</li>
     *     <li>indicated properties file (used primary when the service runs locally / development time)</li>
     *     <li>system environment vars</li>
     *     <li>JVM properties</li>
     *     <li>default properties file</li>
     * </ul>
     *
     * @param jsonSource json source of properties. This is used primary on tests.
     */
    public Configuration(JsonSource jsonSource){
        this(jsonSource, null);
    }

    /**
     * Builds the configuration instance using the given parameters.
     * <p>
     * The service properties support the following sources with the indicated precedence:
     * <ul>
     *     <li>Json (used primary on tests)</li>
     *     <li>indicated properties file (used primary when the service runs locally / development time)</li>
     *     <li>system environment vars</li>
     *     <li>JVM properties</li>
     *     <li>default properties file</li>
     * </ul>
     *
     * @param propertyFile property file used as source of properties. This is used primary on development time.
     */
    public Configuration(String propertyFile){
        this(null, propertyFile);
    }

    /**
     * Builds the configuration instance using the given parameters.
     * <p>
     * The service properties support the following sources with the indicated precedence:
     * <ul>
     *     <li>Json (used primary on tests)</li>
     *     <li>indicated properties file (used primary when the service runs locally / development time)</li>
     *     <li>system environment vars</li>
     *     <li>JVM properties</li>
     *     <li>default properties file</li>
     * </ul>
     */
    public Configuration(){
        this(null, null);
    }

    /**
     * Builds the configuration instance using the given parameters.
     * <p>
     * The service properties support the following sources with the indicated precedence:
     * <ul>
     *     <li>Json (used primary on tests)</li>
     *     <li>indicated properties file (used primary when the service runs locally / development time)</li>
     *     <li>system environment vars</li>
     *     <li>JVM properties</li>
     *     <li>default properties file</li>
     * </ul>
     *
     * @param jsonSource json source of properties. This is used primary on tests.
     * @param propertyFile property file used as source of properties. This is used primary on development time.
     */
    public Configuration(JsonSource jsonSource, String propertyFile){
        this(null, jsonSource, propertyFile);
    }

    /**
     * Builds the configuration instance using the given parameters.
     * <p>
     * The service properties support the following sources with the indicated precedence:
     * <ul>
     *     <li>Json (used primary on tests)</li>
     *     <li>indicated properties file (used primary when the service runs locally / development time)</li>
     *     <li>system environment vars</li>
     *     <li>JVM properties</li>
     *     <li>default properties file</li>
     * </ul>
     *
     * @param jsonSource json source of properties. This is used primary on tests.
     * @param propertyFile property file used as source of properties. This is used primary on development time.
     */
    public Configuration(String definitionsFile, JsonSource jsonSource, String propertyFile){

        // fill the properties source list
        if(jsonSource != null) {
            this.sources.add(new JsonMapSource(jsonSource));
        }
        if(StringUtils.isNotBlank(propertyFile)) {
            this.sources.add(new PropertiesFileSource(propertyFile, true));
        }
        this.sources.add(new EnvVarsSource());
        this.sources.add(new JvmPropertiesSource());
        this.sources.add(new PropertiesFileSource("default.properties", true));

        // definition file
        this.definitionsFile = StringUtils.isNotBlank(definitionsFile) ? definitionsFile : "appService.json";
    }

    /**
     * Builds the service configuration from the properties sources
     *
     * @return service configuration
     * @throws ServiceException if there are issues to read the definitions file
     */
    public ServiceDefinitions definitions() throws ServiceException {
        if(StringUtils.isBlank(this.definitionsFile)){
            throw ServiceException.permanent(ErrorCode.GENERAL, "Definitions file name is invalid");
        }
        try {
            final Json definitions = Json.fromLocalFile(this.definitionsFile);
            return definitions(definitions);
        } catch (ServiceException ee){
            throw ee;
        } catch (Exception ex){
            throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Exception when try to read the service definitions: %s", ex.getMessage()), ex);
        }
    }

    /**
     * Builds the service configuration from the properties sources
     *
     * @param definitions json with the service definitions
     * @return service configuration
     * @throws ServiceException if there are issues to read the definitions file
     */
    ServiceDefinitions definitions(Json definitions) throws ServiceException {
        if(definitions == null || definitions.isEmpty()){
            throw ServiceException.permanent(ErrorCode.GENERAL, "Empty definitions content");
        }
        final DefinitionsBuilder builder = new DefinitionsBuilder();
        try {
            final String label = definitions.string(DEFINITION_LABEL);
            if(StringUtils.isBlank(label)){
                throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Empty service label on service definitions [%s]. Service label is required.", label));
            }
            builder.label = label;

            final String type = definitions.string(DEFINITION_TYPE);
            if(StringUtils.isBlank(type)){
                throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Empty service name on service definitions [%s]. Service name is required.", type));
            }
            builder.type = type;

            final String apiVersion = definitions.string(DEFINITION_API_VERSION);
            if(StringUtils.isNotBlank(apiVersion)){
                builder.apiVersion = apiVersion;
            }

            builder.internal = definitions.bool(DEFINITION_INTERNAL, false);

            final String configurationType = definitions.string(DEFINITION_CONFIGURATION_TYPE, "");
            if(StringUtils.isNotBlank(configurationType)){
                builder.perUserService = DEFINITION_CONFIGURATION_TYPE_PER_USER.equalsIgnoreCase(configurationType);
            } else {
                builder.perUserService = definitions.bool(DEFINITION_CONFIGURATION_TYPE_PER_USER, false);
            }

            final Json deployment = definitions.json(DEFINITION_DEPLOYMENT, null);
            if(deployment == null || deployment.isEmpty()){
                throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Empty deployment on service definitions [%s]. At least one deployment profile is required.", deployment));
            }
            final List<Json> deploymentProfiles = deployment.jsons(DEFINITION_DEPLOYMENT_PROFILES);
            if(deploymentProfiles == null || deploymentProfiles.isEmpty()){
                throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Empty deployment profiles on service definitions [%s]. At least one deployment profile is required.", deploymentProfiles));
            }
            builder.deploymentProfiles = deploymentProfiles;
            builder.allowMultipleInstances = deployment.bool(DEFINITION_DEPLOYMENT_ALLOW_INSTANCES, false);

            final String icon = definitions.string(DEFINITION_ICON);
            if(StringUtils.isNotBlank(icon)){
                builder.icon = icon;
            }

            final String configurationHelpUrl = definitions.string(DEFINITION_HELP_URL);
            if(StringUtils.isNotBlank(configurationHelpUrl)){
                builder.configurationHelpUrl = configurationHelpUrl;
            }

            final String status = definitions.string(DEFINITION_STATUS, "");
            if(StringUtils.isNotBlank(status)){
                builder.active = DEFINITION_STATUS_ACTIVE.equalsIgnoreCase(status);
            } else {
                builder.active = definitions.bool(DEFINITION_STATUS_ACTIVE, false);
            }

            final List<Json> dataStores = definitions.jsons(DEFINITION_DATA_STORES);
            if(dataStores != null && !dataStores.isEmpty()){
                builder.dataStores = dataStores;
            }

            final List<Json> events = definitions.jsons(DEFINITION_EVENTS);
            if(events != null && !events.isEmpty()){
                builder.events = events;
            }

            final List<Json> functions = definitions.jsons(DEFINITION_FUNCTIONS);
            if(functions != null && !functions.isEmpty()){
                builder.functions = functions;
            }

            final List<Json> configuration = definitions.jsons(DEFINITION_UI_CONFIGURATION);
            if(configuration != null && !configuration.isEmpty()){
                builder.configuration = configuration;
            }

            final List<Json> userConfiguration = definitions.jsons(DEFINITION_UI_USER_CONFIGURATION);
            if(userConfiguration != null && !userConfiguration.isEmpty()){
                builder.userConfiguration = userConfiguration;
            }

            final Json userConfigurationButtons = definitions.json(DEFINITION_UI_USER_CONFIGURATION_BUTTONS);
            if(userConfigurationButtons != null && !userConfigurationButtons.isEmpty()){
                builder.userConfigurationButtons = userConfigurationButtons;
            }

        } catch (ServiceException ee){
            throw ee;
        } catch (Exception ex){
            throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Exception when try to read the service definitions: %s", ex.getMessage()), ex);
        }
        return builder.build();
    }

    /**
     * Builds the service configuration from the properties sources
     *
     * @return service configuration
     * @throws ServiceException if there are issues to read the properties
     */
    public ServicesProperties properties() throws ServiceException {
        final PropertiesBuilder builder = new PropertiesBuilder();
        try {
            final String serviceName = resolveProperty(sources, PROPERTY_SERVICE_NAME);
            if(StringUtils.isBlank(serviceName)){
                throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Empty service name on properties [%s]. Service name is required.", serviceName));
            }
            builder.serviceName = serviceName;

            final String applicationName = resolveProperty(sources, PROPERTY_APPLICATION_NAME);
            if(StringUtils.isBlank(applicationName)){
                throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Empty application name on properties [%s]. Application name is required.", applicationName));
            }
            builder.applicationName = applicationName;

            final String environment = resolveProperty(sources, PROPERTY_ENVIRONMENT);
            if(StringUtils.isBlank(environment)){
                throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Empty environment name on properties [%s]. Environment name is required.", environment));
            }
            builder.environment = environment;

            final String podId = resolveProperty(sources, PROPERTY_POD_ID, "-");
            if (podId != null) {
                builder.podId = podId;
            }

            final String profile = resolveProperty(sources, PROPERTY_PROFILE, "default");
            if (profile != null) {
                builder.profile = profile;
            }

            final String customDomain = resolveProperty(sources, PROPERTY_CUSTOM_DOMAIN, "");
            if (customDomain != null) {
                builder.customDomain = customDomain;
            }

            final String baseDomain = resolveProperty(sources, PROPERTY_BASE_DOMAIN, "");
            if (baseDomain != null) {
                builder.baseDomain = baseDomain;
            }

            final Integer webServicesPort = resolveIntegerProperty(sources, PROPERTY_WEBSERVICES_PORT, 10000);
            if (webServicesPort != null) {
                builder.webServicesPort = webServicesPort;
            }

            final boolean localDeployment = resolveBooleanProperty(sources, PROPERTY_LOCAL_DEPLOYMENT, false);
            builder.localDeployment = localDeployment;

            String webServicesUri = resolveProperty(sources, PROPERTY_WEBSERVICES_URI, "");
            if (StringUtils.isBlank(webServicesUri)) {
                if (localDeployment) {
                    webServicesUri = String.format("http://localhost:%s", webServicesPort);
                } else {
                    String domain = String.format("https://%s.%s/%s", applicationName, baseDomain, environment).toLowerCase();

                    webServicesUri = String.format("%s/services/%s", domain, serviceName);
                }
            }
            builder.webServicesUri = webServicesUri;

            final String extensionBrokerApi = resolveProperty(sources, PROPERTY_EXTENSION_BROKER_API, "https://extension-broker/api");
            if(StringUtils.isBlank(extensionBrokerApi)){
                throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Empty Service Services API URI on properties [%s]. URI is required.", extensionBrokerApi));
            }
            builder.extensionBrokerApi = extensionBrokerApi;

            final String extensionBrokerApiVersion = resolveProperty(sources, PROPERTY_EXTENSION_BROKER_VERSION, ApiVersion.EXTENSION_BROKER_API_V1);
            if (extensionBrokerApiVersion != null) {
                builder.extensionBrokerApiVersion = extensionBrokerApiVersion;
            }

            final String token = resolveProperty(sources, PROPERTY_TOKEN);
            if(StringUtils.isNotBlank(token)){
                builder.token = token;
            } else if(localDeployment){
                builder.token = String.format("%s/%s/%s", applicationName, environment, serviceName);
            } else {
                throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Empty token on properties [%s]. Token is required.", token));
            }

            builder.serviceConfiguration = resolveJsonProperty(sources, PROPERTY_SERVICE_CONFIG);

            builder.debug = resolveBooleanProperty(sources, PROPERTY_DEBUG, false);
            builder.testingMode = resolveBooleanProperty(sources, PROPERTY_TESTING_MODE, false);
            builder.shared = resolveBooleanProperty(sources, PROPERTY_IS_SHARED, false);

        } catch (ServiceException ee){
            throw ee;
        } catch (Exception ex){
            throw ServiceException.permanent(ErrorCode.GENERAL, String.format("Exception when try to read the properties: %s", ex.getMessage()), ex);
        }
        return builder.build();
    }

    /**
     * Finds a json property on the stores with the given name.
     *
     * @param sources properties sources
     * @param propertyName the name of the property to resolve
     * @return the value of the property
     */
    public static Json resolveJsonProperty(List<PropertySource> sources, String propertyName){
        return resolveJsonProperty(sources, propertyName, null);
    }

    /**
     * Finds a json property on the stores with the given name.
     *
     * @param sources properties sources
     * @param propertyName the name of the property to resolve
     * @param defaultValue default name to use if the property is empty
     * @return the value of the property
     */
    public static Json resolveJsonProperty(List<PropertySource> sources, String propertyName, Json defaultValue){
        final String response = resolveProperty(sources, propertyName);
        Json json = null;
        if(StringUtils.isNotBlank(response)) {
            try {
                json = Json.parse(response);
            } catch (Exception ex) {
                logger.warn(String.format("Exception when try to convert json for property [%s], json [%s]: [%s]", propertyName, response, ex.getMessage()));
            }
        }
        return json != null ? json : (defaultValue != null ? defaultValue : Json.map());
    }

    /**
     * Finds a boolean property on the stores with the given name.
     *
     * @param sources properties sources
     * @param propertyName the name of the property to resolve
     * @return the value of the property
     */
    public static boolean resolveBooleanProperty(List<PropertySource> sources, String propertyName){
        return resolveBooleanProperty(sources, propertyName, null);
    }

    /**
     * Finds a boolean property on the stores with the given name.
     *
     * @param sources properties sources
     * @param propertyName the name of the property to resolve
     * @param defaultValue default name to use if the property is empty
     * @return the value of the property
     */
    public static boolean resolveBooleanProperty(List<PropertySource> sources, String propertyName, Boolean defaultValue){
        final String response = resolveProperty(sources, propertyName);
        return parseBooleanValue(response, defaultValue);
    }

    /**
     * Converts a string value to a boolean.
     *
     * @param value value to parse
     * @return the value of the property
     */
    public static boolean parseBooleanValue(String value){
        return parseBooleanValue(value, null);
    }

    /**
     * Converts a string value to a boolean.
     *
     * @param value value to parse
     * @param defaultValue default name to use if the property is empty
     * @return the value of the property
     */
    public static boolean parseBooleanValue(String value, Boolean defaultValue){
        if(StringUtils.isNotBlank(value)) {
            switch (value.trim().toLowerCase()) {
                case "0":
                case "no":
                case "false":
                case "disable":
                case "disabled":
                case "off":
                    return false;
                case "1":
                case "yes":
                case "true":
                case "enable":
                case "enabled":
                case "on":
                case "ok":
                    return true;
            }
        }
        return Boolean.TRUE.equals(defaultValue);
    }

    /**
     * Finds an integer property on the stores with the given name.
     *
     * @param sources properties sources
     * @param propertyName the name of the property to resolve
     * @return the value of the property
     */
    public static Integer resolveIntegerProperty(List<PropertySource> sources, String propertyName){
        return resolveIntegerProperty(sources, propertyName, null);
    }

    /**
     * Finds an integer property on the stores with the given name.
     *
     * @param sources properties sources
     * @param propertyName the name of the property to resolve
     * @param defaultValue default name to use if the property is empty
     * @return the value of the property
     */
    public static Integer resolveIntegerProperty(List<PropertySource> sources, String propertyName, Integer defaultValue){
        final String response = resolveProperty(sources, propertyName);
        Integer value = null;
        if(StringUtils.isNotBlank(response)) {
            try {
                value = Integer.parseInt(response);
            } catch (Exception ex){
                logger.warn(String.format("Exception when try to convert integer for property [%s], value [%s]: [%s]", propertyName, response, ex.getMessage()));
            }
        }
        return value != null ? value : (defaultValue);
    }

    /**
     * Finds a property on the stores with the given name.
     *
     * @param sources properties sources
     * @param propertyName the name of the property to resolve
     * @return the value of the property
     */
    public static String resolveProperty(List<PropertySource> sources, String propertyName){
        return resolveProperty(sources, propertyName, null);
    }

    /**
     * Finds a property on the stores with the given name.
     *
     * @param sources properties sources
     * @param propertyName the name of the property to resolve
     * @param defaultValue default name to use if the property is empty
     * @return the value of the property
     */
    private static String resolveProperty(List<PropertySource> sources, String propertyName, String defaultValue){
        String response = null;
        try {
            for (PropertySource source : sources) {
                response = source.findProperty(propertyName);
                if(response != null){
                    return response;
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("Exception when try to read [%s] property: [%s]", propertyName, e.getMessage()));
            response =  "";
        }
        return StringUtils.isNotBlank(response) ? response : defaultValue;
    }

    /**
     * Helper used to build Definitions objects
     */
    private static class DefinitionsBuilder {

        private String label = "";
        private String type = "";
        private String apiVersion = ApiVersion.SERVICES_API_V1;
        private boolean internal = false;
        private boolean perUserService = false;
        private List<Json> deploymentProfiles = new ArrayList<>();
        private boolean allowMultipleInstances = false;
        private String icon = "";
        private String configurationHelpUrl = "";
        private boolean active = true;
        private List<Json> dataStores = new ArrayList<>();
        private List<Json> events = new ArrayList<>();
        private List<Json> functions = new ArrayList<>();
        private List<Json> configuration = new ArrayList<>();
        private List<Json> userConfiguration = new ArrayList<>();
        private Json userConfigurationButtons = Json.map();

        /**
         * Builds a Properties with the given parameters
         *
         * @return Properties object
         */
        public ServiceDefinitions build(){
            return new Definitions(label, type, apiVersion, internal, perUserService, deploymentProfiles,
                    allowMultipleInstances, icon, configurationHelpUrl, active, dataStores, events, functions,
                    configuration, userConfiguration, userConfigurationButtons);
        }
    }

    /**
     * Helper used to build Properties objects
     */
    private static class PropertiesBuilder {

        private String serviceName = "";
        private String applicationName = "";
        private String environment = "";
        private String podId = "";
        private String profile = "default";
        private String customDomain = "";
        private String baseDomain = "";
        private int webServicesPort = 10000;
        private String webServicesUri = "";
        private String extensionBrokerApi = "https://extension-broker/api";
        private String extensionBrokerApiVersion = ApiVersion.EXTENSION_BROKER_API_V1;
        private boolean localDeployment = false;
        private String token = "";
        private Json serviceConfiguration = Json.map();
        private boolean debug = false;
        private boolean shared = false;
        private boolean testingMode = false;

        /**
         * Builds a Properties with the given parameters
         *
         * @return Properties object
         */
        public ServicesProperties build(){
            return new Properties(serviceName, applicationName, environment, podId, profile, customDomain, baseDomain,
                    webServicesPort, webServicesUri, extensionBrokerApi, extensionBrokerApiVersion,
                    localDeployment, token, serviceConfiguration, debug, testingMode, shared);
        }
    }
}