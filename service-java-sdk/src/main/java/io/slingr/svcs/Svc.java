package io.slingr.svcs;

import io.slingr.svcs.configurations.SvcDefinitions;
import io.slingr.svcs.configurations.SvcsProperties;
import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.framework.*;
import io.slingr.svcs.services.*;
import io.slingr.svcs.services.datastores.DataStore;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.ws.exchange.FunctionRequest;
import io.slingr.svcs.ws.exchange.WebServiceRequest;
import io.slingr.svcs.utils.tests.SvcTests;

/**
 * Abstract class that must be implemented by all services annotated with
 * {@link Svc}.
 *
 * This contains all the basic support to interact with the Slingr platform.
 *
 * Created by lefunes on 12/03/18.
 */
public abstract class Svc extends DefaultSvcLifecycleListener implements ISvc {

    public static final String DEBUG = "DEBUG>";

    protected final BaseModule baseModule;

    /**
     * Builds the default service implementation
     */
    public Svc() {
        this.baseModule = new BaseModule();
        this.baseModule.addLifecycleListener(this);
    }

    /**
     * Function called when the service is started
     */
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
     * @param propertyFile property file used as source of properties. This is used primary on development time.
     */
    public final void configure(String propertyFile){
        baseModule.configure(propertyFile, this);
    }

    @Override
    public SvcsProperties properties() {
        return baseModule.properties();
    }

    @Override
    public SvcDefinitions definitions() {
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
    public ESConfigurations svcServicesConfigurations() {
        return baseModule.svcServicesConfigurations();
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
    public DataStore dataStore(String dataStoreName) throws SvcException {
        return baseModule.dataStore(dataStoreName);
    }

    @Override
    public DataStore userDataStore() throws SvcException {
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
    public Json configurationInterceptor(Json configuration) throws SvcException {
        return configuration;
    }

    @Override
    public Object functionInterceptor(FunctionRequest request) throws SvcException {
        return null;
    }

    @Override
    public Object webServicesInterceptor(WebServiceRequest request) throws SvcException {
        return null;
    }

    /**
     * Gets the base module used by the service. This method is used on tests via {@link SvcTests}
     *
     * @return base module
     */
    @SuppressWarnings("unused")
    private BaseModule getBaseModule() {
        return baseModule;
    }
}
