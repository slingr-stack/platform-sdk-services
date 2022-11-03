package io.slingr.svcs.configurations;

import io.slingr.svcs.services.DataStores;
import io.slingr.svcs.services.exchange.ReservedName;
import io.slingr.svcs.utils.Json;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Service definition captured from the appService.json file.
 *
 * <p>Created by lefunes on 23/03/18.
 */
public class Definitions implements SvcDefinitions {

    private final String label;
    private final String type;
    private final String apiVersion;
    private final boolean internal;
    private final boolean perUserSvc;
    private final List<Json> deploymentProfiles;
    private final boolean allowMultipleInstances;
    private final String icon;
    private final String configurationHelpUrl;
    private final boolean active;
    private final List<Json> dataStores;
    private final List<Json> events;
    private final List<Json> functions;
    private final String scripts;
    private final String listeners;
    private final List<Json> configuration;
    private final List<Json> userConfiguration;
    private final Json userConfigurationButtons;

    /**
     * Builds a Definitions instance
     *
     * @param label service type label
     * @param type service type name
     * @param apiVersion api version number
     * @param internal true if the service is defined as internal service
     * @param perUserSvc true if the service is defined as PER USER service
     * @param deploymentProfiles deployment profiles list
     * @param allowMultipleInstances true if the service allow multiple instances
     * @param icon URL of the icon image
     * @param configurationHelpUrl URL of the helper page
     * @param active true if the given data store name is valid
     * @param dataStores data stores list
     * @param events events list
     * @param functions functions list
     * @param scripts script files content
     * @param listeners listener files content
     * @param configuration UI configurations list
     * @param userConfiguration user UI configurations list
     * @param userConfigurationButtons button configuration for user UI
     */
    Definitions(String label, String type, String apiVersion, boolean internal, boolean perUserSvc,
                       List<Json> deploymentProfiles, boolean allowMultipleInstances, String icon, String configurationHelpUrl,
                       boolean active, List<Json> dataStores, List<Json> events, List<Json> functions, String scripts,
                       String listeners, List<Json> configuration, List<Json> userConfiguration, Json userConfigurationButtons) {
        this.label = label;
        this.type = type;
        this.apiVersion = apiVersion;
        this.internal = internal;
        this.deploymentProfiles = deploymentProfiles;
        this.allowMultipleInstances = allowMultipleInstances;
        this.icon = icon;
        this.configurationHelpUrl = configurationHelpUrl;
        this.active = active;
        this.scripts = scripts;
        this.listeners = listeners;
        this.configuration = configuration;
        this.userConfiguration = userConfiguration;
        this.userConfigurationButtons = userConfigurationButtons;

        this.perUserSvc = perUserSvc;
        if(this.perUserSvc){
            // add the USERS data store if this is not defined
            dataStores = dataStores != null ? dataStores : new ArrayList<>();
            if(dataStores.stream().noneMatch(json -> "name".equals(json.string(DataStores.USER_DATA_STORE)))){
                dataStores.add(Json.map().set("name", DataStores.USER_DATA_STORE));
            }

            // add PER USER events if these are not defined
            events = events != null ?  events : new ArrayList<>();
            if(events.stream().noneMatch(json -> "name".equals(json.string(ReservedName.USER_CONNECTED)))){
                events.add(Json.map()
                        .set("label", "User connected")
                        .set("name", ReservedName.USER_CONNECTED)
                        .set("eventType", "PER_USER")
                        .set("description", "Event triggered when the current user is connected to the service.")
                );
            }
            if(events.stream().noneMatch(json -> "name".equals(json.string(ReservedName.USER_DISCONNECTED)))){
                events.add(Json.map()
                        .set("label", "User disconnected")
                        .set("name", ReservedName.USER_DISCONNECTED)
                        .set("eventType", "PER_USER")
                        .set("description", "Event triggered when the current user is disconnected from the service.")
                );
            }

            // add PER USER functions if these are not defined
            functions = functions != null ? functions : new ArrayList<>();
            if(functions.stream().noneMatch(json -> "name".equals(json.string(ReservedName.CONNECT_USER)))){
                functions.add(Json.map()
                        .set("label", "Connect User")
                        .set("name", ReservedName.CONNECT_USER)
                        .set("eventType", "PER_USER")
                        .set("description", "Connects the user to the service.")
                        .set("callbacks", Json.list()
                                .push(Json.map()
                                    .set("name", ReservedName.USER_CONNECTED)
                                    .set("maxWaitingTime", 60000)
                                    .set("maxExpectedResponses", 1)
                                )
                                .push(Json.map()
                                    .set("name", ReservedName.USER_DISCONNECTED)
                                    .set("maxWaitingTime", 60000)
                                    .set("maxExpectedResponses", 1)
                                )
                        )
                );
            }
            if(functions.stream().noneMatch(json -> "name".equals(json.string(ReservedName.DISCONNECT_USER)))){
                functions.add(Json.map()
                        .set("label", "Disconnect User")
                        .set("name", ReservedName.DISCONNECT_USER)
                        .set("eventType", "PER_USER")
                        .set("description", "Disconnects the user from the service.")
                        .set("callbacks", Json.list()
                                .push(Json.map()
                                        .set("name", ReservedName.USER_DISCONNECTED)
                                        .set("maxWaitingTime", 60000)
                                        .set("maxExpectedResponses", 1)
                                )
                        )
                );
            }
        }
        this.dataStores = dataStores;
        this.events = events;
        this.functions = functions;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public boolean isInternal() {
        return internal;
    }

    @Override
    public String getConfigurationType() {
        return isPerUserSvc() ? Configuration.DEFINITION_CONFIGURATION_TYPE_PER_USER : Configuration.DEFINITION_CONFIGURATION_TYPE_GLOBAL;
    }

    @Override
    public boolean isPerUserSvc() {
        return perUserSvc;
    }

    @Override
    public Json getDeployment() {
        return Json.map()
                .set(Configuration.DEFINITION_DEPLOYMENT_PROFILES, getDeploymentProfiles())
                .set(Configuration.DEFINITION_DEPLOYMENT_ALLOW_INSTANCES, allowMultipleInstances());
    }

    @Override
    public List<Json> getDeploymentProfiles() {
        return deploymentProfiles;
    }

    @Override
    public boolean isValidDeploymentProfile(String profileName) {
        final Json profile = getDeploymentProfile(profileName);
        return profile != null;
    }

    @Override
    public Json getDeploymentProfile(String profileName) {
        if(StringUtils.isBlank(profileName)){
            return null;
        }
        for (Json profile : deploymentProfiles) {
            final String name = profile.string(Configuration.DEFINITION_DEPLOYMENT_PROFILES_NAME);
            if(profileName.equals(name)){
                return profile;
            }
        }
        return null;
    }

    @Override
    public boolean allowMultipleInstances() {
        return allowMultipleInstances;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public String getConfigurationHelpUrl() {
        return configurationHelpUrl;
    }

    @Override
    public String getStatus() {
        return isActive() ? Configuration.DEFINITION_STATUS_ACTIVE : Configuration.DEFINITION_STATUS_DISABLED;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public List<Json> getDataStores() {
        return dataStores;
    }

    @Override
    public List<String> getDataStoresNames() {
        final List<String> names = new ArrayList<>();
        for (Json dataStore : dataStores) {
            final String name = dataStore.string(Configuration.DEFINITION_DATA_STORES_NAME);
            if(StringUtils.isNotBlank(name)){
                names.add(name);
            }
        }
        return names;
    }

    @Override
    public boolean isValidDataStore(String dataStoreName) {
        final Json dataStore = getDataStore(dataStoreName);
        return dataStore != null;
    }

    @Override
    public Json getDataStore(String dataStoreName) {
        if(StringUtils.isBlank(dataStoreName)){
            return null;
        }
        for (Json dataStore : dataStores) {
            final String name = dataStore.string(Configuration.DEFINITION_DATA_STORES_NAME);
            if(dataStoreName.equals(name)){
                return dataStore;
            }
        }
        return null;
    }

    @Override
    public List<Json> getEvents() {
        return events;
    }

    @Override
    public boolean isValidEvent(String eventName) {
        final Json event = getEvent(eventName);
        return event != null;
    }

    @Override
    public Json getEvent(String eventName) {
        if(StringUtils.isBlank(eventName)){
            return null;
        }
        for (Json event : events) {
            final String name = event.string(Configuration.DEFINITION_EVENTS_NAME);
            if(eventName.equals(name)){
                return event;
            }
        }
        return null;
    }

    @Override
    public List<Json> getFunctions() {
        return functions;
    }

    @Override
    public boolean isValidFunction(String functionName) {
        final Json function = getFunction(functionName);
        return function != null;
    }

    @Override
    public Json getFunction(String functionName) {
        if(StringUtils.isBlank(functionName)){
            return null;
        }
        for (Json function : functions) {
            final String name = function.string(Configuration.DEFINITION_FUNCTIONS_NAME);
            if(functionName.equals(name)){
                return function;
            }
        }
        return null;
    }

    @Override
    public String getScripts() {
        return scripts;
    }

    @Override
    public String getListeners() {
        return listeners;
    }

    @Override
    public List<Json> getUIConfiguration() {
        return configuration;
    }

    @Override
    public List<Json> getUIUserConfiguration() {
        return userConfiguration;
    }

    @Override
    public Json getUIUserConfigurationButtons() {
        return userConfigurationButtons;
    }

    @Override
    public Json toJson() {
        return Json.map()
                .set(Configuration.DEFINITION_LABEL, getLabel())
                .set(Configuration.DEFINITION_TYPE, getType())
                .set(Configuration.DEFINITION_API_VERSION, getApiVersion())
                .set(Configuration.DEFINITION_INTERNAL, isInternal())
                .set(Configuration.DEFINITION_CONFIGURATION_TYPE, getConfigurationType())
                .set(Configuration.DEFINITION_DEPLOYMENT, getDeployment())
                .setIfNotEmpty(Configuration.DEFINITION_ICON, getIcon())
                .setIfNotEmpty(Configuration.DEFINITION_HELP_URL, getConfigurationHelpUrl())
                .set(Configuration.DEFINITION_STATUS, getStatus())
                .setIfNotEmpty(Configuration.DEFINITION_DATA_STORES, getDataStores())
                .setIfNotEmpty(Configuration.DEFINITION_EVENTS, getEvents())
                .setIfNotEmpty(Configuration.DEFINITION_FUNCTIONS, getFunctions())
                .setIfNotEmpty(Configuration.DEFINITION_SCRIPTS, getScripts())
                .setIfNotEmpty(Configuration.DEFINITION_LISTENERS, getListeners())
                .setIfNotEmpty(Configuration.DEFINITION_UI_CONFIGURATION, getUIConfiguration())
                .setIfNotEmpty(Configuration.DEFINITION_UI_USER_CONFIGURATION, getUIUserConfiguration())
                .setIfNotEmpty(Configuration.DEFINITION_UI_USER_CONFIGURATION_BUTTONS, getUIUserConfigurationButtons());
    }
}
