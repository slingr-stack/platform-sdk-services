package io.slingr.services.utils.tests;

import io.slingr.services.Service;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.framework.BaseModule;
import io.slingr.services.framework.IRunner;
import io.slingr.services.services.DataStores;
import io.slingr.services.services.ExtensionBrokerApi;
import io.slingr.services.services.application.AppUser;
import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.services.rest.RestMethod;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.Strings;
import io.slingr.services.ws.exchange.FunctionRequest;
import io.slingr.services.ws.exchange.UploadedFile;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.ws.exchange.WebServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Utility class used to perform tests over the services
 *
 * <p>Created by lefunes on 24/04/18.
 */
public class ServiceTests {
    private static final Logger logger = LoggerFactory.getLogger(ServiceTests.class);

    public static final String TEST = "TEST>";

    private final Service service;
    private final BaseModule baseModule;
    private final ExtensionBrokerMock extensionBroker;

    /**
     * Creates a new instance of the service test utility
     *
     * @param runner runner class of the service
     * @param configurationFile property file used as source of properties. This is used primary on development time.
     */
    public static ServiceTests start(final IRunner runner, final String configurationFile) throws Exception {
        if(runner == null){
            throw new IllegalArgumentException("Runner is null");
        }

        final Service service = runner.startService(configurationFile);
        if(service == null){
            throw new IllegalArgumentException("Service is null");
        }

        return new ServiceTests(service);
    }


    /**
     * Creates a new instance of the service test utility
     *
     * @param service service to test
     */
    public ServiceTests(Service service) {
        if(service == null){
            throw new IllegalArgumentException("Service is null");
        }
        this.service = service;
        try {
            final Method getBaseModule = Service.class.getDeclaredMethod("getBaseModule");
            getBaseModule.setAccessible(true);
            this.baseModule = (BaseModule) getBaseModule.invoke(service);
            getBaseModule.setAccessible(false);
        } catch (Exception ex){
            throw new IllegalStateException(String.format("Exception when find methods to test: %s", ex.getMessage()), ex);
        }

        final ExtensionBrokerApi extensionBrokerApi = this.baseModule.getExtensionBroker();
        if(extensionBrokerApi instanceof ExtensionBrokerMock){
            extensionBroker = (ExtensionBrokerMock) extensionBrokerApi;
        } else {
            logger.warn(String.format("%s --------------------------", TEST));
            logger.warn(String.format("%s Invalid Extension Broker services: Enable the flag '_testing_mode=true' on configuration to start testing mode.", TEST));
            logger.warn(String.format("%s --------------------------", TEST));

            extensionBroker = null;
        }
    }

    /**
     * Gets the service instance
     *
     * @return service instance
     */
    public Service getService() {
        return service;
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path) throws ServiceException {
        return executeWebServices(method, path, null, null, null, null, null);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, Object body) throws ServiceException {
        return executeWebServices(method, path, body, null, null, null, null);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @param headers an {@link Json} containing the values of the requested header.
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, Object body, Json headers) throws ServiceException {
        return executeWebServices(method, path, body, headers, null, null, null);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @param headers an {@link Json} containing the values of the requested header.
     * @param parameters an {@link Json} containing the values of the query string.
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, Object body, Json headers, Json parameters) throws ServiceException {
        return executeWebServices(method, path, body, headers, parameters, null, null);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @param headers an {@link Json} containing the values of the requested header.
     * @param parameters an {@link Json} containing the values of the query string.
     * @param requestInfo an {@link Json} containing the metadata of the HTTP request.
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, Object body, Json headers, Json parameters, Json requestInfo) throws ServiceException {
        return executeWebServices(method, path, body, headers, parameters, requestInfo, null);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @param headers an {@link Json} containing the values of the requested header.
     * @param parameters an {@link Json} containing the values of the query string.
     * @param requestInfo an {@link Json} containing the metadata of the HTTP request.
     * @param files list of files received on the HTTP request.
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, Object body, Json headers, Json parameters, Json requestInfo, List<UploadedFile> files) throws ServiceException {
        return executeWebServices(method, path, body, headers, parameters, requestInfo, files, false);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param expectedError true if expect an error when executes the web services
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, boolean expectedError) throws ServiceException {
        return executeWebServices(method, path, null, null, null, null, null, expectedError);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @param expectedError true if expect an error when executes the web services
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, Object body, boolean expectedError) throws ServiceException {
        return executeWebServices(method, path, body, null, null, null, null, expectedError);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @param headers an {@link Json} containing the values of the requested header.
     * @param expectedError true if expect an error when executes the web services
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, Object body, Json headers, boolean expectedError) throws ServiceException {
        return executeWebServices(method, path, body, headers, null, null, null, expectedError);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @param headers an {@link Json} containing the values of the requested header.
     * @param parameters an {@link Json} containing the values of the query string.
     * @param expectedError true if expect an error when executes the web services
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, Object body, Json headers, Json parameters, boolean expectedError) throws ServiceException {
        return executeWebServices(method, path, body, headers, parameters, null, null, expectedError);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @param headers an {@link Json} containing the values of the requested header.
     * @param parameters an {@link Json} containing the values of the query string.
     * @param requestInfo an {@link Json} containing the metadata of the HTTP request.
     * @param expectedError true if expect an error when executes the web services
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, Object body, Json headers, Json parameters, Json requestInfo, boolean expectedError) throws ServiceException {
        return executeWebServices(method, path, body, headers, parameters, requestInfo, null, expectedError);
    }

