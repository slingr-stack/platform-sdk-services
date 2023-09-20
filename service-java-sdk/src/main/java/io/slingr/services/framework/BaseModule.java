package io.slingr.services.framework;

import io.slingr.services.Service;
import io.slingr.services.configurations.Configuration;
import io.slingr.services.configurations.ServiceDefinitions;
import io.slingr.services.configurations.ServicesProperties;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.framework.annotations.classes.FunctionResponseType;
import io.slingr.services.framework.annotations.classes.MethodAccessorType;
import io.slingr.services.framework.annotations.classes.MethodParameterType;
import io.slingr.services.framework.annotations.classes.WebServiceResponseType;
import io.slingr.services.services.*;
import io.slingr.services.services.datastores.DataStore;
import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.services.logs.ServiceLayout;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.tests.ExtensionBrokerMock;
import io.slingr.services.utils.tests.ServiceTests;
import io.slingr.services.ws.WebServices;
import io.slingr.services.ws.WebServicesProcessor;
import io.slingr.services.ws.exchange.FunctionRequest;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.ws.exchange.WebServiceResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base module that implements utilities for all {@link Service} implementations
 *
 */
public class BaseModule implements IService, IBaseService {
    private static final Logger logger = LoggerFactory.getLogger(BaseModule.class);

    private IService service = null;
    private ServicesProperties properties = null;
    private ServiceDefinitions definitions = null;
    private WebServices webServicesServer = null;
    private final List<ServiceLifecycleListener> systemLifecycleListeners = new ArrayList<>();
    private final List<ServiceLifecycleListener> lifecycleListeners = new ArrayList<>();

    // services
    private ExtensionBrokerApi extensionBroker;
    private Events events;
    private AppLogs appLogs;
    private Locks locks;
    private Files files;
    private DataStores dataStores;
    private EBConfigurations configurations;
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

    /**
     * Builds the base service instance
     */
    public BaseModule(){}

    /**
     * Add a priority listener to be called when a lifecycle event is emitted.
     * These priority listeners are called before the other listeners.
     *
     * @param listener listener to subscribe
     */
    void addSystemLifecycleListener(ServiceLifecycleListener listener){
        if(listener != null && !lifecycleListeners.contains(listener) && !systemLifecycleListeners.contains(listener)){
            systemLifecycleListeners.add(listener);
        }
    }

    /**
     * Add a listener to be called when a lifecycle event is emitted
     *
     * @param listener listener to subscribe
     */
    public void addLifecycleListener(ServiceLifecycleListener listener){
        if(listener != null && !lifecycleListeners.contains(listener) && !systemLifecycleListeners.contains(listener)){
            lifecycleListeners.add(listener);
        }
    }

