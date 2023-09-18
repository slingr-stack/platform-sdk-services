package io.slingr.services.services;

import io.slingr.services.configurations.ServiceContext;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.services.rest.DownloadedFile;
import io.slingr.services.services.rest.HttpRequest;
import io.slingr.services.services.rest.RestClient;
import io.slingr.services.services.rest.RestMethod;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.XmlToJsonParser;
import io.slingr.services.ws.exchange.FunctionRequest;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.ws.exchange.WebServiceResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

/**
 * <p>Helper to exchange information with an external HTTP service
 *
 * <p>Created by lefunes on 09/04/18.
 */
public class HttpService extends RestClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpService.class);

    public static final String FILE_DOWNLOADED_EVENT = "fileDownloaded";
    public static final String WEBHOOK_EVENT = "webhook";
    public static final String CALLBACK_EVENT = "callback";
    private final Events events;
    private final Files files;

    private IHttpExceptionConverter exceptionConverter = null;

    /**
     * Creates a http service over an external service
     *
     * @param apiUri base URI to build the requests to the API.
     * @param events events manager.
     * @param files  files manager.
     * @param debug  true if the service shows information useful for debug.
     */
    public HttpService(String apiUri, Events events, Files files, boolean debug) {
        super(apiUri);
        setDebug(debug);

        this.events = events;
        this.files = files;
    }

    /**
     * Setup the default exception converter
     *
     * @param exceptionConverter default exception converter
     */
    public void setupExceptionConverter(IHttpExceptionConverter exceptionConverter) {
        this.exceptionConverter = exceptionConverter;
    }

    /**
     * Process the GET requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultGetRequest(FunctionRequest request) {
        Json params = request.getJsonParams();
        // copy app info
        params.set(Parameter.APP, request.getApp());
        params.set(Parameter.ENV, request.getEnv());
        return defaultGetRequest(params, request.getFunctionId());
    }

    /**
     * Process the GET requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultGetRequest(Json request) {
        return defaultGetRequest(request, null);
    }

    /**
     * Process the GET requests to the external HTTP service
     *
     * @param request        request to send to the external HTTP service
     * @param fromFunctionId Id of a function related to the event. It is used to send {@link HttpService#FILE_DOWNLOADED_EVENT} events.
     * @return response from the external HTTP service
     */
    public Json defaultGetRequest(Json request, String fromFunctionId) {
        return defaultGetRequest(HttpRequest.fromJson(RestMethod.GET, request), fromFunctionId);
    }

    /**
     * Process the GET requests to the external HTTP service
     *
     * @param request        request to send to the external HTTP service
     * @param fromFunctionId Id of a function related to the event. It is used to send {@link HttpService#FILE_DOWNLOADED_EVENT} events.
     * @return response from the external HTTP service
     */
    public Json defaultGetRequest(HttpRequest request, String fromFunctionId) {
        try {
            if (request.isForceDownload()) {
                // download file
                if (request.isDownloadSync()) {
                    // download file synchronously
                    return downloadFileAndUploadToPlatform(request);
                } else {
                    String app = ServiceContext.getCurrentApp();
                    String env = ServiceContext.getCurrentEnv();
                    // download file asynchronously
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            ServiceContext.initContext(app, env);
                            final Json uploadedFileToPlatform = downloadFileAndUploadToPlatform(request);
                            this.events.send(null, FILE_DOWNLOADED_EVENT, uploadedFileToPlatform, fromFunctionId, null, null, 1);
                        } catch (Exception e) {
                            logger.warn(String.format("Exception when try to send the 'fileDownloaded' event - exception: %s", e.getMessage()), e);
                        }
                    });

                    if (request.isFullResponse()) {
                        return Json.map()
                                .set("body", "ok")
                                .set("fullResponse", true);
                    } else {
                        return Json.map()
                                .set("body", "ok");
                    }
                }
            } else {
                // return get in body
                processCallback(request, fromFunctionId);
                return httpGet(request);
            }
        } catch (Exception ex) {
            throw convertToServiceException(ex);
        }
    }

    /**
     * Download the file from the external service and upload the same to the platform
     *
     * @param request information of the file to download
     * @return metadata of the file upload to the platform
     */
    private Json downloadFileAndUploadToPlatform(HttpRequest request) {
        final DownloadedFile file = httpDownload(request);

        // upload file to platform
        String fileName = request.getFilename();
        if (StringUtils.isBlank(fileName) || fileName.equals(DEFAULT_FILE_NAME)) {
            // try yo guess filename from path if name not provided
            final String path = request.getPath();
            fileName = extractFileName(path);
            if (StringUtils.isEmpty(fileName)) {
                fileName = DEFAULT_FILE_NAME;
            }
        }
        return files.upload(fileName, file);
    }

    /**
     * Extracts the file name from a path
     *
     * @param path path to check
     * @return file name
     */
    public static String extractFileName(String path) {
        String fileName;
        if (StringUtils.isEmpty(path)) {
            fileName = "";
        } else {
            // file name should be the last part after "/" and before "?" (if exists)
            String[] parts = StringUtils.split(path.trim(), "/");
            fileName = parts[parts.length - 1];
            parts = StringUtils.split(fileName, "?");
            fileName = parts[0];
        }
        return fileName;
    }

    /**
     * Process the POST requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultPostRequest(FunctionRequest request) {
        return defaultPostRequest(request.getJsonParams(), request.getFunctionId());
    }

    /**
     * Process the POST requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @param functionId -
     * @return response from the external HTTP service
     */
    public Json defaultPostRequest(Json request, String functionId) {
        return defaultPostRequest(HttpRequest.fromJson(RestMethod.POST, request), functionId);
    }

    /**
     * Process the POST requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultPostRequest(HttpRequest request, String functionId) {
        try {
            if (request.isMultipart()) {
                return httpMultipart(request, files);
            } else {
                processCallback(request, functionId);
                return httpPost(request);
            }
        } catch (Exception ex) {
            throw convertToServiceException(ex);
        }
    }

    /**
     * Process the PUT requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultPutRequest(FunctionRequest request) {
        return defaultPutRequest(request.getJsonParams(), request.getFunctionId());
    }

    /**
     * Process the PUT requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultPutRequest(Json request, String functionId) {
        return defaultPutRequest(HttpRequest.fromJson(RestMethod.PUT, request), functionId);
    }

    /**
     * Process the PUT requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultPutRequest(HttpRequest request, String functionId) {
        try {
            if (request.isMultipart()) {
                return httpMultipart(request, files);
            } else {
                processCallback(request, functionId);
                return httpPut(request);
            }
        } catch (Exception ex) {
            throw convertToServiceException(ex);
        }
    }

    /**
     * Process the DELETE requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultDeleteRequest(FunctionRequest request) {
        return defaultDeleteRequest(request.getJsonParams(), request.getFunctionId());
    }

    /**
     * Process the DELETE requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultDeleteRequest(Json request, String functionId) {
        return defaultDeleteRequest(HttpRequest.fromJson(RestMethod.DELETE, request), functionId);
    }

    /**
     * Process the DELETE requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultDeleteRequest(HttpRequest request, String functionId) {
        try {
            processCallback(request, functionId);
            return httpDelete(request);
        } catch (Exception ex) {
            throw convertToServiceException(ex);
        }
    }

    /**
     * Process the HEAD requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultHeadRequest(FunctionRequest request) {
        return defaultHeadRequest(request.getJsonParams(), request.getFunctionId());
    }

    /**
     * Process the HEAD requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultHeadRequest(Json request, String functionId) {
        return defaultHeadRequest(HttpRequest.fromJson(RestMethod.HEAD, request), functionId);
    }

    /**
     * Process the HEAD requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultHeadRequest(HttpRequest request, String functionId) {
        try {
            processCallback(request, functionId);
            return httpHead(request);
        } catch (Exception ex) {
            throw convertToServiceException(ex);
        }
    }

    /**
     * Process the PATCH requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultPatchRequest(FunctionRequest request) {
        return defaultPatchRequest(request.getJsonParams(), request.getFunctionId());
    }

    /**
     * Process the PATCH requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultPatchRequest(Json request, String functionId) {
        return defaultPatchRequest(HttpRequest.fromJson(RestMethod.PATCH, request), functionId);
    }

    /**
     * Process the PATCH requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultPatchRequest(HttpRequest request, String functionId) {
        try {
            processCallback(request, functionId);
            return httpPatch(request);
        } catch (Exception ex) {
            throw convertToServiceException(ex);
        }
    }

    /**
     * Process the OPTIONS requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultOptionsRequest(FunctionRequest request) {
        return defaultOptionsRequest(request.getJsonParams(), request.getFunctionId());
    }

    /**
     * Process the OPTIONS requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultOptionsRequest(Json request, String functionId) {
        return defaultOptionsRequest(HttpRequest.fromJson(RestMethod.OPTIONS, request), functionId);
    }

    /**
     * Process the OPTIONS requests to the external HTTP service
     *
     * @param request request to send to the external HTTP service
     * @return response from the external HTTP service
     */
    public Json defaultOptionsRequest(HttpRequest request, String functionId) {
        try {
            processCallback(request, functionId);
            return httpOptions(request);
        } catch (Exception ex) {
            throw convertToServiceException(ex);
        }
    }

    /**
     * Process the incoming web service requests from the external HTTP service.
     * Sends a 'webhook' event to the platform.
     *
     * @param request request from the external HTTP service
     * @return response to send to the external HTTP service
     */
    public WebServiceResponse defaultWebhookProcessor(WebServiceRequest request) {
        // send the webhook event
        events.send(WEBHOOK_EVENT, defaultWebhookConverter(request));

        return defaultWebhookResponse();
    }

    /**
     * Converts the exception to a generated exception when works with the external HTTP service to an {@link ServiceException}
     *
     * @param exception exception to process
     * @return equivalent service exception
     */
    public ServiceException convertToServiceException(Exception exception) {
        ServiceException response = null;
        if (exceptionConverter != null) {
            response = exceptionConverter.convertToServiceException(exception);
        }
        if (response == null) {
            response = defaultConvertToServiceException(exception);
        }
        return response;
    }

    /**
     * Converts the exception to a generated exception when works with the external HTTP service to an {@link ServiceException}
     *
     * @param exception exception to process
     * @return equivalent service exception
     */
    public static ServiceException defaultConvertToServiceException(Exception exception) {
        ServiceException response;
        if (exception instanceof ServiceException) {
            final Json ex = ((ServiceException) exception).toJson(true);
            if (ex.string(Parameter.EXCEPTION_MESSAGE).startsWith("HTTP ") && ex.contains(Parameter.EXCEPTION_ADDITIONAL_INFO)) {
                final Json ai = ex.json(Parameter.EXCEPTION_ADDITIONAL_INFO);
                if (ai.contains("details")) {
                    final Json dt = ai.json("details");
                    if (dt.contains(Parameter.EXCEPTION_CODE)) {
                        final String description = dt.json(Parameter.EXCEPTION_CODE).string(Parameter.EXCEPTION_DESCRIPTION);
                        if (StringUtils.isNotBlank(description)) {
                            ai.set("originalMessage", ex.string(Parameter.EXCEPTION_MESSAGE));
                            return ServiceException.permanent(((ServiceException) exception).getCode(), description, ai, exception);
                        }
                    }
                }
            }
            response = (ServiceException) exception;
        } else {
            response = ServiceException.permanent(ErrorCode.API, exception.getMessage(), exception);
        }
        return response;
    }

    /**
     * Converts the web service requests to an equivalent {@link Json } to be used on events
     *
     * @param request request to convert
     * @return equivalent Json object
     */
    public static Json defaultWebhookConverter(WebServiceRequest request) {
        final Json json = request.toJson();
        if (request.getBody() != null) {
            String contentType = request.getHeader(Parameter.CONTENT_TYPE);
            if (StringUtils.isNotBlank(contentType)) {
                contentType = contentType.toLowerCase();
                if (contentType.contains("json")) {
                    try {
                        // try to parse JSON documents
                        json.set("body", request.getJsonBody());
                    } catch (Exception ex) {
                        json.set("body", request.getBody());
                    }
                } else if (contentType.contains("xml")) {
                    try {
                        // try to convert XML documents
                        json.set("body", XmlToJsonParser.parse(request.getBody().toString().trim()));
                    } catch (Exception ex) {
                        json.set("body", request.getBody());
                    }
                } else {
                    json.set("body", request.getBody());
                }
            } else {
                json.set("body", request.getBody());
            }
        } else {
            json.set("body", "");
        }
        return json;
    }

    /**
     * Returns the default web service response for services that call to the webhooks
     *
     * @return web service response
     */
    public static WebServiceResponse defaultWebhookResponse() {
        return defaultWebhookResponse(null);
    }

    /**
     * Returns the default web service response for services that call to the webhooks
     *
     * @param response response message
     * @return web service response
     */
    public static WebServiceResponse defaultWebhookResponse(String response) {
        return defaultWebhookResponse(response, 200);
    }

    /**
     * Returns the default web service response for services that call to the webhooks
     *
     * @param response response message
     * @param code     HTTP code to return
     * @return web service response
     */
    public static WebServiceResponse defaultWebhookResponse(String response, int code) {
        return new WebServiceResponse(code, StringUtils.isBlank(response) ? "ok" : response, ContentType.TEXT_PLAIN.toString());
    }

    private void processCallback(HttpRequest request, String functionId){
        if(request.isDefaultCallback()){
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    Json body = request.getJsonBody();
                    this.events.send(CALLBACK_EVENT, body, functionId);
                } catch (Exception e) {
                    logger.warn(String.format("Exception when try to send the 'callback' event - exception: %s", e.getMessage()), e);
                }
            });
        }
    }
}