    /**
     * Executes the web service on the service
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @param headers an {@link Json} containing the values of the requested header.
     * @param parameters an {@link Json} containing the values of the query string.
     * @param requestInfo an {@link Json} containing the metadata of the HTTP request.
     * @param files list of files received on the HTTP request.
     * @param expectedError true if expect an error when executes the web services
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(RestMethod method, String path, Object body, Json headers, Json parameters, Json requestInfo, List<UploadedFile> files, boolean expectedError) throws ServiceException {
        return executeWebServices(new WebServiceRequest(method, path, body, headers, parameters, requestInfo, files), expectedError);
    }

    /**
     * Executes the web service on the service
     *
     * @param request web service request
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(WebServiceRequest request) throws ServiceException {
        return executeWebServices(request, false);
    }

    /**
     * Executes the web service on the service
     *
     * @param request web service request
     * @param expectedError true if expect an error when executes the web services
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    public WebServiceResponse executeWebServices(WebServiceRequest request, boolean expectedError) throws ServiceException {
        if(request == null){
            throw new IllegalArgumentException("Request is null");
        }
        logger.info(String.format("%s -------------------------- INI", TEST));
        logger.info(String.format("%s Testing web service: [%s %s]", TEST, request.getMethod(), request.getPath()));
        logger.info(String.format("%s  - WS REQUEST: %s", TEST, request.toJson()));

        WebServiceResponse response;
        boolean isException = false;
        try {
            response = this.baseModule.executeWebServices(request);
            logger.info(String.format("%s  - WS RESPONSE: %s", TEST, response != null ? response.toJson() : "<null>"));
        } catch (ServiceException ee){
            if(expectedError){
                isException = true;
                logger.error(String.format("%s  - WS EXPECTED ERROR: %s", TEST, ee.toJson()));
                response = new WebServiceResponse(ee.toJson());
            } else {
                logger.error(String.format("%s  - WS ERROR: %s", TEST, ee.toJson()));
                throw ee;
            }
        } catch (Exception ex){
            logger.error(String.format("%s  - WS UNEXPECTED ERROR: %s", TEST, ex.getMessage()), ex);
            throw ex;
        }
        if(expectedError && !isException){
            logger.error(String.format("%s  - WS UNEXPECTED RESPONSE: %s", TEST, response != null ? response : "<null>"));
            throw new IllegalStateException("Expected exception but received response: %s");
        }
        logger.info(String.format("%s -------------------------- END", TEST));
        return response;
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName) throws ServiceException {
        return executeFunction(null, functionName, null, null, null, null);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param params body of the function with the parameters
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, Object params) throws ServiceException {
        return executeFunction(null, functionName, null, params, null, null);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param params body of the function with the parameters
     * @param userId user id
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, Object params, String userId) throws ServiceException {
        return executeFunction(null, functionName, null, params, userId, null);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param params body of the function with the parameters
     * @param userId user id
     * @param userEmail user name
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, Object params, String userId, String userEmail) throws ServiceException {
        return executeFunction(null, functionName, null, params, userId, userEmail);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param functionId function id
     * @param params body of the function with the parameters
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, String functionId, Object params) throws ServiceException {
        return executeFunction(null, functionName, functionId, params, null, null);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param functionId function id
     * @param params body of the function with the parameters
     * @param userId user id
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, String functionId, Object params, String userId) throws ServiceException {
        return executeFunction(null, functionName, functionId, params, userId, null);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param functionId function id
     * @param params body of the function with the parameters
     * @param userId user id
     * @param userEmail user name
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, String functionId, Object params, String userId, String userEmail) throws ServiceException {
        return executeFunction(null, functionName, functionId, params, userId, userEmail);
    }

    /**
     * Executes the function on the service
     *
     * @param date date of execution of the request
     * @param functionName function name
     * @param functionId function id
     * @param params body of the function with the parameters
     * @param userId user id
     * @param userEmail user name
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(Long date, String functionName, String functionId, Object params, String userId, String userEmail) throws ServiceException {
        return executeFunction(date, functionName, functionId, params, userId, userEmail, false);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param expectedError true if expect an error when executes the function
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, boolean expectedError) throws ServiceException {
        return executeFunction(null, functionName, null, null, null, null, expectedError);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param params body of the function with the parameters
     * @param expectedError true if expect an error when executes the function
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, Object params, boolean expectedError) throws ServiceException {
        return executeFunction(null, functionName, null, params, null, null, expectedError);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param params body of the function with the parameters
     * @param userId user id
     * @param expectedError true if expect an error when executes the function
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, Object params, String userId, boolean expectedError) throws ServiceException {
        return executeFunction(null, functionName, null, params, userId, null, expectedError);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param params body of the function with the parameters
     * @param userId user id
     * @param userEmail user name
     * @param expectedError true if expect an error when executes the function
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, Object params, String userId, String userEmail, boolean expectedError) throws ServiceException {
        return executeFunction(null, functionName, null, params, userId, userEmail, expectedError);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param functionId function id
     * @param params body of the function with the parameters
     * @param expectedError true if expect an error when executes the function
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, String functionId, Object params, boolean expectedError) throws ServiceException {
        return executeFunction(null, functionName, functionId, params, null, null, expectedError);
    }

    /**
     * Executes the function on the service
     *
     * @param functionName function name
     * @param functionId function id
     * @param params body of the function with the parameters
     * @param userId user id
     * @param userEmail user name
     * @param expectedError true if expect an error when executes the function
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(String functionName, String functionId, Object params, String userId, String userEmail, boolean expectedError) throws ServiceException {
        return executeFunction(null, functionName, functionId, params, userId, userEmail, expectedError);
    }

    /**
     * Executes the function on the service
     *
     * @param date date of execution of the request
     * @param functionName function name
     * @param functionId function id
     * @param params body of the function with the parameters
     * @param userId user id
     * @param userEmail user name
     * @param expectedError true if expect an error when executes the function
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(Long date, String functionName, String functionId, Object params, String userId, String userEmail, boolean expectedError) throws ServiceException {
        return executeFunction(generateRequest(date, functionName, functionId, params, userId, userEmail), expectedError);
    }

    /**
     * Executes the function on the service
     *
     * @param request function request
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(FunctionRequest request) throws ServiceException {
        return executeFunction(request, false);
    }

    /**
     * Executes the function on the service
     *
     * @param request function request
     * @param expectedError true if expect an error when executes the function
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    public Json executeFunction(FunctionRequest request, boolean expectedError) throws ServiceException {
        if(request == null){
            throw new IllegalArgumentException("Request is null");
        }
        logger.info(String.format("%s -------------------------- INI", TEST));
        logger.info(String.format("%s Testing function: [%s]", TEST, request.getFunctionName()));
        logger.info(String.format("%s  - FC REQUEST: %s", TEST, request.toJson()));
        Json response;
        boolean isException = false;
        try {
            response = this.baseModule.executeFunction(request);
            logger.info(String.format("%s  - FC RESPONSE: %s", TEST, response != null ? response.toJson() : "<null>"));
        } catch (ServiceException ee){
            if(expectedError){
                isException = true;
                logger.error(String.format("%s  - FC EXPECTED ERROR: %s", TEST, ee.toJson()));
                response = ee.toJson();
            } else {
                logger.error(String.format("%s  - FC ERROR: %s", TEST, ee.toJson()));
                throw ee;
            }
        } catch (Exception ex){
            logger.error(String.format("%s  - FC UNEXPECTED ERROR: %s", TEST, ex.getMessage()), ex);
            throw ex;
        }
        if(expectedError && !isException){
            logger.error(String.format("%s  - FC UNEXPECTED RESPONSE: %s", TEST, response != null ? response : "<null>"));
            throw new IllegalStateException(String.format("Expected exception but received response: %s", response != null ? response : "<null>"));
        }
        logger.info(String.format("%s -------------------------- END", TEST));
        return response;
    }

    /**
     * Generates e function request
     *
     * @param date date of execution of the request
     * @param functionName function name
     * @param functionId function id
     * @param params body of the function with the parameters
     * @param userId user id
     * @param userEmail user name
     * @return function request
     */
    public static FunctionRequest generateRequest(Long date, String functionName, String functionId, Object params, String userId, String userEmail) {
        return new FunctionRequest(
                Json.map()
                        .set(Parameter.DATE, date != null ? date : System.currentTimeMillis())
                        .setIfNotEmpty(Parameter.FUNCTION_NAME, functionName)
                        .setIfNotEmpty(Parameter.FUNCTION_ID, functionId)
                        .setIfNotEmpty(Parameter.PARAMS, params)
                        .setIfNotEmpty(Parameter.USER_ID, userId)
                        .setIfNotEmpty(Parameter.USER_EMAIL, userEmail)
        );
    }

