package io.slingr.svcs.framework;

import io.slingr.svcs.Svc;
import io.slingr.svcs.configurations.Configuration;
import io.slingr.svcs.configurations.SvcDefinitions;
import io.slingr.svcs.configurations.SvcsProperties;
import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.exceptions.ErrorCode;
import io.slingr.svcs.framework.annotations.classes.FunctionResponseType;
import io.slingr.svcs.framework.annotations.classes.MethodAccessorType;
import io.slingr.svcs.framework.annotations.classes.MethodParameterType;
import io.slingr.svcs.framework.annotations.classes.WebServiceResponseType;
import io.slingr.svcs.services.*;
import io.slingr.svcs.services.datastores.DataStore;
import io.slingr.svcs.services.exchange.Parameter;
import io.slingr.svcs.services.logs.SvcLayout;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.tests.SvcTests;
import io.slingr.svcs.utils.tests.ExtensionBrokerMock;
import io.slingr.svcs.ws.WebServices;
import io.slingr.svcs.ws.WebServicesProcessor;
import io.slingr.svcs.ws.exchange.FunctionRequest;
import io.slingr.svcs.ws.exchange.WebServiceRequest;
import io.slingr.svcs.ws.exchange.WebServiceResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Base module that implements utilities for all {@link Svc} implementations
 *
 * <p>Created by lefunes on 12/03/18.
 */
public class BaseModule implements ISvc, IBaseSvc {
    private static final Logger logger = LoggerFactory.getLogger(BaseModule.class);

    private ISvc svc = null;
    private SvcsProperties properties = null;
    private SvcDefinitions definitions = null;
    private WebServices webServicesServer = null;
    private List<SvcLifecycleListener> systemLifecycleListeners = new ArrayList<>();
    private List<SvcLifecycleListener> lifecycleListeners = new ArrayList<>();

    // services
    private ExtensionBrokerApi extensionBroker;
    private Events events;
    private AppLogs appLogs;
    private Locks locks;
    private Files files;
    private DataStores dataStores;
    private ESConfigurations configurations;
    private AppUsers appUsers;
    private Management management;

    // cache of methods
    private final Map<String, Method> declaredMethodsCache = Collections.synchronizedMap(new HashMap<>());

    // functions definitions
    private final Map<String, RegisteredFunction> functions = new HashMap<>();
    private final ReentrantReadWriteLock functionsLock = new ReentrantReadWriteLock();

    // web services definitions
    private final List<RegisteredWebService> webServices = new ArrayList<>();
    private final ReentrantReadWriteLock webServicesLock = new ReentrantReadWriteLock();

    // configuration flags
    private final ReentrantLock configureLock = new ReentrantLock();
    private final AtomicBoolean starting = new AtomicBoolean(false);
    private final AtomicBoolean configured = new AtomicBoolean(false);
    private final AtomicBoolean extensionBrokerApiConfigured = new AtomicBoolean(false);
    private final AtomicBoolean webServicesConfigured = new AtomicBoolean(false);

    // interceptors
    private final AtomicBoolean enabledConfiguratorInterceptor = new AtomicBoolean(false);
    private final AtomicBoolean enabledFunctionInterceptor = new AtomicBoolean(false);
    private final AtomicBoolean enabledWebServiceInterceptor = new AtomicBoolean(false);

    /**
     * Builds the base service instance
     */
    public BaseModule(){}

    /**
     * Add a priority listener to be called when the a lifecycle event is emitted. These priority listener are called
     * before the other listeners.
     *
     * @param listener listener to subscribe
     */
    void addSystemLifecycleListener(SvcLifecycleListener listener){
        if(listener != null && !lifecycleListeners.contains(listener) && !systemLifecycleListeners.contains(listener)){
            systemLifecycleListeners.add(listener);
        }
    }

    /**
     * Add a listener to be called when the a lifecycle event is emitted
     *
     * @param listener listener to subscribe
     */
    public void addLifecycleListener(SvcLifecycleListener listener){
        if(listener != null && !lifecycleListeners.contains(listener) && !systemLifecycleListeners.contains(listener)){
            lifecycleListeners.add(listener);
        }
    }

