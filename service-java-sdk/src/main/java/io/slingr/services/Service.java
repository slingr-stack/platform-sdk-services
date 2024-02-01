package io.slingr.services;

import io.slingr.services.configurations.ServiceDefinitions;
import io.slingr.services.configurations.ServicesProperties;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.framework.*;
import io.slingr.services.services.*;
import io.slingr.services.services.datastores.DataStore;
import io.slingr.services.utils.Json;
import io.slingr.services.ws.exchange.FunctionRequest;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.utils.tests.ServiceTests;

/**
 * Abstract class that must be implemented by all services annotated with
 * {@link Service}.
 * <p>
 * This contains all the basic support to interact with the Slingr platform.
 *
 */
public abstract class Service extends DefaultServiceLifecycleListener implements IService {

    public static final String DEBUG = "DEBUG>";

    protected final BaseModule baseModule;

    /**
     * Builds the default service implementation
     */
    public Service() {
        this.baseModule = new BaseModule();
        this.baseModule.addLifecycleListener(this);
    }

    /**
     * Function called when the service is started
     */
    @SuppressWarnings("unused")
    protected final void finishedStart() {
        baseModule.finishedStart();
    }

    /**
     * Builds the default service implementation.
     */
    public final void configure(){
        configure(null);
    }

    /**
     * Builds the default service implementation using the given parameters.
     *
     * @param propertyFile property file used as sources of properties. This is used primary on development time.
     */
    public final void configure(String propertyFile){
        baseModule.configure(propertyFile, this);
    }

    @Override
    public ServicesProperties properties() {
        return baseModule.properties();
    }

    @Override
    public ServiceDefinitions definitions() {
        return baseModule.definitions();
    }

    @Override
    public Events events() {
        return baseModule.events();
    }

    @Override
    public AppLogs appLogs() {
        return baseModule.appLogs();
    }

    @Override
    public Locks locks() {
        return baseModule.locks();
    }

    @Override
    public Files files() {
        return baseModule.files();
    }

    @Override
    public EBConfigurations serviceConfigurations() {
        return baseModule.serviceConfigurations();
    }

    @Override
    public AppUsers appUsers() {
        return baseModule.appUsers();
    }

    @Override
    public Management management() {
        return baseModule.management();
    }

    @Override
    public DataStores dataStores() {
        return baseModule.dataStores();
    }

    @Override
    public DataStore dataStore(String dataStoreName) throws ServiceException {
        return baseModule.dataStore(dataStoreName);
    }

    @Override
    public DataStore userDataStore() throws ServiceException {
        return baseModule.userDataStore();
    }

    @Override
    public Json toJson() {
        return baseModule.toJson();
    }

    /**
     * Register a Slingr function to be executed when arrives the platform requests
     *
     * @param function function definition
     * @param throwExceptionIfInvalid true if an exception is thrown when the function is invalid.
     */
    @SuppressWarnings("unused")
    private void internalRegisterFunction(RegisteredFunction function, Boolean throwExceptionIfInvalid){
        baseModule.registerFunction(function, throwExceptionIfInvalid);
    }

    /**
     * Register a web service to be executed when arrives request to the web service server
     *
     * @param webService web service definition
     */
    @SuppressWarnings("unused")
    private void internalRegisterWebService(RegisteredWebService webService){
        baseModule.registerWebService(webService);
    }

    @Override
    public void setupDefaultExceptionsProperties(int maxRedelivers){
        this.baseModule.setupDefaultExceptionsProperties(maxRedelivers);
    }

    @Override
    public void setupPermanentExceptionsProperties(int maxRedelivers, long delay){
        this.baseModule.setupPermanentExceptionsProperties(maxRedelivers, delay);
    }

    @Override
    public void setupRetryableExceptionsProperties(int maxRedelivers, long delay){
        this.baseModule.setupRetryableExceptionsProperties(maxRedelivers, delay);
    }

    @Override
    public Json configurationInterceptor(Json configuration) throws ServiceException {
        return configuration;
    }

    @Override
    public Object functionInterceptor(FunctionRequest request) throws ServiceException {
        return null;
    }

    @Override
    public Object webServicesInterceptor(WebServiceRequest request) throws ServiceException {
        return null;
    }

    /**
     * Gets the base module used by the service. This method is used on tests via {@link ServiceTests}
     *
     * @return base module
     */
    @SuppressWarnings("unused")
    private BaseModule getBaseModule() {
        return baseModule;
    }
}