    /**
     * Throws an exception if the testing mode is not enabled
     */
    private void isTestingModeEnabled(){
        if(extensionBroker == null) {
            final String error = String.format("%s Testing mode is not enabled. Use the flag '_testing_mode=true' on configuration.", TEST);

            logger.warn(error);
            throw new IllegalStateException(error);
        }
    }

    /**
     * Clears the list of received events
     */
    public void clearReceivedEvents(){
        isTestingModeEnabled();

        extensionBroker.clearReceivedEvents();
        logger.info(String.format("%s received events cleared", TEST));
    }

    /**
     * Gets the list of received events
     *
     * @return list of received events
     */
    public List<Json> getReceivedEvents(){
        isTestingModeEnabled();

        final List<Json> events = extensionBroker.getReceivedEvents();
        logger.info(String.format("%s received events [%s]", TEST, events.size()));
        return events;
    }

    /**
     * Registers a processor for the event name
     */
    public void registerEventProcessor(String event, MessageProcessor processor){
        isTestingModeEnabled();

        extensionBroker.registerEventProcessor(event, processor);
        logger.info(String.format("%s registered event processor [%s]", TEST, event));
    }

    /**
     * Removes the event processor for the given event name
     */
    public void removeEventProcessor(String event){
        isTestingModeEnabled();

        extensionBroker.removeEventProcessor(event);
        logger.info(String.format("%s event processor removed [%s]", TEST, event));
    }