    /**
     * Configure the service instance using the given parameters.
     *
     * @param propertyFile property file used as source of properties. This is used primary on development time.
     * @param svc object that implements the service.
     */
    public final void configure(String propertyFile, ISvc svc){
        configureLock.lock();
        try {
            if (!starting.get()) {
                if(svc == null){
                    throw new IllegalArgumentException("Invalid service object");
                }
                this.svc = svc;

                if (StringUtils.isBlank(propertyFile)) {
                    logger.info(String.format("Configuring service [%s]...", svc.getClass().getSimpleName()));
                } else {
                    logger.info(String.format("Configuring service [%s] from [%s]...", svc.getClass().getSimpleName(), propertyFile));
                }
                starting.set(true);
            }
            if (!configured.get()) {
                // start configuration
                final Configuration configuration = new Configuration(propertyFile);
                this.definitions = configuration.definitions();
                this.properties = configuration.properties();

                configured.set(true);

                // configure logger
                SvcLayout.setApplication(this.properties.getApplicationName());
                SvcLayout.setEnvironment(this.properties.getEnvironment());
                SvcLayout.setSvc(this.properties.getSvcName());
                SvcLayout.setPodId(this.properties.getPodId());
                SvcLayout.setComponent(String.format("svc-%s", this.definitions().getType()));
                if(this.properties().isLocalDeployment()){
                    SvcLayout.setDeployment("local");
                } else {
                    SvcLayout.setDeployment("cloud");
                }

                logger.info(String.format("Starting service [%s][%s][%s]", this.properties.getApplicationName(), this.properties.getEnvironment(), this.properties.getSvcName()));

                if (this.properties.isDebug()) {
                    // show properties and definitions if the service is started in debug mode
                    logger.info(String.format("%s --------------", Svc.DEBUG));
                    logger.info(String.format("%s Definitions: %s", Svc.DEBUG, this.definitions.toJson()));
                    logger.info(String.format("%s Properties: %s", Svc.DEBUG, this.properties.toJson()));
                }

                for (SvcLifecycleListener listener : systemLifecycleListeners) {
                    listener.svcConfigured();
                }
                for (SvcLifecycleListener listener : lifecycleListeners) {
                    listener.svcConfigured();
                }

                if (this.properties.isDebug()) {
                    logger.info(String.format("%s --------------", Svc.DEBUG));
                }
            }

            if(!extensionBrokerApiConfigured.get()) {
                // start extension broker
                if(!this.properties.isTestingMode()) {
                    this.extensionBroker = new ExtensionBroker(
                            this.properties.getSvcsServicesApi(),
                            this.properties.getToken(),
                            this.properties.getSvcsServicesApiVersion()
                    );
                } else {
                    // stating the testing mode using a mock of the Extension Broker
                    logger.info(String.format("%s --------------", SvcTests.TEST));
                    logger.info(String.format("%s Testing mode ", SvcTests.TEST));
                    this.extensionBroker = new ExtensionBrokerMock();
                    logger.info(String.format("%s --------------", SvcTests.TEST));
                }

                // initialize helper managers
                this.events = new Events(this.extensionBroker, this.properties.isDebug());
                this.appLogs = new AppLogs(this.extensionBroker, this.properties.isDebug());
                this.locks = new Locks(this.extensionBroker, this.properties.isDebug());
                this.files = new Files(this.extensionBroker, this.properties.isDebug());
                this.dataStores = new DataStores(this.extensionBroker, this.definitions.getDataStoresNames(), this.properties.isDebug());
                this.configurations = new ESConfigurations(this.extensionBroker, this.properties.isDebug());
                this.appUsers = new AppUsers(this.extensionBroker, this.properties.isDebug());
                this.management = new Management(this.extensionBroker);

                extensionBrokerApiConfigured.set(true);

                logger.info("Checking Extension Broker connection...");
                try {
                    final Json configuration = this.extensionBroker.getConfiguration();
                    if(configuration != null) {
                        final boolean usingProxy = configuration.is(Parameter.CONFIGURATION_PROXY, false);
                        properties().setUsingProxy(usingProxy);

                        final String webServiceUri = configuration.string(Parameter.CONFIGURATION_WEB_SERVICE_URI);
                        if(StringUtils.isNotBlank(webServiceUri)) {
                            properties().setDefaultWebServicesUri(webServiceUri);
                            logger.info(String.format("Services is working through the proxy [%s]", webServiceUri));
                        }
                    }
                } catch (Exception ex){
                    logger.warn(String.format("Extension Broker  app is not ready yet: %s", ex.getMessage()), ex);
                }

                logger.info("Extension Broker  API enabled");

                for (SvcLifecycleListener listener : systemLifecycleListeners) {
                    listener.extensionBrokerConfigured();
                }
                for (SvcLifecycleListener listener : lifecycleListeners) {
                    listener.extensionBrokerConfigured();
                }

                if (this.properties.isDebug()) {
                    logger.info(String.format("%s --------------", Svc.DEBUG));
                }
            }

            if(!webServicesConfigured.get()) {
                // start web services
                final WebServicesProcessor webServicesProcessor = new WebServicesProcessor(this, this.properties.getToken(), this.properties.isLocalDeployment(), this.properties.isDebug());

                webServicesServer = new WebServices(this.properties, webServicesProcessor);
                webServicesServer.start();

                webServicesConfigured.set(true);

                for (SvcLifecycleListener listener : systemLifecycleListeners) {
                    listener.webServicesConfigured();
                }
                for (SvcLifecycleListener listener : lifecycleListeners) {
                    listener.webServicesConfigured();
                }

                if (this.properties.isDebug()) {
                    logger.info(String.format("%s --------------", Svc.DEBUG));
                }
            }
        } finally {
            configureLock.unlock();
        }
    }

