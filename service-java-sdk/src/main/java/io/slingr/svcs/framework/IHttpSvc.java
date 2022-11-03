package io.slingr.svcs.framework;

import io.slingr.svcs.services.HttpService;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.ws.exchange.FunctionRequest;
import io.slingr.svcs.ws.exchange.WebServiceRequest;
import io.slingr.svcs.ws.exchange.WebServiceResponse;
import io.slingr.svcs.framework.annotations.SlingrService;

/**
 * Interface that the HTTP {@link SlingrService} must be implement
 *
 * <p>Created by lefunes on 12/03/18.
 */
public interface IHttpSvc {

    /**
     * Returns the HTTP service to help with the exchanges with the external service
     *
     * @return HTTP service
     */
    HttpService httpService();

    /**
     * Process the GET requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    Json defaultGetRequest(FunctionRequest request);

    /**
     * Process the POST requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    Json defaultPostRequest(FunctionRequest request);

    /**
     * Process the PUT requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    Json defaultPutRequest(FunctionRequest request);

    /**
     * Process the DELETE requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    Json defaultDeleteRequest(FunctionRequest request);

    /**
     * Process the HEAD requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    Json defaultHeadRequest(FunctionRequest request);

    /**
     * Process the PATCH requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    Json defaultPatchRequest(FunctionRequest request);

    /**
     * Process the OPTIONS requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    Json defaultOptionsRequest(FunctionRequest request);

    /**
     * Process the incoming web service requests from the external HTTP service.
     * Sends a 'webhook' event to the platform.
     *
     * @param request request from the external HTTP service
     * @return response to send to the external HTTP service
     */
    WebServiceResponse defaultWebhookProcessor(WebServiceRequest request);
}
