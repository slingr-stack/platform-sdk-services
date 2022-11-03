package io.slingr.svcs.framework;

import io.slingr.svcs.Svc;
import io.slingr.svcs.configurations.SvcDefinitions;
import io.slingr.svcs.configurations.SvcsProperties;
import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.services.*;
import io.slingr.svcs.services.datastores.DataStore;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.converters.JsonSource;
import io.slingr.svcs.ws.exchange.FunctionRequest;
import io.slingr.svcs.ws.exchange.WebServiceRequest;

/**
 * Interface that all {@link Svc} must be implement
 *
 * Created by lefunes on 12/03/18.
 */
public interface ISvc extends JsonSource {

    /**
     * Returns the properties set to the service
     *
     * @return service properties
     */
    SvcsProperties properties();

    /**
     * Returns the definitions of the service type
     *
     * @return service definitions
     */
    SvcDefinitions definitions();

    /**
     * Returns the manager over all messages related to events
     *
     * @return events manager
     */
    Events events();

    /**
     * Returns the manager over all messages related to configuration scripts
     *
     * @return configuration scripts manager
     */
    Scripts scripts();

    /**
     * Returns the manager over all messages related to application logs
     *
     * @return application logs manager
     */
    AppLogs appLogs();

    /**
     * Returns the manager over all messages related to locks
     *
     * @return locks manager
     */
    Locks locks();

    /**
     * Returns the manager over all messages related to files
     *
     * @return files manager
     */
    Files files();

    /**
     * Returns the manager over all messages related to the Extension Broker configuration
     *
     * @return Extension Broker configuration manager
     */
    ESConfigurations svcServicesConfigurations();

    /**
     * Returns the manager over all messages related to application users
     *
     * @return application users manager
     */
    AppUsers appUsers();

    /**
     * Returns the manager for the service
     *
     * @return service manager
     */
    Management management();

    /**
     * Returns the manager over all messages related to data stores
     *
     * @return data stores manager
     */
    DataStores dataStores();

    /**
     * Gets the Data Store to use in the service.
     *
     * @param dataStoreName name of the data store to properties
     * @throws SvcException if there is an issue with the data store name value
     * @return data store
     */
    DataStore dataStore(String dataStoreName) throws SvcException;

    /**
     * Gets the UserRecordData Store to use in the service.
     *
     * @throws SvcException if there is an issue with the user data store
     * @return user data store
     */
    DataStore userDataStore() throws SvcException;

    /**
     * Returns a representation of the service through its definitions and properties
     *
     * @return service representation
     */
    @Override
    Json toJson();

    /**
     * Setup the parameters to deal with exceptions
     *
     * @param maxRedelivers maximum retries of a request that throws an exception
     */
    void setupDefaultExceptionsProperties(int maxRedelivers);

    /**
     * Setup the parameters to deal with permanent exceptions
     *
     * @param maxRedelivers maximum retries of a request that throws a permanent exception
     * @param delay delay between retries
     */
    void setupPermanentExceptionsProperties(int maxRedelivers, long delay);

    /**
     * Setup the parameters to deal with retryable exceptions
     *
     * @param maxRedelivers maximum retries of a request that throws a retryable exception
     * @param delay delay between retries
     */
    void setupRetryableExceptionsProperties(int maxRedelivers, long delay);

    /**
     * Interceptor of the get configuration requests. This can be enabled with {@code Svc.baseSvc.enabledConfiguratorInterceptor() }
     *
     * @param configuration current service configuration
     * @return configuration
     * @throws SvcException exception if an error happened when process the get configuration request
     */
    Json configurationInterceptor(Json configuration) throws SvcException;

    /**
     * Interceptor of the function requests. This can be enabled with {@code Svc.baseSvc.enableFunctionInterceptor() }
     *
     * @param request function request
     * @return function response
     * @throws SvcException exception if an error happened when process the function request
     */
    Object functionInterceptor(FunctionRequest request) throws SvcException;

    /**
     * Interceptor of the web service requests. This can be enabled with {@code Svc.baseSvc.enableWebServicesInterceptor() }
     *
     * @param request web service request
     * @return web service response
     * @throws SvcException exception if an error happened when process the web service request
     */
    Object webServicesInterceptor(WebServiceRequest request) throws SvcException;
}
