package io.slingr.services.ws;

import io.slingr.services.Service;
import io.slingr.services.configurations.ServiceContext;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.framework.IBaseService;
import io.slingr.services.services.exchange.ApiUri;
import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.services.rest.RestMethod;
import io.slingr.services.utils.FilesUtils;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.Strings;
import io.slingr.services.utils.converters.ContentTypeFormat;
import io.slingr.services.utils.converters.JsonConverter;
import io.slingr.services.ws.exchange.FunctionRequest;
import io.slingr.services.ws.exchange.UploadedFile;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.ws.exchange.WebServiceResponse;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Receives the web services requests, calls to the respective processor and returns the response to the HTTP client.
 *
 */
public class WebServicesProcessor extends AbstractHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebServicesProcessor.class);

    private static final int DEFAULT_MAX_REDELIVERS = 3;
    private static final int DEFAULT_PERMANENT_MAX_REDELIVERS = 0;
    private static final int DEFAULT_RETRYABLE_MAX_REDELIVERS = DEFAULT_MAX_REDELIVERS;
    private static final long DEFAULT_PERMANENT_DELAY = 4000;
    private static final long DEFAULT_RETRYABLE_DELAY = 2000;

    private final IBaseService baseService;
    private final String token;
    private final boolean localDeployment;
    private final boolean debug;
    private final AtomicBoolean firstLocalDeploymentWarning = new AtomicBoolean(false);

    private int maxRedelivers = DEFAULT_MAX_REDELIVERS;
    private int permanentMaxRedelivers = DEFAULT_PERMANENT_MAX_REDELIVERS;
    private long permanentDelay = DEFAULT_PERMANENT_DELAY;
    private int retryableMaxRedelivers = DEFAULT_RETRYABLE_MAX_REDELIVERS;
    private long retryableDelay = DEFAULT_RETRYABLE_DELAY;

    /**
     * Instances a new web services processor
     *
     * @param baseService object that will receive the close signal
     * @param token token used to exchange information between the service and the Extension Broker
     * @param localDeployment true if is executed on local environment
     * @param debug true if the service shows information useful for debug
     */
    public WebServicesProcessor(IBaseService baseService, String token, boolean localDeployment, boolean debug) {
        if(baseService == null) {
            throw new IllegalArgumentException("Base service is required to instance a web service processor");
        }
        this.baseService = baseService;
        this.token = token;
        this.localDeployment = localDeployment;
        this.debug = debug;
    }

    /**
     * Handle a web services request.
     *
     * @param path the target of the servletRequest
     * @param baseRequest the original unwrapped request object.
     * @param servletRequest the request either as the {@link Request} object or a wrapper of that servlet request.
     * @param servletResponse the response as the {@link Response} object or a wrapper of that servlet request.
     * @throws IOException if unable to handle the request or response processing
     */
    @Override
    public void handle(String path, Request baseRequest, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        logger.info(String.format("WS request [%s %s]", servletRequest.getMethod(), path));
        ServiceContext.initContext(servletRequest.getHeader(Parameter.METADATA_APP), servletRequest.getHeader(Parameter.METADATA_ENV));
        try {
            final WebServiceRequest webServiceRequest = prepareRequest(path, servletRequest);
            if(debug){
                logger.info(String.format("%s request %s", Service.DEBUG, webServiceRequest));
            }
            final WebServiceResponse response = processRequest(webServiceRequest);
            sendResponse(response, servletResponse);

            // Inform jetty that this request has now been handled
            baseRequest.setHandled(true);
        } finally {
            ServiceContext.endContext();
        }
    }

    /**
     * Process the {@link WebServiceResponse} object and send to an HTTP client via {@link Response}
     *
     * @param request the equivalent {@link WebServiceRequest} to the received {@link Request}
     */
    private WebServiceResponse processRequest(WebServiceRequest request) {
        final WebServiceResponse response;
        if(request.getPath().equals(ApiUri.URL_SYSTEM_ALIVE) ||
                request.getPath().equals(ApiUri.URL_SYSTEM_TERMINATE) ||
                request.getPath().equals(ApiUri.URL_CONFIGURATION) ||
                request.getPath().equals(ApiUri.URL_FUNCTION)){
            response = processApiRequest(request);
        } else {
            response = processWSRequest(request);
        }
        return response;
    }

    /**
     * Process the {@link WebServiceResponse} object and send to an HTTP client via {@link Response}
     *
     * @param request the equivalent {@link WebServiceRequest} to the received {@link Request}
     */
    private WebServiceResponse processApiRequest(WebServiceRequest request) {
        int code = 200;
        Object body = null;
        Json headers = Json.map();

        // check request token
        boolean validToken = true;
        if(!token.equals(request.getHeader(Parameter.TOKEN))){
            // avoid errors on local environment
            if(this.localDeployment){
                if(firstLocalDeploymentWarning.compareAndSet(false, true)) {
                    logger.warn("Invalid or empty token on request. Ignored exceptions of this kind because the service is running in local deployment.");
                }
            } else {
                validToken = false;
                code = 401;
                body = "Invalid token";
                headers.set(Parameter.CONTENT_TYPE, ContentTypeFormat.PLAIN_TEXT.getMimeType());
            }
        }

        boolean processedRequest = false;
        if(validToken){
            // process request using the Service API specification v1
            if(ApiUri.URL_SYSTEM_ALIVE.equals(request.getPath())) {
                body = Json.map().set("started", true);
                processedRequest = true;
            } else if(ApiUri.URL_SYSTEM_TERMINATE.equals(request.getPath())) {
                baseService.stopService("termination signal received");

                body = "ok";
                headers.set(Parameter.CONTENT_TYPE, ContentTypeFormat.PLAIN_TEXT.getMimeType());

                processedRequest = true;
            } else if(ApiUri.URL_CONFIGURATION.equals(request.getPath())) {
                body = baseService.getConfiguration();

                processedRequest = true;
            } else if(ApiUri.URL_FUNCTION.equals(request.getPath())) {
                final FunctionRequest functionRequest = new FunctionRequest(request.getBody(), false, 0, DEFAULT_MAX_REDELIVERS);
                return executeFunction(functionRequest);
            }
        }

        if(validToken && !processedRequest){
            code = 404;
            body = "Invalid API request";
            headers.set(Parameter.CONTENT_TYPE, ContentTypeFormat.PLAIN_TEXT.getMimeType());
        }

        if(body == null){
            body = Json.map();
        }

        return new WebServiceResponse(code, body, headers);
    }

    /**
     * Process the function with the information of the given {@link FunctionRequest}
     *
     * @param functionRequest request to process
     * @return response of the function execution
     */
    private WebServiceResponse executeFunction(FunctionRequest functionRequest) {
        if(functionRequest == null){
            return new WebServiceResponse(400, "Invalid API request", ContentTypeFormat.PLAIN_TEXT.getMimeType());
        }
        Json originalRequest = null;
        try {
            originalRequest = functionRequest.getRequest().cloneJson();
            final Json response = baseService.executeFunction(functionRequest);
            return new WebServiceResponse(response != null && response.isMap() && response.is(Parameter.EXCEPTION_FLAG) ? 500 : 200, Json.map()
                    .set(Parameter.DATE, System.currentTimeMillis())
                    .setIfNotNull(Parameter.DATA, response)
            );
        } catch (ServiceException ee){
            int maxRedelivers = Math.min(this.maxRedelivers, ee.isRetryable() ? this.retryableMaxRedelivers : this.permanentMaxRedelivers);
            maxRedelivers = Math.min(maxRedelivers, functionRequest.getRedeliveredMaxCounter());

            if(originalRequest != null && maxRedelivers > functionRequest.getRedeliveredCounter()){
                // retry request
                try {
                    Thread.sleep(ee.isRetryable() ? this.retryableDelay : this.permanentDelay);
                } catch (InterruptedException e) {
                    // do nothing
                }
                final FunctionRequest newFunctionRequest = new FunctionRequest(originalRequest, true, functionRequest.getRedeliveredCounter() + 1, maxRedelivers);
                return executeFunction(newFunctionRequest);
            }

            return new WebServiceResponse(ee.getReturnCode(), Json.map()
                    .set(Parameter.DATE, System.currentTimeMillis())
                    .setIfNotNull(Parameter.DATA, ee.toJson())
            );
        } catch (Exception ex){
            return new WebServiceResponse(500, Json.map()
                    .set(Parameter.DATE, System.currentTimeMillis())
                    .setIfNotNull(Parameter.DATA, ServiceException.json(ErrorCode.GENERAL, String.format("Exception when execute function: %s", ex.getMessage()), functionRequest.toJson(), ex))
            );
        }
    }

    /**
     * Process the {@link WebServiceResponse} object and send to an HTTP client via {@link Response}
     *
     * @param request the equivalent {@link WebServiceRequest} to the received {@link Request}
     */
    private WebServiceResponse processWSRequest(WebServiceRequest request) {
        try {
            return baseService.executeWebServices(request);
        } catch (ServiceException ee){
            return new WebServiceResponse(ee.getReturnCode(), ee.toJson());
        } catch (Exception ex){
            return new WebServiceResponse(500, ServiceException.json(ErrorCode.GENERAL, String.format("Exception when execute web service: %s", ex.getMessage()), request.toJson(), ex));
        }
    }

    /**
     * Convert the {@link Request} to a {@link WebServiceRequest} object
     *
     * @param path the target of the servletRequest
     * @param servletRequest the request either as the {@link Request} object or a wrapper of that servlet request.
     * @return the equivalent {@link WebServiceRequest} to the received {@link Request}
     */
    private WebServiceRequest prepareRequest(String path, HttpServletRequest servletRequest) {
        final RestMethod method = RestMethod.fromString(servletRequest.getMethod());


        final List<UploadedFile> files = new ArrayList<>();
        Object body = "";
        String rawBody = null;
        try {
            InputStream bodyInputStream = null;
            if(ContentTypeFormat.isMultipartContentType(servletRequest.getContentType())){

                final MultipartConfigElement multipartConfigElement = new MultipartConfigElement((String)null);
                servletRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfigElement);
                servletRequest.setCharacterEncoding(StandardCharsets.ISO_8859_1.name());

                for (Part part : servletRequest.getParts()) {
                    if(bodyInputStream == null && ContentTypeFormat.isJsonContentType(part.getContentType())){
                        bodyInputStream = part.getInputStream();
                    } else {
                        final Json headers = Json.map();
                        for (String header : part.getHeaderNames()) {
                            headers.setIfNotEmpty(header.toLowerCase(), part.getHeader(header));
                        }

                        final UploadedFile uploadedFile = new UploadedFile(part.getName(), part.getSubmittedFileName(), part.getContentType(), part.getSize(), part.getInputStream(), headers);
                        files.add(uploadedFile);
                    }
                }
            } else {
                bodyInputStream = servletRequest.getInputStream();
            }

            if(bodyInputStream != null){
                rawBody = Strings.readAsString(bodyInputStream);
                if(StringUtils.isNoneEmpty(rawBody)) {
                    body = JsonConverter.convertString(rawBody, servletRequest.getContentType(), false);
                    if (body == null) {
                        body = rawBody;
                    }
                }
            }

        } catch (Exception ex){
            logger.warn(String.format("Invalid body on web service servletRequest body: %s", ex.getMessage()), ex);
        }

        final Json headers = Json.map();
        Json.fromEnumeration(servletRequest.getHeaderNames()).forEachList(k -> headers.set(k.toString(), servletRequest.getHeader(k.toString())));

        final Json parameters = Strings.parseQueryString(Strings.urlDecode(servletRequest.getQueryString()), true);

        final Json requestInfo = Json.map();
        // recollect metadata
        try {
            requestInfo.set("method", servletRequest.getMethod());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("path", servletRequest.getPathInfo());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("query", servletRequest.getQueryString());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("contentType", servletRequest.getContentType());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("url", servletRequest.getRequestURL());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("uri", servletRequest.getRequestURI());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("encoding", servletRequest.getCharacterEncoding());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("localAddress", servletRequest.getLocalAddr());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("localName", servletRequest.getLocalName());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("scheme", servletRequest.getScheme());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("serverName", servletRequest.getServerName());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("remoteHost", servletRequest.getRemoteHost());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("remoteAddress", servletRequest.getRemoteAddr());
        } catch (Exception ex){
            // do nothing
        }
        try {
            requestInfo.set("protocol", servletRequest.getProtocol());
        } catch (Exception ex){
            // do nothing
        }

        WebServiceRequest webServiceRequest = new WebServiceRequest(method, path, body, headers, parameters, requestInfo, files);
        webServiceRequest.setRawBody(rawBody);
        return webServiceRequest;
    }

    /**
     * Process the {@link WebServiceResponse} object and send to an HTTP client via {@link Response}
     *
     * @param response the equivalent {@link WebServiceResponse} to the received response
     * @param servletResponse the response as the {@link Response} object or a wrapper of that servlet request.
     * @throws IOException if unable to handle the request or response processing
     */
    private void sendResponse(WebServiceResponse response, HttpServletResponse servletResponse) throws IOException {
        if(debug){
            logger.info(String.format("%s response %s", Service.DEBUG, response.toString()));
        }

        // headers
        response.getHeaders()
                .forEachMap((key, value) -> servletResponse.addHeader(key, value != null ? value.toString() : "true"));

        // content type
        String contentType = response.getStringHeader(Parameter.CONTENT_TYPE);
        if(StringUtils.isBlank(contentType)){
            contentType = ContentTypeFormat.JSON.getMimeType();
        }
        servletResponse.setContentType(contentType);

        // content
        final Object body = response.getBody();
        if(body != null) {
            if(body instanceof InputStream){
                FilesUtils.copyStreamAndFlush((InputStream) body, servletResponse.getOutputStream());
            } else if(body instanceof byte[]){
                FilesUtils.copyStreamAndFlush(new ByteArrayInputStream((byte[]) body), servletResponse.getOutputStream());
            } else if(body instanceof ByteArrayOutputStream){
                ((ByteArrayOutputStream) body).writeTo(servletResponse.getOutputStream());
            } else  {
                servletResponse.getWriter().print(body);
            }
        }

        // http code
        servletResponse.setStatus(response.getHttpCode());
    }

    /**
     * Setup the parameters to deal with exceptions
     *
     * @param maxRedelivers maximum retries of a request that throws an exception
     */
    void setupDefaultExceptionsProperties(int maxRedelivers){
        this.maxRedelivers = maxRedelivers >= 0 ? maxRedelivers : DEFAULT_MAX_REDELIVERS;
    }

    /**
     * Setup the parameters to deal with permanent exceptions
     *
     * @param maxRedelivers maximum retries of a request that throws a permanent exception
     * @param delay delay between retries
     */
    void setupPermanentExceptionsProperties(int maxRedelivers, long delay){
        this.permanentMaxRedelivers = maxRedelivers >= 0 ? maxRedelivers : DEFAULT_PERMANENT_MAX_REDELIVERS;
        this.permanentDelay = delay >= 0 ? delay : DEFAULT_PERMANENT_DELAY;
    }

    /**
     * Setup the parameters to deal with retryable exceptions
     *
     * @param maxRedelivers maximum retries of a request that throws a retryable exception
     * @param delay delay between retries
     */
    void setupRetryableExceptionsProperties(int maxRedelivers, long delay){
        this.retryableMaxRedelivers = maxRedelivers >= 0 ? maxRedelivers : DEFAULT_RETRYABLE_MAX_REDELIVERS;
        this.retryableDelay = delay >= 0 ? delay : DEFAULT_RETRYABLE_DELAY;
    }
}