    /**
     * Configure the service instance using the given parameters.
     *
     * @param propertyFile property file used as source of properties. This is used primary on development time.
     * @param service object that implements the service.
     */
    public final void configure(String propertyFile, IService service){
        configureLock.lock();
        try {
            if (!starting.get()) {
                if(service == null){
                    throw new IllegalArgumentException("Invalid service object");
                }
                this.service = service;

                if (StringUtils.isBlank(propertyFile)) {
                    logger.info(String.format("Configuring service [%s]...", service.getClass().getSimpleName()));
                } else {
                    logger.info(String.format("Configuring service [%s] from [%s]...", service.getClass().getSimpleName(), propertyFile));
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
                ServiceLayout.setApplication(this.properties.getApplicationName());
                ServiceLayout.setEnvironment(this.properties.getEnvironment());
                ServiceLayout.setService(this.properties.getServiceName());
                ServiceLayout.setPodId(this.properties.getPodId());
                ServiceLayout.setComponent(String.format("service-%s", this.definitions().getType()));
                if(this.properties().isLocalDeployment()){
                    ServiceLayout.setDeployment("local");
                } else {
                    ServiceLayout.setDeployment("cloud");
                }

                logger.info(String.format("Starting service [%s][%s][%s]", this.properties.getApplicationName(), this.properties.getEnvironment(), this.properties.getServiceName()));

                if (this.properties.isDebug()) {
                    // show properties and definitions if the service is started in debug mode
                    logger.info(String.format("%s --------------", Service.DEBUG));
                    logger.info(String.format("%s Definitions: %s", Service.DEBUG, this.definitions.toJson()));
                    logger.info(String.format("%s Properties: %s", Service.DEBUG, this.properties.toJson()));
                }

                for (ServiceLifecycleListener listener : systemLifecycleListeners) {
                    listener.serviceConfigured();
                }
                for (ServiceLifecycleListener listener : lifecycleListeners) {
                    listener.serviceConfigured();
                }

                if (this.properties.isDebug()) {
                    logger.info(String.format("%s --------------", Service.DEBUG));
                }
            }

            if(!extensionBrokerApiConfigured.get()) {
                // start extension broker
                if(!this.properties.isTestingMode()) {
                    this.extensionBroker = new ExtensionBroker(
                            this.properties.getServicesApi(),
                            this.properties.getToken(),
                            this.properties.getServicesApiVersion()
                    );
                } else {
                    // stating the testing mode using the mock of the Extension Broker
                    logger.info(String.format("%s --------------", ServiceTests.TEST));
                    logger.info(String.format("%s Testing mode ", ServiceTests.TEST));
                    this.extensionBroker = new ExtensionBrokerMock();
                    logger.info(String.format("%s --------------", ServiceTests.TEST));
                }

                // initialize helper managers
                this.events = new Events(this.extensionBroker, this.properties.isDebug());
                this.appLogs = new AppLogs(this.extensionBroker, this.properties.isDebug());
                this.locks = new Locks(this.extensionBroker, this.properties.isDebug());
                this.files = new Files(this.extensionBroker, this.properties.isDebug());
                this.dataStores = new DataStores(this.extensionBroker, this.definitions.getDataStoresNames(), this.properties.isDebug());
                this.configurations = new EBConfigurations(this.extensionBroker, this.properties.isDebug());
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

                for (ServiceLifecycleListener listener : systemLifecycleListeners) {
                    listener.extensionBrokerConfigured();
                }
                for (ServiceLifecycleListener listener : lifecycleListeners) {
                    listener.extensionBrokerConfigured();
                }

                if (this.properties.isDebug()) {
                    logger.info(String.format("%s --------------", Service.DEBUG));
                }
            }

            if(!webServicesConfigured.get()) {
                // start web services
                final WebServicesProcessor webServicesProcessor = new WebServicesProcessor(this, this.properties.getToken(), this.properties.isLocalDeployment(), this.properties.isDebug());

                webServicesServer = new WebServices(this.properties, webServicesProcessor);
                webServicesServer.start();

                webServicesConfigured.set(true);

                for (ServiceLifecycleListener listener : systemLifecycleListeners) {
                    listener.webServicesConfigured();
                }
                for (ServiceLifecycleListener listener : lifecycleListeners) {
                    listener.webServicesConfigured();
                }

                if (this.properties.isDebug()) {
                    logger.info(String.format("%s --------------", Service.DEBUG));
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
        appLogs().info(String.format("Services [%s] started", this.properties.getServiceName()));
        for (ServiceLifecycleListener listener : systemLifecycleListeners) {
            listener.serviceStarted();
        }
        if (this.properties.isDebug()) {
            logger.info(String.format("%s --------------", Service.DEBUG));
        }

        for (ServiceLifecycleListener listener : lifecycleListeners) {
            listener.serviceStarted();
        }

        if (properties.isUsingProxy()) {
            // if we are using the proxy, we will clear cache after restarting the services
            // this is to make development of services easier so developers just need to
            // restart the services when there are changes
            management.clearCache();
        }
    }

    /**
     * Throws an exception if the service is not already configured
     *
     * @throws ServiceException if the service is not already configured
     */
    private void errorIfNotConfigured() throws ServiceException {
        if(!configured.get()){
            throw ServiceException.permanent(ErrorCode.CLIENT, "Services is not already configured.");
        }
    }

    /**
     * Throws an exception if the Extension Broker services are not already configured
     *
     * @throws ServiceException if the Extension Broker services are not already configured
     */
    private void errorIfExtensionBrokerNotConfigured() throws ServiceException {
        if(!extensionBrokerApiConfigured.get()){
            throw ServiceException.permanent(ErrorCode.CLIENT, "Extension Broker services are not already configured.");
        }
        if(extensionBroker == null){
            throw ServiceException.permanent(ErrorCode.CLIENT, "Extension Broker services are not properly configured.");
        }
    }

    /**
     * Throws an exception if the users are not ready to be already used.
     */
    private void errorIfWebServicesNotConfigured(){
        if(!this.webServicesConfigured.get()) {
            throw ServiceException.permanent(ErrorCode.CLIENT, "Web service is not ready to be used yet. Use it in Service.webServicesConfigured() hook or after its execution.");
        }
    }

    /**
     * Starts the stopping process of the services
     *
     * @param cause cause of the termination
     */
    @Override
    public final void stopService(final String cause){
        webServicesServer.stop();
        for (ServiceLifecycleListener listener : systemLifecycleListeners) {
            listener.serviceStopped(cause);
        }

        // hook to permit to the Services implementations executing code when a terminated signal is received
        for (ServiceLifecycleListener listener : lifecycleListeners) {
            listener.serviceStopped(cause);
        }

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            logger.info(String.format("Services stopped [%s]", cause));
            System.exit(0);
        }, 2, TimeUnit.SECONDS);
    }

    @Override
    public ServicesProperties properties() {
        return properties;
    }

    @Override
    public ServiceDefinitions definitions() {
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
    public EBConfigurations serviceConfigurations() {
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
    public DataStore dataStore(String dataStoreName) throws ServiceException {
        errorIfExtensionBrokerNotConfigured();
        return this.dataStores.getDataStore(dataStoreName);
    }

    @Override
    public DataStore userDataStore() throws ServiceException {
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
     * Get the list of properties and metadata of services used when the services are connected through a Proxy service.
     *
     * @return a map that contains configuration and metadata about the services
     */
    @Override
    public final Json getConfiguration() throws ServiceException {
        errorIfNotConfigured();
        Json response;
        try {
            final Json serviceConfiguration = this.properties.toJson();
            if (serviceConfiguration.contains(Configuration.PROPERTY_TOKEN)) {
                serviceConfiguration.set(Configuration.PROPERTY_TOKEN, "-");
            }
            response = Json.map()
                    .set(Parameter.METADATA_APP, this.properties.getApplicationName())
                    .set(Parameter.METADATA_NAME, this.properties.getServiceName())
                    .set(Parameter.METADATA_ENV, this.properties.getEnvironment())
                    .set(Parameter.METADATA_PER_USER, this.definitions.isPerUserService())
                    .setIfNotNull(Parameter.METADATA_CONFIGURATION, serviceConfiguration)
                    .setIfNotEmpty(Parameter.METADATA_API_VERSION, this.definitions.getApiVersion())
                    .setIfNotNull(Parameter.METADATA_DATA_STORES, this.definitions.getDataStores())
                    .setIfNotNull(Parameter.METADATA_FUNCTIONS, this.definitions.getFunctions())
                    .setIfNotNull(Parameter.METADATA_EVENTS, this.definitions.getEvents())
                    .setIfNotEmpty(Parameter.METADATA_HELP_URL, this.definitions.getConfigurationHelpUrl())
                    .setIfNotEmpty(Parameter.METADATA_CONF, this.definitions.getUIConfiguration())
                    .setIfNotEmpty(Parameter.METADATA_USER_CONF, this.definitions.getUIUserConfiguration())
                    .setIfNotEmpty(Parameter.METADATA_USER_CONF_BUTTONS, this.definitions.getUIUserConfigurationButtons());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception ex) {
            throw ServiceException.permanent(ErrorCode.CLIENT, String.format("Exception when process configuration: %s", ex.getMessage()), ex);
        }

        if(enabledConfiguratorInterceptor.get()){
            // execute function interceptor
            try {
                response = configurationInterceptor(response);
            } catch (ServiceException e) {
                throw e;
            } catch (Exception ex) {
                throw ServiceException.permanent(ErrorCode.CLIENT, String.format("Exception when process configuration on interceptor: %s", ex.getMessage()), ex);
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
                throw ServiceException.permanent(ErrorCode.ARGUMENT, "Invalid function definition");
            }
            if (StringUtils.isBlank(function.getName())) {
                throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("Invalid function name [%s]", function.getName()));
            }
            final String functionName = function.getName().trim();
            if (functions.containsKey(functionName)) {
                throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("Function [%s] for method [%s] is already defined for method [%s]", functionName, function.getMethod(), functions.get(functionName).getMethod()));
            }
            if (StringUtils.isBlank(function.getMethod())) {
                throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("Invalid function method [%s] for function [%s]", function.getMethod(), functionName));
            }

            if (function.getAccessorType() == null || function.getResponseType() == null || function.getParameterType() == null) {
                throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("Invalid definitions for function [%s]: accessor [%s] - response [%s] - parameter [%s]", functionName, function.getAccessorType(), function.getResponseType(), function.getParameterType()));
            }

            // check function name on definitions
            if(!definitions().isValidFunction(functionName)){
                if(throwExceptionIfInvalid == null || throwExceptionIfInvalid) {
                    throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("Invalid function name [%s] because is not defined on the appService.json file", functionName));
                } else {
                    if(this.properties.isDebug()){
                        logger.info(String.format("%s Auto generated function [%s] is disabled because is not defined on the appService.json file", Service.DEBUG, functionName));
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
    public final Json executeFunction(FunctionRequest request) throws ServiceException {
        if(request == null || StringUtils.isBlank(request.getFunctionName())){
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "Empty function name").returnCode(400);
        }
        if(enabledFunctionInterceptor.get()){
            // execute function interceptor
            try {
                final Json response = executeOnInterceptor(request);
                if(response != null){
                    return response;
                }
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                logger.warn(String.format("An error happens when try to execute the interceptor for function [%s]: %s", request.getFunctionName(), e.getMessage()), e);
                String msg = e.getMessage();
                if (StringUtils.isBlank(msg) || "null".equalsIgnoreCase(msg) || "undefined".equalsIgnoreCase(msg)) {
                    msg = String.format("An error happens when try to execute the interceptor for function [%s]", request.getFunctionName());
                }
                throw ServiceException.permanent(ErrorCode.GENERAL, msg, e).returnCode(500);
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
                throw ServiceException.permanent(ErrorCode.ARGUMENT, error).returnCode(500);
            } else {
                final String error = String.format("Function [%s] is not defined for the services", request.getFunctionName());
                logger.error(error);
                throw ServiceException.permanent(ErrorCode.CLIENT, error).returnCode(400);
            }
        }

        // Valid function request, execute java method
        try {
            final Method method;

            final Method declaredMethod = declaredMethodsCache.getOrDefault(function.getName(), null);
            if (declaredMethod != null) {
                method = declaredMethod;
            } else {
                final Class<?> serviceClass = function.getServiceClass() != null ? function.getServiceClass() : this.service.getClass();
                if (function.getParameterType() == MethodParameterType.STRING) {
                    method = serviceClass.getDeclaredMethod(function.getMethod(), String.class);
                } else if (function.getParameterType() == MethodParameterType.JSON) {
                    method = serviceClass.getDeclaredMethod(function.getMethod(), Json.class);
                } else if (function.getParameterType() == MethodParameterType.REQUEST) {
                    method = serviceClass.getDeclaredMethod(function.getMethod(), FunctionRequest.class);
                } else if (function.getParameterType() == MethodParameterType.OBJECT) {
                    method = serviceClass.getDeclaredMethod(function.getMethod(), Object.class);
                } else {
                    method = serviceClass.getMethod(function.getMethod());
                }
            }

            try {
                declaredMethodsCache.put(function.getName(), method);
            } catch (Exception e) {
                logger.error(String.format("Method [%s] for function [%s] is not defined for the services: [%s]", function.getMethod(), function.getName(), e.getMessage()));
                throw ServiceException.permanent(ErrorCode.CLIENT, String.format("Function [%s] is not implemented on the services", function.getName())).returnCode(500);
            }

            if (function.getAccessorType() == MethodAccessorType.PRIVATE) {
                method.setAccessible(true);
            }

            Object response;
            if (function.getParameterType() == MethodParameterType.STRING) {
                response = method.invoke(this.service, request.getParams() != null ? request.getParams().toString() : null);
            } else if (function.getParameterType() == MethodParameterType.JSON) {
                response = method.invoke(this.service, request.getJsonParams());
            } else if (function.getParameterType() == MethodParameterType.REQUEST) {
                response = method.invoke(this.service, request);
            } else if (function.getParameterType() == MethodParameterType.OBJECT) {
                response = method.invoke(this.service, request.getParams());
            } else {
                response = method.invoke(this.service);
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
        } catch (ServiceException e) {
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
            if (tw instanceof ServiceException) {
                throw (ServiceException) tw;
            }
            throw ServiceException.permanent(ErrorCode.GENERAL, msg, tw).returnCode(500);
        } catch (Exception e) {
            logger.warn(String.format("An error happens when try to execute the function [%s]: %s", function.getName(), e.getMessage()), e);
            String msg = e.getMessage();
            if (StringUtils.isBlank(msg) || "null".equalsIgnoreCase(msg) || "undefined".equalsIgnoreCase(msg)) {
                msg = String.format("An error happens when try to execute the function [%s]", function.getName());
            }
            throw ServiceException.permanent(ErrorCode.GENERAL, msg, e).returnCode(500);
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
                throw ServiceException.permanent(ErrorCode.ARGUMENT, "Invalid web service definition");
            }
            if (StringUtils.isBlank(webService.getPath())) {
                throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("Invalid web service path [%s]", webService.getPath()));
            }
            if (webService.getRestMethod() == null) {
                throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("Invalid web service rest method [%s]", webService.getRestMethod()));
            }
            if (StringUtils.isBlank(webService.getMethod())) {
                throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("Invalid function method [%s] for web service", webService.getMethod()));
            }

            if (webService.getAccessorType() == null || webService.getResponseType() == null || webService.getParameterType() == null) {
                throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("Invalid definitions for web service: accessor [%s] - response [%s] - parameter [%s]", webService.getAccessorType(), webService.getResponseType(), webService.getParameterType()));
            }

            // add web service definition
            webServices.add(webService);

        } finally {
            webServicesLock.writeLock().unlock();
        }
    }