    /**
     * Clears the event processors
     */
    public void clearEventProcessors(){
        isTestingModeEnabled();

        extensionBroker.clearEventProcessors();
        logger.info(String.format("%s event processors cleaned", TEST));
    }

    /**
     * Clears the list of locks
     */
    public void clearLocks(){
        isTestingModeEnabled();

        extensionBroker.clearLocks();
        logger.info(String.format("%s cleaned locks", TEST));
    }

    /**
     * Gets the list of locks
     *
     * @return list of locks
     */
    public List<String> getLocks(){
        isTestingModeEnabled();

        final List<String> locks = extensionBroker.getLocks();
        logger.info(String.format("%s locks [%s]", TEST, locks.size()));
        return locks;
    }

    /**
     * Add a file to be used on tests
     */
    public void addFile(String fileId, ExtensionBrokerMock.FileMock file){
        isTestingModeEnabled();

        extensionBroker.addFile(fileId, file);
        logger.info(String.format("%s file added [%s]", TEST, fileId));
    }

    /**
     * Clears the list of files
     */
    public void clearFiles(){
        isTestingModeEnabled();

        extensionBroker.clearFiles();
        logger.info(String.format("%s cleaned files", TEST));
    }

    /**
     * Gets the list of files
     *
     * @return list of files
     */
    public List<ExtensionBrokerMock.FileMock> getFiles(){
        isTestingModeEnabled();

        final List<ExtensionBrokerMock.FileMock> files = extensionBroker.getFiles();
        logger.info(String.format("%s files [%s]", TEST, files.size()));
        return files;
    }

    /**
     * Clears the list of data stores
     */
    public void clearDataStores(){
        isTestingModeEnabled();

        extensionBroker.clearDataStores();
        logger.info(String.format("%s cleaned data stores", TEST));
    }

    /**
     * Clears the data store
     *
     * @param dataStoreName data store name
     */
    public void clearDataStore(String dataStoreName){
        isTestingModeEnabled();

        extensionBroker.clearDataStore(dataStoreName);
        logger.info(String.format("%s cleaned data store [%s]", TEST, dataStoreName));
    }

    /**
     * Clears the data store
     */
    public void clearUserDataStore(){
        clearDataStore(DataStores.USER_DATA_STORE);
    }

    /**
     * Gets the list of data store names
     *
     * @return list of data stores names
     */
    public List<String> getDataStoreNames(){
        isTestingModeEnabled();

        final List<String> names = extensionBroker.getDataStoreNames();
        logger.info(String.format("%s data store names [%s]", TEST, names.size()));
        return names;
    }

