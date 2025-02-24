package io.slingr.services.framework;

import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.utils.Json;
import io.slingr.services.ws.exchange.FunctionRequest;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.ws.exchange.WebServiceResponse;

/**
 * Interface to implement the executable actions over a base service from the Extension Broker app or external services.
 *
 */
public interface IBaseService {

    /**
     * Starts the stopping process of the service
     *
     * @param cause cause of the termination
     */
    void stopService(String cause);

    /**
     * Get the list of properties and metadata of service used when the Extension Broker is connected through a Proxy service.
     *
     * @return a map that contains configuration and metadata about the service
     */
    Json getConfiguration() throws ServiceException;

    /**
     * Executes the function on the service
     *
     * @param request function request
     * @return function response
     * @throws ServiceException exception if an error happened when process the function request
     */
    Json executeFunction(FunctionRequest request) throws ServiceException;

    /**
     * Executes the web service on the service
     *
     * @param request web service request
     * @return web service response
     * @throws ServiceException exception if an error happened when process the web service request
     */
    WebServiceResponse executeWebServices(WebServiceRequest request) throws ServiceException;

    /**
     * Enables the configurator interceptor in this service
     */
    @SuppressWarnings("unused")
    void enableConfiguratorInterceptor();

    /**
     * Enables the function interceptor in this service
     */
    @SuppressWarnings("unused")
    void enableFunctionInterceptor();

    /**
     * Enables the web services interceptor in this service
     */
    @SuppressWarnings("unused")
    void enableWebServicesInterceptor();

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
}