    /**
     * Function called when the services are started
     */
    public final void finishedStart(){
        appLogs().info(String.format("Services [%s] started", this.properties.getSvcName()));
        for (SvcLifecycleListener listener : systemLifecycleListeners) {
            listener.svcStarted();
        }
        if (this.properties.isDebug()) {
            logger.info(String.format("%s --------------", Svc.DEBUG));
        }

        for (SvcLifecycleListener listener : lifecycleListeners) {
            listener.svcStarted();
        }

        if (properties.isUsingProxy()) {
            // if we are using the proxy we will clear cache after restarting the services
            // this is to make development of services easier so developers just need to
            // restart the services when there are changes
            management.clearCache();
        }
    }

    /**
     * Throws an exception if the services is not already configured
     *
     * @throws SvcException if the services is not already configured
     */
    private void errorIfNotConfigured() throws SvcException {
        if(!configured.get()){
            throw SvcException.permanent(ErrorCode.CLIENT, "Services is not already configured.");
        }
    }

    /**
     * Throws an exception if the Extension Broker services are not already configured
     *
     * @throws SvcException if the Extension Broker services are not already configured
     */
    private void errorIfExtensionBrokerNotConfigured() throws SvcException {
        if(!extensionBrokerApiConfigured.get()){
            throw SvcException.permanent(ErrorCode.CLIENT, "Extension Broker services are not already configured.");
        }
        if(extensionBroker == null){
            throw SvcException.permanent(ErrorCode.CLIENT, "Extension Broker services are not properly configured.");
        }
    }

    /**
     * Throws an exception if the users are not ready to be used yet.
     */
    private void errorIfWebServicesNotConfigured(){
        if(!this.webServicesConfigured.get()) {
            throw SvcException.permanent(ErrorCode.CLIENT, "Web service is not ready to be used yet. Use it in Svc.webServicesConfigured() hook or after its execution.");
        }
    }