    /**
     * Gets the list of data store items
     *
     * @param dataStoreName data store name
     * @return list of data stores items
     */
    public List<Json> getDataStoreItems(String dataStoreName){
        isTestingModeEnabled();

        final List<Json> items = extensionBroker.getDataStoreItems(dataStoreName);
        logger.info(String.format("%s data store [%s] items [%s]", TEST, dataStoreName, items.size()));
        return items;
    }

    /**
     * Gets the list of user data store items
     *
     * @return list of data stores items
     */
    public List<Json> getUserDataStoreItems(){
        return getDataStoreItems(DataStores.USER_DATA_STORE);
    }

    /**
     * Saves a document on a data store.
     *
     * @param dataStoreName data store name
     * @param document document to save on data store
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public Json addDataStoreItem(String dataStoreName, Json document) throws ServiceException {
        isTestingModeEnabled();

        final Json item = extensionBroker.saveDocument(dataStoreName, document);
        logger.info(String.format("%s saved data store [%s] item [%s]", TEST, dataStoreName, item));
        return item;
    }

    /**
     * Saves a document on the user data store.
     *
     * @param document document to save on data store
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public Json addUserDataStoreItem(Json document) throws ServiceException {
        return addDataStoreItem(DataStores.USER_DATA_STORE, document);
    }

    /**
     * Add an user to be used on tests using a random token
     */
    public void addAppUser(AppUser appUser){
        addAppUser(Strings.randomUUIDString(), appUser);
    }

    /**
     * Add an user to be used on tests
     */
    public void addAppUser(String token, AppUser appUser){
        isTestingModeEnabled();

        extensionBroker.addAppUser(token, appUser);
        logger.info(String.format("%s user added [%s]", TEST, token));
    }

    /**
     * Clears the list of users
     */
    public void clearAppUsers(){
        isTestingModeEnabled();

        extensionBroker.clearAppUsers();
        logger.info(String.format("%s cleaned users", TEST));
    }

    /**
     * Gets the list of users
     *
     * @return list of users
     */
    public List<AppUser> getAppUsers(){
        isTestingModeEnabled();

        final List<AppUser> users = extensionBroker.getAppUsers();
        logger.info(String.format("%s users [%s]", TEST, users.size()));
        return users;
    }

    /**
     * Creates a new application user with a random id
     *
     * @param email user email
     * @param firstName first name of the user
     * @param lastName last name of the user
     * @param developer true if the user is a developer of the application
     */
    public AppUser createAppUser(String email, String firstName, String lastName, boolean developer){
        return createAppUser(email, firstName, lastName, "ACTIVE", developer);
    }

    /**
     * Creates a new application user with a random id
     *
     * @param email user email
     * @param firstName first name of the user
     * @param lastName last name of the user
     * @param status status of the user
     * @param developer true if the user is a developer of the application
     */
    public AppUser createAppUser(String email, String firstName, String lastName, String status, boolean developer){
        return createAppUser(email, firstName, lastName, status, developer, null);
    }

    /**
     * Creates a new application user with a random id
     *
     * @param email user email
     * @param firstName first name of the user
     * @param lastName last name of the user
     * @param status status of the user
     * @param developer true if the user is a developer of the application
     * @param groups list of groups
     */
    public AppUser createAppUser(String email, String firstName, String lastName, String status, boolean developer, List<Json> groups){
        return createAppUser(Strings.randomUUIDString(), email, firstName, lastName, status, developer, groups);
    }

    /**
     * Creates a new application user
     *
     * @param id id of the user on the application
     * @param email user email
     * @param firstName first name of the user
     * @param lastName last name of the user
     * @param status status of the user
     * @param developer true if the user is a developer of the application
     * @param groups list of groups
     */
    public AppUser createAppUser(String id, String email, String firstName, String lastName, String status, boolean developer, List<Json> groups){
        return createAppUser(id, email, firstName, lastName, status, developer, groups, null, null);
    }

    /**
     * Creates a new application user
     *
     * @param id id of the user on the application
     * @param email user email
     * @param firstName first name of the user
     * @param lastName last name of the user
     * @param status status of the user
     * @param developer true if the user is a developer of the application
     * @param groups list of groups
     * @param localization information about the localization of the user
     * @param permissions permissions of the user
     */
    public AppUser createAppUser(String id, String email, String firstName, String lastName, String status, boolean developer, List<Json> groups, Json localization, Json permissions){
        final String fullName = String.format("%s %s", firstName, lastName);
        return new AppUser(id, "1", email, firstName, lastName, fullName, status, developer, groups, localization, permissions);
    }
}