    @Override
    public final WebServiceResponse executeWebServices(WebServiceRequest request) throws ServiceException {
        if(request == null){
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "Empty web service name").returnCode(400);
        }

        // if the interceptor is not enabled or fails
        RegisteredWebService webService = null;
        webServicesLock.readLock().lock();
        try {
            final List<RegisteredWebService> validWebServices = webServices.stream()
                    .filter(ws -> request.getMethod() == ws.getRestMethod())
                    .filter(ws -> ws.isValidRoute(request.getPath()))
                    .sorted(RegisteredWebService::compareTo)
                    .toList();

            if (!validWebServices.isEmpty()) {
                if (validWebServices.size() > 1) {
                    logger.info(String.format("More than one valid routes for [%s %s]: %s", request.getMethod().name(), request.getPath(),
                            validWebServices.stream()
                                    .map(ws -> String.format("[%s %s %s]", ws.getRestMethod().name(), ws.getPath(), ws.getMethod()))
                                    .reduce("", (s, s2) -> s + " " + s2)
                    ));
                }

                webService = validWebServices.get(0);
            }
        } finally {
            webServicesLock.readLock().unlock();
        }

        if (webService == null) {
            if(enabledFunctionInterceptor.get()){
                // execute web services interceptor
                try {
                    return executeOnInterceptor(request);
                } catch (ServiceException e) {
                    throw e;
                } catch (Exception e) {
                    logger.warn(String.format("An error happens when try to execute the interceptor for web service [%s %s]: %s", request.getMethod().name(), request.getPath(), e.getMessage()), e);
                    String msg = e.getMessage();
                    if (StringUtils.isBlank(msg) || "null".equalsIgnoreCase(msg) || "undefined".equalsIgnoreCase(msg)) {
                        msg = String.format("An error happens when try to execute the interceptor for web service [%s %s]", request.getMethod().name(), request.getPath());
                    }
                    throw ServiceException.permanent(ErrorCode.GENERAL, msg, e).returnCode(500);
                }
            }
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "Web service not found").returnCode(404);
        }

        if (this.properties.isDebug()) {
            logger.info(String.format("%s selected route [%s %s]: method [%s]", Service.DEBUG, webService.getRestMethod().name(), webService.getPath(), webService.getMethod()));
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
                final Class<?> serviceClass = webService.getServiceClass() != null ? webService.getServiceClass() : this.service.getClass();
                if (webService.getParameterType() == MethodParameterType.STRING) {
                    method = serviceClass.getDeclaredMethod(webService.getMethod(), String.class);
                } else if (webService.getParameterType() == MethodParameterType.JSON) {
                    method = serviceClass.getDeclaredMethod(webService.getMethod(), Json.class);
                } else if (webService.getParameterType() == MethodParameterType.REQUEST) {
                    method = serviceClass.getDeclaredMethod(webService.getMethod(), WebServiceRequest.class);
                } else if (webService.getParameterType() == MethodParameterType.OBJECT) {
                    method = serviceClass.getDeclaredMethod(webService.getMethod(), Object.class);
                } else {
                    method = serviceClass.getMethod(webService.getMethod());
                }
            }

            try {
                declaredMethodsCache.put(webService.getName(), method);
            } catch (Exception e) {
                logger.error(String.format("Method [%s] for web service [%s %s] is not defined for the service [%s]", webService.getMethod(), webService.getRestMethod().name(), webService.getPath(), e.getMessage()));
                throw ServiceException.permanent(ErrorCode.CLIENT, String.format("Web service [%s %s] is not implemented on the service", webService.getRestMethod().name(), webService.getPath())).returnCode(500);
            }

            if (webService.getAccessorType() == MethodAccessorType.PRIVATE) {
                method.setAccessible(true);
            }

            Object response;
            if (webService.getParameterType() == MethodParameterType.STRING) {
                response = method.invoke(this.service, request.getBody() != null ? request.getBody().toString() : null);
            } else if (webService.getParameterType() == MethodParameterType.JSON) {
                response = method.invoke(this.service, request.getJsonBody());
            } else if (webService.getParameterType() == MethodParameterType.REQUEST) {
                response = method.invoke(this.service, request);
            } else if (webService.getParameterType() == MethodParameterType.OBJECT) {
                response = method.invoke(this.service, request.getBody());
            } else {
                response = method.invoke(this.service);
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
        } catch (ServiceException e) {
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
            if (tw instanceof ServiceException) {
                throw (ServiceException) tw;
            }
            throw ServiceException.permanent(ErrorCode.GENERAL, msg, e).returnCode(500);
        } catch (Exception e) {
            logger.warn(String.format("An error happens when try to execute the web service [%s %s]: %s", webService.getRestMethod().name(), webService.getPath(), e.getMessage()), e);
            String msg = e.getMessage();
            if (StringUtils.isBlank(msg) || "null".equalsIgnoreCase(msg) || "undefined".equalsIgnoreCase(msg)) {
                msg = String.format("An error happens when try to execute the web service [%s %s]", webService.getRestMethod().name(), webService.getPath());
            }
            throw ServiceException.permanent(ErrorCode.GENERAL, msg, e).returnCode(500);
        }
    }

    @Override
    public final void enableConfiguratorInterceptor(){
        enabledConfiguratorInterceptor.set(true);
        if(this.properties.isDebug()) {
            logger.info(String.format("%s configurator interceptor enabled", Service.DEBUG));
        }
    }

    @Override
    public final void enableFunctionInterceptor(){
        enabledFunctionInterceptor.set(true);
        if(this.properties.isDebug()) {
            logger.info(String.format("%s functions interceptor enabled", Service.DEBUG));
        }
    }

    @Override
    public final void enableWebServicesInterceptor(){
        if(this.properties.isDebug()) {
            logger.info(String.format("%s web services interceptor enabled", Service.DEBUG));
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
     * @throws ServiceException exception if an error happened when process the function request
     */
    private Json executeOnInterceptor(FunctionRequest request) throws ServiceException {
        if (this.properties.isDebug()) {
            logger.info(String.format("%s function interceptor: function [%s] id [%s]", Service.DEBUG, request.getFunctionName(), request.getFunctionName()));
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
     * @throws ServiceException exception if an error happened when process the web service request
     */
    private WebServiceResponse executeOnInterceptor(WebServiceRequest request) throws ServiceException {
        if (this.properties.isDebug()) {
            logger.info(String.format("%s web service interceptor: request [%s %s]", Service.DEBUG, request.getMethod().name(), request.getPath()));
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
    public final Json configurationInterceptor(Json configuration) throws ServiceException {
        return service.configurationInterceptor(configuration);
    }

    @Override
    public final Object functionInterceptor(FunctionRequest request) throws ServiceException {
        return service.functionInterceptor(request);
    }

    @Override
    public final Object webServicesInterceptor(WebServiceRequest request) throws ServiceException {
        return service.webServicesInterceptor(request);
    }
}
