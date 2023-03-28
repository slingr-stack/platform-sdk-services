package io.slingr.services.configurations;

import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.JsonSource;

import java.util.List;

/**
 * Service definition captured from the appService.json file.
 *
 * <p>Created by lefunes on 23/03/18.
 */
public interface ServiceDefinitions extends JsonSource {

    /**
     * Gets the label of the service type
     *
     * @return service type label
     */
    String getLabel();

    /**
     * Gets the name of service type
     *
     * @return service type name
     */
    String getType();

    /**
     * Gets the api version that the service implements
     *
     * @return api version number
     */
    String getApiVersion();

    /**
     * Returns true if the service is defined as internal service
     *
     * @return true if the service is defined as internal service
     */
    boolean isInternal();

    /**
     * Gets the type of service. This can be 'GLOBAL' or 'PER_USER'
     *
     * @return service type
     */
    String getConfigurationType();

    /**
     * Returns true if the service is defined as PER USER service
     *
     * @return true if the service is defined as PER USER service
     */
    boolean isPerUserService();

    /**
     * Gets the deployment information
     *
     * @return deployment information
     */
    Json getDeployment();

    /**
     * Gets the list of valid deployment profiles
     *
     * @return deployment profiles list
     */
    List<Json> getDeploymentProfiles();

    /**
     * Returns true if the given profile name is valid
     *
     * @param profileName profile name
     * @return true if the given profile name is valid
     */
    boolean isValidDeploymentProfile(String profileName);

    /**
     * Returns the profile found with the given profile name. Otherwise, returns null.
     *
     * @param profileName profile name
     * @return the profile found with the given profile name. Otherwise, returns null.
     */
    Json getDeploymentProfile(String profileName);

    /**
     * Returns true if the service implementation permits multiples instances of the same service working at same
     * time.
     *
     * @return true if the service allow multiple instances
     */
    boolean allowMultipleInstances();

    /**
     * Gets the URL of the icon image
     *
     * @return URL of the icon image
     */
    String getIcon();

    /**
     * Gets the URL if the helper page in order to show it to the app developers
     *
     * @return URL of the helper page
     */
    String getConfigurationHelpUrl();

    /**
     * Gets the status of the service. 'ACTIVE' indicates that the service is active.
     *
     * @return status of the service
     */
    String getStatus();

    /**
     * Returns true if the service type is active.
     *
     * @return true if the service type is active.
     */
    boolean isActive();

    /**
     * Gets the list of valid data stores
     *
     * @return data stores list
     */
    List<Json> getDataStores();

    /**
     * Gets the list of valid data stores names
     *
     * @return data stores names list
     */
    List<String> getDataStoresNames();

    /**
     * Returns true if the given data store name is valid
     *
     * @param dataStoreName data store name
     * @return true if the given data store name is valid
     */
    boolean isValidDataStore(String dataStoreName);

    /**
     * Returns the data store found with the given data store name. Otherwise, returns null.
     *
     * @param dataStoreName data store name
     * @return the data store found with the given data store name. Otherwise, returns null.
     */
    Json getDataStore(String dataStoreName);

    /**
     * Gets the list of valid events
     *
     * @return events list
     */
    List<Json> getEvents();

    /**
     * Returns true if the given event name is valid
     *
     * @param eventName event name
     * @return true if the given event name is valid
     */
    boolean isValidEvent(String eventName);

    /**
     * Returns the event found with the given event name. Otherwise, returns null.
     *
     * @param eventName event name
     * @return the event found with the given event name. Otherwise, returns null.
     */
    Json getEvent(String eventName);

    /**
     * Gets the list of valid functions
     *
     * @return functions list
     */
    List<Json> getFunctions();

    /**
     * Returns true if the given function name is valid
     *
     * @param functionName function name
     * @return true if the given function name is valid
     */
    boolean isValidFunction(String functionName);

    /**
     * Returns the function found with the given function name. Otherwise, returns null.
     *
     * @param functionName function name
     * @return the function found with the given function name. Otherwise, returns null.
     */
    Json getFunction(String functionName);

    /**
     * Gets a list of the UI configurations to show on the App Builder app
     *
     * @return UI configurations list
     */
    List<Json> getUIConfiguration();

    /**
     * Gets a list of the user UI configurations to show on the App Runtime app
     *
     * @return user UI configurations list
     */
    List<Json> getUIUserConfiguration();

    /**
     * Gets a map with th button configuration of the user UI to show on the App Runtime app
     *
     * @return button configuration for user UI
     */
    Json getUIUserConfigurationButtons();

    /**
     * Gets a map with the service definition extracted from the appService.json file
     *
     * @return json definition
     */
    @Override
    Json toJson();
}