    /**
     * Starts the stopping process of the services
     *
     * @param cause cause of the termination
     */
    @Override
    public final void stopSvc(final String cause){
        webServicesServer.stop();
        for (SvcLifecycleListener listener : systemLifecycleListeners) {
            listener.svcStopped(cause);
        }

        // hook to permit to the Services implementations to execute code when a terminate signal is received
        for (SvcLifecycleListener listener : lifecycleListeners) {
            listener.svcStopped(cause);
        }

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            logger.info(String.format("Services stopped [%s]", cause));
            System.exit(0);
        }, 2, TimeUnit.SECONDS);
    }

    @Override
    public SvcsProperties properties() {
        return properties;
    }

    @Override
    public SvcDefinitions definitions() {
        return definitions;
    }

    /**
     * Returns the extension broker implementation
     *
     * @return extension broker implementation
     */
    public ExtensionBrokerApi getExtensionBroker() {
        errorIfExtensionBrokerNotConfigured();
        return this.extensionBroker;
    }

    @Override
    public Events events() {
        errorIfExtensionBrokerNotConfigured();
        return this.events;
    }

    @Override
    public AppLogs appLogs() {
        errorIfExtensionBrokerNotConfigured();
        return this.appLogs;
    }

    @Override
    public Locks locks() {
        errorIfExtensionBrokerNotConfigured();
        return this.locks;
    }

    @Override
    public Files files() {
        errorIfExtensionBrokerNotConfigured();
        return this.files;
    }

    @Override
    public ESConfigurations svcServicesConfigurations() {
        errorIfExtensionBrokerNotConfigured();
        return this.configurations;
    }

    @Override
    public AppUsers appUsers() {
        errorIfExtensionBrokerNotConfigured();
        return this.appUsers;
    }

    @Override
    public Management management() {
        errorIfExtensionBrokerNotConfigured();
        return this.management;
    }

    @Override
    public DataStores dataStores() {
        errorIfExtensionBrokerNotConfigured();
        return this.dataStores;
    }

    @Override
    public DataStore dataStore(String dataStoreName) throws SvcException {
        errorIfExtensionBrokerNotConfigured();
        return this.dataStores.getDataStore(dataStoreName);
    }

    @Override
    public DataStore userDataStore() throws SvcException {
        errorIfExtensionBrokerNotConfigured();
        return this.dataStores.getUserDataStore();
    }

    @Override
    public Json toJson() {
        errorIfNotConfigured();
        return Json.map()
                .set("properties", this.properties.toJson())
                .set("definitions", this.definitions.toJson());
    }

    /**
     * Get the list of properties and metadata of services used when the svcs is connected through a Proxy svc.
     *
     * @return a map that contains configuration and metadata about the services
     */
    @Override
    public final Json getConfiguration() throws SvcException {
        errorIfNotConfigured();
        Json response;
        try {
            final Json svcConfiguration = this.properties.toJson();
            if (svcConfiguration.contains(Configuration.PROPERTY_TOKEN)) {
                svcConfiguration.set(Configuration.PROPERTY_TOKEN, "-");
            }
            response = Json.map()
                    .set(Parameter.METADATA_APP, this.properties.getApplicationName())
                    .set(Parameter.METADATA_NAME, this.properties.getSvcName())
                    .set(Parameter.METADATA_ENV, this.properties.getEnvironment())
                    .set(Parameter.METADATA_PER_USER, this.definitions.isPerUserSvc())
                    .setIfNotNull(Parameter.METADATA_CONFIGURATION, svcConfiguration)
                    .setIfNotEmpty(Parameter.METADATA_API_VERSION, this.definitions.getApiVersion())
                    .setIfNotNull(Parameter.METADATA_DATA_STORES, this.definitions.getDataStores())
                    .setIfNotNull(Parameter.METADATA_FUNCTIONS, this.definitions.getFunctions())
                    .setIfNotNull(Parameter.METADATA_EVENTS, this.definitions.getEvents())
                    .setIfNotEmpty(Parameter.METADATA_HELP_URL, this.definitions.getConfigurationHelpUrl())
                    .setIfNotEmpty(Parameter.METADATA_CONF, this.definitions.getUIConfiguration())
                    .setIfNotEmpty(Parameter.METADATA_USER_CONF, this.definitions.getUIUserConfiguration())
                    .setIfNotEmpty(Parameter.METADATA_USER_CONF_BUTTONS, this.definitions.getUIUserConfigurationButtons());
        } catch (SvcException e) {
            throw e;
        } catch (Exception ex) {
            throw SvcException.permanent(ErrorCode.CLIENT, String.format("Exception when process configuration: %s", ex.getMessage()), ex);
        }

        if(enabledConfiguratorInterceptor.get()){
            // execute function interceptor
            try {
                response = configurationInterceptor(response);
            } catch (SvcException e) {
                throw e;
            } catch (Exception ex) {
                throw SvcException.permanent(ErrorCode.CLIENT, String.format("Exception when process configuration on interceptor: %s", ex.getMessage()), ex);
            }
        }
        return response;
    }

    /**
     * Register a Slingr function to be executed when arrives the platform requests
     *
     * @param function function definition
     * @param throwExceptionIfInvalid true if an exception is thrown when the function is invalid.
     */
    public final void registerFunction(RegisteredFunction function, Boolean throwExceptionIfInvalid){
        functionsLock.writeLock().lock();
        try {
            if (function == null) {
                throw SvcException.permanent(ErrorCode.ARGUMENT, "Invalid function definition");
            }
            if (StringUtils.isBlank(function.getName())) {
                throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("Invalid function name [%s]", function.getName()));
            }
            final String functionName = function.getName().trim();
            if (functions.containsKey(functionName)) {
                throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("Function [%s] for method [%s] is already defined for method [%s]", functionName, function.getMethod(), functions.get(functionName).getMethod()));
            }
            if (StringUtils.isBlank(function.getMethod())) {
                throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("Invalid function method [%s] for function [%s]", function.getMethod(), functionName));
            }

            if (function.getAccessorType() == null || function.getResponseType() == null || function.getParameterType() == null) {
                throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("Invalid definitions for function [%s]: accessor [%s] - response [%s] - parameter [%s]", functionName, function.getAccessorType(), function.getResponseType(), function.getParameterType()));
            }

            // check function name on definitions
            if(!definitions().isValidFunction(functionName)){
                if(throwExceptionIfInvalid == null || throwExceptionIfInvalid) {
                    throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("Invalid function name [%s] because is not defined on the appService.json file", functionName));
                } else {
                    if(this.properties.isDebug()){
                        logger.info(String.format("%s Auto generated function [%s] is disabled because is not defined on the appService.json file", Svc.DEBUG, functionName));
                    }
                    return;
                }
            }

            // add function definition
            functions.put(functionName, function);
        } finally {
            functionsLock.writeLock().unlock();
        }
    }

    @Override
    public final Json executeFunction(FunctionRequest request) throws SvcException {
        if(request == null || StringUtils.isBlank(request.getFunctionName())){
            throw SvcException.permanent(ErrorCode.ARGUMENT, "Empty function name").returnCode(400);
        }
        if(enabledFunctionInterceptor.get()){
            // execute function interceptor
            try {
                final Json response = executeOnInterceptor(request);
                if(response != null){
                    return response;
                }
            } catch (SvcException e) {
                throw e;
            } catch (Exception e) {
                logger.warn(String.format("An error happens when try to execute the interceptor for function [%s]: %s", request.getFunctionName(), e.getMessage()), e);
                String msg = e.getMessage();
                if (StringUtils.isBlank(msg) || "null".equalsIgnoreCase(msg) || "undefined".equalsIgnoreCase(msg)) {
                    msg = String.format("An error happens when try to execute the interceptor for function [%s]", request.getFunctionName());
                }
                throw SvcException.permanent(ErrorCode.GENERAL, msg, e).returnCode(500);
            }

            logger.warn(String.format("The function interceptor returns a null response, the request [%s] will be processed as a regular function.", request.getFunctionName()));
        }

        RegisteredFunction function;
        functionsLock.readLock().lock();
        try {
            function = functions.get(request.getFunctionName());
        } catch (Exception ex) {
            function = null;
        } finally {
            functionsLock.readLock().unlock();
        }

        if (function == null) {
            if (definitions().isValidFunction(request.getFunctionName())) {
                final String error = String.format("Function [%s] is defined on the appService.json file but not implemented", request.getFunctionName());
                logger.error(error);
                throw SvcException.permanent(ErrorCode.ARGUMENT, error).returnCode(500);
            } else {
                final String error = String.format("Function [%s] is not defined for the services", request.getFunctionName());
                logger.error(error);
                throw SvcException.permanent(ErrorCode.CLIENT, error).returnCode(400);
            }
        }

        // Valid function request, execute java method
        try {
            final Method method;

            final Method declaredMethod = declaredMethodsCache.getOrDefault(function.getName(), null);
            if (declaredMethod != null) {
                method = declaredMethod;
            } else {
                final Class<?> svcClass = function.getSvcClass() != null ? function.getSvcClass() : this.svc.getClass();
                if (function.getParameterType() == MethodParameterType.STRING) {
                    method = svcClass.getDeclaredMethod(function.getMethod(), String.class);
                } else if (function.getParameterType() == MethodParameterType.JSON) {
                    method = svcClass.getDeclaredMethod(function.getMethod(), Json.class);
                } else if (function.getParameterType() == MethodParameterType.REQUEST) {
                    method = svcClass.getDeclaredMethod(function.getMethod(), FunctionRequest.class);
                } else if (function.getParameterType() == MethodParameterType.OBJECT) {
                    method = svcClass.getDeclaredMethod(function.getMethod(), Object.class);
                } else {
                    method = svcClass.getMethod(function.getMethod());
                }
            }

            if (method == null) {
                logger.error(String.format("Method [%s] for function [%s] is not defined for the services", function.getMethod(), function.getName()));
                throw SvcException.permanent(ErrorCode.CLIENT, String.format("Function [%s] is not implemented on the services", function.getName())).returnCode(500);
            } else {
                declaredMethodsCache.put(function.getName(), method);
            }

            if (function.getAccessorType() == MethodAccessorType.PRIVATE) {
                method.setAccessible(true);
            }

            Object response;
            if (function.getParameterType() == MethodParameterType.STRING) {
                response = method.invoke(this.svc, request.getParams() != null ? request.getParams().toString() : null);
            } else if (function.getParameterType() == MethodParameterType.JSON) {
                response = method.invoke(this.svc, request.getJsonParams());
            } else if (function.getParameterType() == MethodParameterType.REQUEST) {
                response = method.invoke(this.svc, request);
            } else if (function.getParameterType() == MethodParameterType.OBJECT) {
                response = method.invoke(this.svc, request.getParams());
            } else {
                response = method.invoke(this.svc);
            }

            if (function.getAccessorType() == MethodAccessorType.PRIVATE) {
                method.setAccessible(false);
            }

            if (response == null || function.getResponseType() == FunctionResponseType.VOID) {
                return Json.map();
            } else if (function.getResponseType() == FunctionResponseType.OTHER) {
                return Json.map().setIfNotEmpty(Parameter.PARAMS_BODY, response);
            } else {
                // JSON response
                return Json.fromObject(response);
            }
        } catch (SvcException e) {
            throw e;
        } catch (InvocationTargetException e) {
            Throwable tw = e.getTargetException();
            String msg = tw != null ? tw.getMessage() : e.getMessage();
            if (StringUtils.isBlank(msg) || "null".equalsIgnoreCase(msg) || "undefined".equalsIgnoreCase(msg)) {
                msg = String.format("An error happens when try to execute the function [%s]", function.getName());
                logger.warn(msg, tw);
            } else {
                logger.warn(String.format("An error happens when try to execute the function [%s]: %s", function.getName(), msg), tw);
            }
            if (tw instanceof SvcException) {
                throw (SvcException) tw;
            }
            throw SvcException.permanent(ErrorCode.GENERAL, msg, tw).returnCode(500);
        } catch (Exception e) {
            logger.warn(String.format("An error happens when try to execute the function [%s]: %s", function.getName(), e.getMessage()), e);
            String msg = e.getMessage();
            if (StringUtils.isBlank(msg) || "null".equalsIgnoreCase(msg) || "undefined".equalsIgnoreCase(msg)) {
                msg = String.format("An error happens when try to execute the function [%s]", function.getName());
            }
            throw SvcException.permanent(ErrorCode.GENERAL, msg, e).returnCode(500);
        }
    }

    /**
     * Register a web service to be executed when arrives request to the web service server
     *
     * @param webService web service definition
     */
    public final void registerWebService(RegisteredWebService webService){
        webServicesLock.writeLock().lock();
        try {
            if (webService == null) {
                throw SvcException.permanent(ErrorCode.ARGUMENT, "Invalid web service definition");
            }
            if (StringUtils.isBlank(webService.getPath())) {
                throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("Invalid web service path [%s]", webService.getPath()));
            }
            if (webService.getRestMethod() == null) {
                throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("Invalid web service rest method [%s]", webService.getRestMethod()));
            }
            if (StringUtils.isBlank(webService.getMethod())) {
                throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("Invalid function method [%s] for web service", webService.getMethod()));
            }

            if (webService.getAccessorType() == null || webService.getResponseType() == null || webService.getParameterType() == null) {
                throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("Invalid definitions for web service: accessor [%s] - response [%s] - parameter [%s]", webService.getAccessorType(), webService.getResponseType(), webService.getParameterType()));
            }

            // add web service definition
            webServices.add(webService);

        } finally {
            webServicesLock.writeLock().unlock();
        }
    }

    @Override
    public final WebServiceResponse executeWebServices(WebServiceRequest request) throws SvcException {
        if(request == null){
            throw SvcException.permanent(ErrorCode.ARGUMENT, "Empty web service name").returnCode(400);
        }

        // if the interceptor is not enabled or fails
        RegisteredWebService webService = null;
        webServicesLock.readLock().lock();
        try {
            final List<RegisteredWebService> validWebServices = webServices.stream()
                    .filter(ws -> request.getMethod() == ws.getRestMethod())
                    .filter(ws -> ws.isValidRoute(request.getPath()))
                    .sorted(RegisteredWebService::compareTo)
                    .collect(Collectors.toList());

            if (validWebServices != null && !validWebServices.isEmpty()) {
                if (validWebServices.size() > 1) {
                    logger.info(String.format("More than one valid routes for [%s %s]: %s", request.getMethod().name(), request.getPath(),
                            validWebServices.stream()
                                    .map(ws -> String.format("[%s %s %s]", ws.getRestMethod().name(), ws.getPath(), ws.getMethod()))
                                    .reduce("", (s, s2) -> s + " " + s2)
                    ));
                }

                webService = validWebServices.get(0);
            }
        } catch (Exception ex) {
            webService = null;
        } finally {
            webServicesLock.readLock().unlock();
        }

        if (webService == null) {
            if(enabledFunctionInterceptor.get()){
                // execute web services interceptor
                try {
                    final WebServiceResponse webServiceResponse = executeOnInterceptor(request);
                    if(webServiceResponse != null){
                        return webServiceResponse;
                    }
                } catch (SvcException e) {
                    throw e;
                } catch (Exception e) {
                    logger.warn(String.format("An error happens when try to execute the interceptor for web service [%s %s]: %s", request.getMethod().name(), request.getPath(), e.getMessage()), e);
                    String msg = e.getMessage();
                    if (StringUtils.isBlank(msg) || "null".equalsIgnoreCase(msg) || "undefined".equalsIgnoreCase(msg)) {
                        msg = String.format("An error happens when try to execute the interceptor for web service [%s %s]", request.getMethod().name(), request.getPath());
                    }
                    throw SvcException.permanent(ErrorCode.GENERAL, msg, e).returnCode(500);
                }

                logger.info(String.format("The web service interceptor returns a null response for the request [%s %s].", request.getMethod().name(), request.getPath()));
            }
            throw SvcException.permanent(ErrorCode.ARGUMENT, "Web service not found").returnCode(404);
        }

        if (this.properties.isDebug()) {
            logger.info(String.format("%s selected route [%s %s]: method [%s]", Svc.DEBUG, webService.getRestMethod().name(), webService.getPath(), webService.getMethod()));
        }

        // fill path variables
        final Json variables = webService.getPathVariables(request.getPath());
        if (variables.isNotEmpty()) {
            variables.forEachMap((s, o) -> request.setPathVariable(s, o != null ? o.toString() : ""));
        }

        // Valid web service request, execute java method
        try {
            final Method method;

            final Method declaredMethod = declaredMethodsCache.getOrDefault(webService.getName(), null);
            if (declaredMethod != null) {
                method = declaredMethod;
            } else {
                final Class<?> svcClass = webService.getSvcClass() != null ? webService.getSvcClass() : this.svc.getClass();
                if (webService.getParameterType() == MethodParameterType.STRING) {
                    method = svcClass.getDeclaredMethod(webService.getMethod(), String.class);
                } else if (webService.getParameterType() == MethodParameterType.JSON) {
                    method = svcClass.getDeclaredMethod(webService.getMethod(), Json.class);
                } else if (webService.getParameterType() == MethodParameterType.REQUEST) {
                    method = svcClass.getDeclaredMethod(webService.getMethod(), WebServiceRequest.class);
                } else if (webService.getParameterType() == MethodParameterType.OBJECT) {
                    method = svcClass.getDeclaredMethod(webService.getMethod(), Object.class);
                } else {
                    method = svcClass.getMethod(webService.getMethod());
                }
            }

            if (method == null) {
                logger.error(String.format("Method [%s] for web service [%s %s] is not defined for the svc", webService.getMethod(), webService.getRestMethod().name(), webService.getPath()));
                throw SvcException.permanent(ErrorCode.CLIENT, String.format("Web service [%s %s] is not implemented on the svc", webService.getRestMethod().name(), webService.getPath())).returnCode(500);
            } else {
                declaredMethodsCache.put(webService.getName(), method);
            }

            if (webService.getAccessorType() == MethodAccessorType.PRIVATE) {
                method.setAccessible(true);
            }

            Object response;
            if (webService.getParameterType() == MethodParameterType.STRING) {
                response = method.invoke(this.svc, request.getBody() != null ? request.getBody().toString() : null);
            } else if (webService.getParameterType() == MethodParameterType.JSON) {
                response = method.invoke(this.svc, request.getJsonBody());
            } else if (webService.getParameterType() == MethodParameterType.REQUEST) {
                response = method.invoke(this.svc, request);
            } else if (webService.getParameterType() == MethodParameterType.OBJECT) {
                response = method.invoke(this.svc, request.getBody());
            } else {
                response = method.invoke(this.svc);
            }

            if (webService.getAccessorType() == MethodAccessorType.PRIVATE) {
                method.setAccessible(false);
            }

            // convert response
            if (response == null || webService.getResponseType() == WebServiceResponseType.VOID) {
                return new WebServiceResponse(Json.map());
            } else if (webService.getResponseType() == WebServiceResponseType.STRING) {
                return new WebServiceResponse(response.toString(), Json.map().set(Parameter.CONTENT_TYPE, ContentType.TEXT_PLAIN.getMimeType()));
            } else if (webService.getResponseType() == WebServiceResponseType.RESPONSE) {
                if (response instanceof WebServiceResponse) {
                    return (WebServiceResponse) response;
                }
                return new WebServiceResponse(Json.map());
            } else if (webService.getResponseType() == WebServiceResponseType.JSON) {
                return new WebServiceResponse(Json.fromObject(response));
            } else {
                // Object response
                return new WebServiceResponse(response);
            }
        } catch (SvcException e) {
            throw e;
        } catch (InvocationTargetException e) {
            Throwable tw = e.getTargetException();
            String msg = tw != null ? tw.getMessage() : e.getMessage();
            if (StringUtils.isBlank(msg) || "null".equalsIgnoreCase(msg) || "undefined".equalsIgnoreCase(msg)) {
                msg = String.format("An error happens when try to execute the web service [%s %s]", webService.getRestMethod().name(), webService.getPath());
                logger.warn(msg, tw);
            } else {
                logger.warn(String.format("An error happens when try to execute the web service [%s %s]: %s", webService.getRestMethod().name(), webService.getPath(), msg), tw);
            }
            if (tw instanceof SvcException) {
                throw (SvcException) tw;
            }
            throw SvcException.permanent(ErrorCode.GENERAL, msg, e).returnCode(500);
        } catch (Exception e) {
            logger.warn(String.format("An error happens when try to execute the web service [%s %s]: %s", webService.getRestMethod().name(), webService.getPath(), e.getMessage()), e);
            String msg = e.getMessage();
            if (StringUtils.isBlank(msg) || "null".equalsIgnoreCase(msg) || "undefined".equalsIgnoreCase(msg)) {
                msg = String.format("An error happens when try to execute the web service [%s %s]", webService.getRestMethod().name(), webService.getPath());
            }
            throw SvcException.permanent(ErrorCode.GENERAL, msg, e).returnCode(500);
        }
    }

    @Override
    public final void enableConfiguratorInterceptor(){
        enabledConfiguratorInterceptor.set(true);
        if(this.properties.isDebug()) {
            logger.info(String.format("%s configurator interceptor enabled", Svc.DEBUG));
        }
    }

    @Override
    public final void enableFunctionInterceptor(){
        enabledFunctionInterceptor.set(true);
        if(this.properties.isDebug()) {
            logger.info(String.format("%s functions interceptor enabled", Svc.DEBUG));
        }
    }

    @Override
    public final void enableWebServicesInterceptor(){
        enabledWebServiceInterceptor.set(true);
        if(this.properties.isDebug()) {
            logger.info(String.format("%s web services interceptor enabled", Svc.DEBUG));
        }
    }

    @Override
    public void setupDefaultExceptionsProperties(int maxRedelivers){
        errorIfWebServicesNotConfigured();
        this.webServicesServer.setupDefaultExceptionsProperties(maxRedelivers);
    }

    @Override
    public void setupPermanentExceptionsProperties(int maxRedelivers, long delay){
        errorIfWebServicesNotConfigured();
        this.webServicesServer.setupPermanentExceptionsProperties(maxRedelivers, delay);
    }

    @Override
    public void setupRetryableExceptionsProperties(int maxRedelivers, long delay){
        errorIfWebServicesNotConfigured();
        this.webServicesServer.setupRetryableExceptionsProperties(maxRedelivers, delay);
    }

    /**
     * Executes the function request on the interceptor processor
     *
     * @param request function request
     * @return function response
     * @throws SvcException exception if an error happened when process the function request
     */
    private Json executeOnInterceptor(FunctionRequest request) throws SvcException {
        if (this.properties.isDebug()) {
            logger.info(String.format("%s function interceptor: function [%s] id [%s]", Svc.DEBUG, request.getFunctionName(), request.getFunctionName()));
        }

        final Object response = functionInterceptor(request);

        Json jsonResponse;
        if (response == null) {
            jsonResponse =  Json.map();
        } else {
            // JSON response
            jsonResponse = Json.fromObject(response, false, true);
            if(jsonResponse == null){
                jsonResponse = Json.map().setIfNotEmpty(Parameter.PARAMS_BODY, response);
            }
        }
        return jsonResponse;
    }

    /**
     * Executes the web service request on the interceptor processor
     *
     * @param request web service request
     * @return web service response
     * @throws SvcException exception if an error happened when process the web service request
     */
    private WebServiceResponse executeOnInterceptor(WebServiceRequest request) throws SvcException {
        if (this.properties.isDebug()) {
            logger.info(String.format("%s web service interceptor: request [%s %s]", Svc.DEBUG, request.getMethod().name(), request.getPath()));
        }

        final Object response = webServicesInterceptor(request);

        // convert response
        WebServiceResponse wsResponse = null;
        if (response == null) {
            wsResponse = new WebServiceResponse(Json.map());
        } else if (response instanceof WebServiceResponse) {
            wsResponse = (WebServiceResponse) response;
        } else {
            final Json jsonResponse = Json.fromObject(response, false, true);
            if(jsonResponse != null){
                wsResponse = new WebServiceResponse(jsonResponse);
            }
            if(wsResponse == null) {
                if (response instanceof String) {
                    wsResponse = new WebServiceResponse(response.toString(), ContentType.TEXT_PLAIN.getMimeType());
                } else {
                    wsResponse = new WebServiceResponse(response);
                }
            }
        }
        return wsResponse;
    }

    @Override
    public final Json configurationInterceptor(Json configuration) throws SvcException {
        return svc.configurationInterceptor(configuration);
    }

    @Override
    public final Object functionInterceptor(FunctionRequest request) throws SvcException {
        return svc.functionInterceptor(request);
    }

    @Override
    public final Object webServicesInterceptor(WebServiceRequest request) throws SvcException {
        return svc.webServicesInterceptor(request);
    }




    /*
        final String path = (String) headers.getOrDefault(Exchange.HTTP_PATH, "/");
        final String method = (String) headers.getOrDefault(Exchange.HTTP_METHOD, "GET");
        final Map<String, String> parameters = ParametersHelper.parse((String) headers.getOrDefault(Exchange.HTTP_QUERY, ""));

        body = BodyConverter.parseHttpBody(body, exchange, logger);

        Object response = customWsProcessor(RestMethod.valueOf(method.toUpperCase()), path, body, headers, parameters);
        if(response == null) {
            response = body;
            headers.put(Exchange.HTTP_RESPONSE_CODE, 404);
        }
        return response;



        if (response == null || function.getResponseType() == FunctionResponseType.VOID) {
            return Json.map();
        } else if (function.getResponseType() == FunctionResponseType.OTHER) {
            return Json.map().setIfNotEmpty(Parameter.PARAMS_BODY, response);
        } else {
            // JSON response
            return Json.fromObject(response);
        }
    */
}
