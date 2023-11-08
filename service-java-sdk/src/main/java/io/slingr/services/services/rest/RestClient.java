package io.slingr.services.services.rest;

import io.slingr.services.Service;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.services.Files;
import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.services.rest.authentication.AuthenticationType;
import io.slingr.services.utils.Base64Utils;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.Strings;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * REST client to help to consume external services
 *
 * <p>Created by lefunes on 14/05/15.
 */
public abstract class RestClient {
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000; // 5 seconds by default
    static final int DEFAULT_READ_TIMEOUT = 60 * 1000; // 60 seconds by default
    static final boolean DEFAULT_FOLLOW_REDIRECTS = true;
    private static final boolean DEFAULT_ALLOW_EXTERNAL_URL = false;
    private static final String BASIC_AUTH = "BASIC";
    private static final String DIGEST_AUTH = "DIGEST";
    protected static final String DEFAULT_FILE_NAME = "file";
    private static final String DEFAULT_EMPTY_PATH = "/";
    static final String DEFAULT_UPLOAD_PARAMETER = Parameter.FILE_UPLOAD_PARAMETER;
    static final String DEFAULT_UPLOAD_BODY = Parameter.FILE_UPLOAD_BODY;
    public static final String BASIC_AUTHENTICATION_HEADER = "Basic";
    public static final String BEARER_AUTHENTICATION_HEADER = "Bearer";
    public static final String OAUTH_AUTHENTICATION_HEADER = "OAuth";
    public static int DEFAULT_MAX_REDIRECTS = 10;

    protected boolean debug = false;
    private boolean skipEncodeParams = false;
    private RestClientFactory factory;
    private WebTarget apiTarget;
    private final Map<String, Object> apiHeaders = new HashMap<>();
    private final Map<String, String> apiParams = new HashMap<>();
    private Integer connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private Integer readTimeout = DEFAULT_READ_TIMEOUT;
    private boolean followRedirects = DEFAULT_FOLLOW_REDIRECTS;
    private String defaultEmptyPath = DEFAULT_EMPTY_PATH;
    private boolean allowExternalUrl = DEFAULT_ALLOW_EXTERNAL_URL;
    private String uploadParameter = DEFAULT_UPLOAD_PARAMETER;
    private String uploadBody = DEFAULT_UPLOAD_BODY;
    private String defaultAuth = null;
    private String authUsername = null;
    private String authPassword = null;

    private static RestClientFactory defaultFactory = null;
    private static final ReentrantLock defaultFactoryLock = new ReentrantLock();

    /**
     * Constructs a RestClient with default configuration
     *
     * @param apiUri base URI to build the requests to the API.
     */
    public RestClient(String apiUri) throws ServiceException {
        if (StringUtils.isBlank(apiUri)) {
            apiUri = "";
        }
        this.factory = new RestClientFactory();
        this.apiTarget = factory.uri(apiUri);
    }

    /**
     * Constructs a RestClient with default configuration
     *
     * @param apiUri base URI to build the requests to the API.
     */
    public RestClient(String apiUri, RestClientFactory factory) throws ServiceException {
        if (StringUtils.isBlank(apiUri)) {
            apiUri = "";
        }
        this.factory = factory;
        this.apiTarget = factory.uri(apiUri);
    }

    /**
     * Rest client builder to use over an URI in a unique request
     *
     * @param apiUri base URI to build the requests to the API.
     */
    public static RestClientBuilder builder(String apiUri) {
        return new RestClientBuilder(apiUri, new RestClientFactory());
    }

    /**
     * Restart status of client factory and target
     */
    public void restartClient() {
        this.factory = new RestClientFactory();
        this.apiTarget = factory.uri(this.apiTarget.getUri().toString());
    }

    /**
     * Enables the debug logging
     *
     * @param debug true to enable debug logging
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        this.factory.setDebug(debug);
    }

    /**
     * Disable encode params
     *
     * @param skipEncodeParams
     */
    public void setSkipEncodeParams(boolean skipEncodeParams) {
        this.skipEncodeParams = skipEncodeParams;
    }

    /**
     * Gets the configured api target
     *
     * @return api target
     */
    public WebTarget getApiTarget() {
        return apiTarget;
    }

    /**
     * Configures basic authentication in the client so calls will use it.
     *
     * @param username the username to authenticate
     * @param password the password of the user
     */
    public void setupBasicAuthentication(String username, String password) {
        this.defaultAuth = BASIC_AUTH;
        this.authUsername = username;
        this.authPassword = password;

        this.apiTarget = factory.setupBasicAuthentication(apiTarget, username, password);
    }

    /**
     * Configures digest authentication in the client so calls will use it.
     *
     * @param username the username to authenticate
     * @param password the password of the user
     */
    public void setupDigestAuthentication(String username, String password) {
        this.defaultAuth = DIGEST_AUTH;
        this.authUsername = username;
        this.authPassword = password;

        this.apiTarget = factory.setupDigestAuthentication(apiTarget, username, password);
    }


    public void setupAuthentication(Map<String, String> params) {
        AuthenticationType authenticationType = AuthenticationType.fromType(params);
        this.apiTarget = factory.setupAuthentication(apiTarget, authenticationType, params);
    }

    /**
     * Configures basic authentication as a header in the client so calls will use it.
     *
     * @param username the username to authenticate
     * @param password the password of the user
     */
    public void setupBasicAuthenticationHeader(String username, String password) {
        setupAuthenticationHeader(BASIC_AUTHENTICATION_HEADER, username, password);
    }

    /**
     * Configures basic authentication as a header in the client so calls will use it.
     *
     * @param authorizationToken token generated between the user name and password values
     */
    public void setupBasicAuthenticationHeader(String authorizationToken) {
        setupAuthenticationHeader(BASIC_AUTHENTICATION_HEADER, authorizationToken);
    }

    /**
     * Configures bearer authentication as a header in the client so calls will use it.
     *
     * @param username the username to authenticate
     * @param password the password of the user
     */
    public void setupBearerAuthenticationHeader(String username, String password) {
        setupAuthenticationHeader(BEARER_AUTHENTICATION_HEADER, username, password);
    }

    /**
     * Configures bearer authentication as a header in the client so calls will use it.
     *
     * @param authorizationToken token generated between the user name and password values
     */
    public void setupBearerAuthenticationHeader(String authorizationToken) {
        setupAuthenticationHeader(BEARER_AUTHENTICATION_HEADER, authorizationToken);
    }

    /**
     * Configures authentication as a header in the client so calls will use it.
     *
     * @param type     type of authentication to include in the header, per example {@code Basic}, {@code Bearer}, etc
     * @param username the username to authenticate
     * @param password the password of the user
     */
    public void setupAuthenticationHeader(String type, String username, String password) {
        final String authorizationToken = Base64Utils.encodeBasicAuthorization(username, password);
        setupAuthenticationHeader(type, authorizationToken);
    }

    /**
     * Configures authentication as a header in the client so calls will use it.
     *
     * @param type               type of authentication to include in the header, per example {@code Basic}, {@code Bearer}, etc
     * @param authorizationToken token generated between the user name and password values
     */
    public void setupAuthenticationHeader(String type, String authorizationToken) {
        setupDefaultHeader("Authorization", String.format("%s %s", StringUtils.isNotBlank(type) ? type.trim() : BASIC_AUTHENTICATION_HEADER, authorizationToken));
    }

    /**
     * True if the client save and send cookies automatically
     *
     * @param rememberCookies true if want to remember cookies between requests
     */
    public void setRememberCookies(boolean rememberCookies) {
        factory.setRememberCookies(rememberCookies);
    }

    /**
     * Configures a parameter that will be sent in all requests. For example a token or format value is a typical
     * case where you need this.
     *
     * @param name  parameter name
     * @param value parameter value
     */
    public void setupDefaultParam(String name, String value) {
        this.apiParams.put(name, value);
        this.apiTarget = this.apiTarget.queryParam(name, Strings.urlEncode(value));
    }

    /**
     * Configures a path to the base target API
     *
     * @param path path to add to api target
     */
    protected void setPath(String path) {
        // add path
        if (StringUtils.isNotBlank(path)) {
            this.apiTarget = this.apiTarget.path(path);
        }
    }

    /**
     * Generates a new target pointing to the path
     *
     * @return path target
     */
    protected WebTarget target(String path) {
        return this.apiTarget.path(path);
    }

    /**
     * Generates a new target pointing to the path
     *
     * @return path target
     */
    protected WebTarget target(String path, Json parameters) {
        WebTarget response = target(path);
        if (response != null && parameters != null && parameters.isMap()) {
            for (Map.Entry<String, Object> entry : parameters.toMap().entrySet()) {
                response = response.queryParam(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : true);
            }
        }
        return response;
    }

    /**
     * Configures a header that will be sent in all requests. For example a token is a typical case where you
     * need this.
     *
     * @param name  name of header
     * @param value header value
     */
    public void setupDefaultHeader(String name, Object value) {
        this.apiHeaders.put(name, value);
    }

    /**
     * Gets a default header that will be sent in all requests.
     *
     * @param name name of header
     * @return header value
     */
    public Object getDefaultHeader(String name) {
        return this.apiHeaders.get(name);
    }

    /**
     * Gets a default headers
     *
     * @return default headers
     */
    public Map<String, Object> getDefaultHeaders() {
        return this.apiHeaders;
    }

    /**
     * Removes a default header.
     *
     * @param name name of header
     */
    public void removeDefaultHeader(String name) {
        this.apiHeaders.remove(name);
    }

    /**
     * Gets read timeout interval, in milliseconds.
     *
     * @return read timeout in milliseconds
     */
    public Integer getReadTimeout() {
        return readTimeout;
    }

    /**
     * Set read timeout interval, in milliseconds. The default value is infinity (0).
     *
     * @param readTimeout read timeout in milliseconds
     */
    public void setReadTimeout(Integer readTimeout) {
        if (readTimeout != null && readTimeout >= 0) {
            this.readTimeout = readTimeout;
        } else {
            this.readTimeout = null;
        }
    }

    /**
     * Gets connect timeout interval, in milliseconds.
     *
     * @return connection timeout in milliseconds
     */
    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Set connect timeout interval, in milliseconds. The default value is infinity (0).
     *
     * @param connectionTimeout connection timeout in milliseconds
     */
    public void setConnectionTimeout(Integer connectionTimeout) {
        if (connectionTimeout != null && connectionTimeout >= 0) {
            this.connectionTimeout = connectionTimeout;
        } else {
            this.connectionTimeout = null;
        }
    }

    /**
     * Enable/disable the automatic redirection. The default value is {@code true}.
     *
     * @param followRedirects true if enable automatic redirection.
     */
    public void setFollowRedirects(Boolean followRedirects) {
        this.followRedirects = Boolean.TRUE.equals(followRedirects);
    }

    /**
     * Configures the default path for requests with an empty path. By default, is '/'
     *
     * @param defaultEmptyPath default for requests with an empty path
     */
    public void setDefaultEmptyPath(String defaultEmptyPath) {
        if (defaultEmptyPath != null) {
            this.defaultEmptyPath = defaultEmptyPath.trim();
        } else {
            this.defaultEmptyPath = DEFAULT_EMPTY_PATH;
        }
    }

    /**
     * True if the client accept external URLs
     *
     * @param allowExternalUrl true if the client accept external URLs
     */
    public void setAllowExternalUrl(boolean allowExternalUrl) {
        this.allowExternalUrl = allowExternalUrl;
    }

    /**
     * Sets the name of the parameter used to upload a file to the REST service
     *
     * @param uploadParameter name of the parameter to use when upload a file to the REST service
     */
    public void setUploadParameter(String uploadParameter) {
        this.uploadParameter = uploadParameter;
    }

    /**
     * Sets the name of the body part used to upload a file to the REST service
     *
     * @param uploadBody name of the body part to use when upload a file to the REST service
     */
    public void setUploadBody(String uploadBody) {
        this.uploadBody = uploadBody;
    }

    /**
     * Disables some options to avoid issues per example when using the websocket over SSL
     */
    public void setDisabledSslOptions() {
        System.setProperty("com.sun.net.ssl.enableECC", "false");
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // GET methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Perform a GET request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpGet(Json request) throws ServiceException {
        return httpGet(HttpRequest.fromJson(RestMethod.GET, request));
    }

    /**
     * Perform a GET request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpGet(HttpRequest request) throws ServiceException {
        return executeHttpRequest(RestMethod.GET, request);
    }

    /**
     * Perform a GET request with the default target information.
     *
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json get() throws ServiceException {
        return get(null);
    }

    /**
     * Perform a GET request with the target information.
     *
     * @param target target of the request
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json get(WebTarget target) throws ServiceException {
        return get(target, null, false);
    }

    /**
     * Perform a GET request with the default target information.
     *
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json get(boolean fullResponse) throws ServiceException {
        return get((WebTarget) null, fullResponse);
    }

    /**
     * Perform a GET request with the target information.
     *
     * @param target       target of the request
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json get(WebTarget target, boolean fullResponse) throws ServiceException {
        return get(target, null, fullResponse);
    }

    /**
     * Perform a GET request with the target information and without content.
     *
     * @param target  target of the request
     * @param headers headers of HTTP request. the header on target with the same name will be override by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json get(WebTarget target, Json headers) throws ServiceException {
        return get(target, headers, false);
    }

    /**
     * Perform a GET request with the default target information and without content.
     *
     * @param headers      headers of HTTP request. the header on target with the same name will be override by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json get(Json headers, boolean fullResponse) throws ServiceException {
        return get(null, headers, fullResponse);
    }

    /**
     * Perform a GET request with the target information and without content.
     *
     * @param target       target of the request
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json get(WebTarget target, Json headers, boolean fullResponse) throws ServiceException {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFullResponse(fullResponse)
                .build();

        return get(target, headers, request);
    }

    /**
     * Perform a GET request with the target information and without content.
     *
     * @param target  target of the request
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json get(WebTarget target, Json headers, HttpRequest request) throws ServiceException {
        return execute(RestMethod.GET, target, null, headers, request);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // POST methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Perform a POST request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpPost(Json request) throws ServiceException {
        return httpPost(HttpRequest.fromJson(RestMethod.POST, request));
    }

    /**
     * Perform a POST request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpPost(HttpRequest request) throws ServiceException {
        return executeHttpRequest(RestMethod.POST, request);
    }

    /**
     * Perform a POST request with the default target information and without content.
     *
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post() throws ServiceException {
        return post(null);
    }

    /**
     * Perform a POST request with the target information and without content.
     *
     * @param target target of the request
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(WebTarget target) throws ServiceException {
        return post(target, null, null, false);
    }

    /**
     * Perform a POST request with the default target information and without content.
     *
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(boolean fullResponse) throws ServiceException {
        return post(null, fullResponse);
    }

    /**
     * Perform a POST request with the target information and without content.
     *
     * @param target       target of the request
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(WebTarget target, boolean fullResponse) throws ServiceException {
        return post(target, null, null, fullResponse);
    }

    /**
     * Perform a POST request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content body of the HTTP request.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(Object content) throws ServiceException {
        return post(null, content);
    }

    /**
     * Perform a POST request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target  target of the request
     * @param content body of the HTTP request.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(WebTarget target, Object content) throws ServiceException {
        return post(target, content, null, false);
    }

    /**
     * Perform a POST request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content      body of the HTTP request.
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(Object content, boolean fullResponse) throws ServiceException {
        return post(null, content, fullResponse);
    }

    /**
     * Perform a POST request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target       target of the request
     * @param content      body of the HTTP request.
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(WebTarget target, Object content, boolean fullResponse) throws ServiceException {
        return post(target, content, null, fullResponse);
    }

    /**
     * Perform a POST request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target  target of the request
     * @param content body of the HTTP request.
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(WebTarget target, Object content, Json headers) throws ServiceException {
        return post(target, content, headers, false);
    }

    /**
     * Perform a POST request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content      body of the HTTP request.
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(Object content, Json headers, boolean fullResponse) throws ServiceException {
        return post(null, content, headers, fullResponse);
    }

    /**
     * Perform a POST request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target       target of the request
     * @param content      body of the HTTP request.
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(WebTarget target, Object content, Json headers, boolean fullResponse) throws ServiceException {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFullResponse(fullResponse)
                .build();

        return post(target, content, headers, request);
    }

    /**
     * Perform a POST request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target  target of the request
     * @param content body of the HTTP request.
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json post(WebTarget target, Object content, Json headers, HttpRequest request) throws ServiceException {
        return execute(RestMethod.POST, target, content, headers, request);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // PUT methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Perform a PUT request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpPut(Json request) throws ServiceException {
        return httpPut(HttpRequest.fromJson(RestMethod.PUT, request));
    }

    /**
     * Perform a PUT request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpPut(HttpRequest request) throws ServiceException {
        return executeHttpRequest(RestMethod.PUT, request);
    }

    /**
     * Perform a PUT request with the default target information and without content.
     *
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put() throws ServiceException {
        return put(null);
    }

    /**
     * Perform a PUT request with the target information and without content.
     *
     * @param target target of the request
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(WebTarget target) throws ServiceException {
        return put(target, null, null, false);
    }

    /**
     * Perform a PUT request with the default target information and without content.
     *
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(boolean fullResponse) throws ServiceException {
        return put(null, fullResponse);
    }

    /**
     * Perform a PUT request with the target information and without content.
     *
     * @param target       target of the request
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(WebTarget target, boolean fullResponse) throws ServiceException {
        return put(target, null, null, fullResponse);
    }

    /**
     * Perform a PUT request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content body of the HTTP request.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(Object content) throws ServiceException {
        return put(null, content);
    }

    /**
     * Perform a PUT request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target  target of the request
     * @param content body of the HTTP request.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(WebTarget target, Object content) throws ServiceException {
        return put(target, content, null, false);
    }

    /**
     * Perform a PUT request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content      body of the HTTP request.
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(Object content, boolean fullResponse) throws ServiceException {
        return put(null, content, fullResponse);
    }

    /**
     * Perform a PUT request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target       target of the request
     * @param content      body of the HTTP request.
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(WebTarget target, Object content, boolean fullResponse) throws ServiceException {
        return put(target, content, null, fullResponse);
    }

    /**
     * Perform a PUT request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target  target of the request
     * @param content body of the HTTP request.
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(WebTarget target, Object content, Json headers) throws ServiceException {
        return put(target, content, headers, false);
    }

    /**
     * Perform a PUT request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content      body of the HTTP request.
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(Object content, Json headers, boolean fullResponse) throws ServiceException {
        return put(null, content, headers, fullResponse);
    }

    /**
     * Perform a PUT request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target       target of the request
     * @param content      body of the HTTP request.
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(WebTarget target, Object content, Json headers, boolean fullResponse) throws ServiceException {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFullResponse(fullResponse)
                .build();

        return put(target, content, headers, request);
    }

    /**
     * Perform a PUT request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target  target of the request
     * @param content body of the HTTP request.
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json put(WebTarget target, Object content, Json headers, HttpRequest request) throws ServiceException {
        return execute(RestMethod.PUT, target, content, headers, request);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // PATCH methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Perform a PATCH request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpPatch(Json request) throws ServiceException {
        return httpPatch(HttpRequest.fromJson(RestMethod.PATCH, request));
    }

    /**
     * Perform a PATCH request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpPatch(HttpRequest request) throws ServiceException {
        return executeHttpRequest(RestMethod.PATCH, request);
    }

    /**
     * Perform a PATCH request with the default target information and without content.
     *
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch() throws ServiceException {
        return patch(null);
    }

    /**
     * Perform a PATCH request with the target information and without content.
     *
     * @param target target of the request
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(WebTarget target) throws ServiceException {
        return patch(target, null, null, false);
    }

    /**
     * Perform a PATCH request with the default target information and without content.
     *
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(boolean fullResponse) throws ServiceException {
        return patch(null, fullResponse);
    }

    /**
     * Perform a PATCH request with the target information and without content.
     *
     * @param target       target of the request
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(WebTarget target, boolean fullResponse) throws ServiceException {
        return patch(target, null, null, fullResponse);
    }

    /**
     * Perform a PATCH request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content body of the HTTP request.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(Object content) throws ServiceException {
        return patch(null, content);
    }

    /**
     * Perform a PATCH request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target  target of the request
     * @param content body of the HTTP request.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(WebTarget target, Object content) throws ServiceException {
        return patch(target, content, null, false);
    }

    /**
     * Perform a PATCH request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content      body of the HTTP request.
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(Object content, boolean fullResponse) throws ServiceException {
        return patch(null, content, fullResponse);
    }

    /**
     * Perform a PATCH request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target       target of the request
     * @param content      body of the HTTP request.
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(WebTarget target, Object content, boolean fullResponse) throws ServiceException {
        return patch(target, content, null, fullResponse);
    }

    /**
     * Perform a PATCH request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content body of the HTTP request.
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(Object content, Json headers) throws ServiceException {
        return patch(null, content, headers);
    }

    /**
     * Perform a PATCH request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target  target of the request
     * @param content body of the HTTP request.
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(WebTarget target, Object content, Json headers) throws ServiceException {
        return patch(target, content, headers, false);
    }

    /**
     * Perform a PATCH request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content      body of the HTTP request.
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(Object content, Json headers, boolean fullResponse) throws ServiceException {
        return patch(null, content, headers, fullResponse);
    }

    /**
     * Perform a PATCH request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target       target of the request
     * @param content      body of the HTTP request.
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(WebTarget target, Object content, Json headers, boolean fullResponse) throws ServiceException {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFullResponse(fullResponse)
                .build();

        return patch(target, content, headers, request);

    }

    /**
     * Perform a PATCH request with the target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param target  target of the request
     * @param content body of the HTTP request.
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json patch(WebTarget target, Object content, Json headers, HttpRequest request) throws ServiceException {
        return execute(RestMethod.PATCH, target, content, headers, request);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // DELETE methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Perform a DELETE request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpDelete(Json request) throws ServiceException {
        return httpDelete(HttpRequest.fromJson(RestMethod.DELETE, request));
    }

    /**
     * Perform a DELETE request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpDelete(HttpRequest request) throws ServiceException {
        return executeHttpRequest(RestMethod.DELETE, request);
    }

    /**
     * Perform a DELETE request with the default target information and without content
     *
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json delete() throws ServiceException {
        return delete(null);
    }

    /**
     * Perform a DELETE request with the target information and without content
     *
     * @param target target of the request
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json delete(WebTarget target) throws ServiceException {
        return delete(target, null, false);
    }

    /**
     * Perform a DELETE request with the default target information and without content
     *
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json delete(boolean fullResponse) throws ServiceException {
        return delete((WebTarget) null, fullResponse);
    }

    /**
     * Perform a DELETE request with the target information and without content
     *
     * @param target       target of the request
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json delete(WebTarget target, boolean fullResponse) throws ServiceException {
        return delete(target, null, fullResponse);
    }

    /**
     * Perform a DELETE request with the target information and without content
     *
     * @param target  target of the request
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json delete(WebTarget target, Json headers) throws ServiceException {
        return delete(target, headers, false);
    }

    /**
     * Perform a DELETE request with the default target information and without content
     *
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json delete(Json headers, boolean fullResponse) throws ServiceException {
        return delete(null, headers, fullResponse);
    }

    /**
     * Perform a DELETE request with the target information and without content
     *
     * @param target       target of the request
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json delete(WebTarget target, Json headers, boolean fullResponse) throws ServiceException {
        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFullResponse(fullResponse)
                .build();

        return delete(target, headers, request);
    }

    /**
     * Perform a DELETE request with the target information and without content
     *
     * @param target  target of the request
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json delete(WebTarget target, Json headers, HttpRequest request) throws ServiceException {
        return execute(RestMethod.DELETE, target, null, headers, request);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // HEAD methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Perform a HEAD request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpHead(Json request) throws ServiceException {
        return httpHead(HttpRequest.fromJson(RestMethod.HEAD, request));
    }

    /**
     * Perform a HEAD request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpHead(HttpRequest request) throws ServiceException {
        return executeHttpRequest(RestMethod.HEAD, request);
    }

    /**
     * Perform a HEAD request with the default target information
     *
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json head() throws ServiceException {
        return head(null);
    }

    /**
     * Perform a HEAD request with the target information
     *
     * @param target target of the request
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json head(WebTarget target) throws ServiceException {
        return head(target, null, false);
    }

    /**
     * Perform a HEAD request with the default target information
     *
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json head(boolean fullResponse) throws ServiceException {
        return head((WebTarget) null, fullResponse);
    }

    /**
     * Perform a HEAD request with the target information
     *
     * @param target       target of the request
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json head(WebTarget target, boolean fullResponse) throws ServiceException {
        return head(target, null, fullResponse);
    }

    /**
     * Perform a HEAD request with the target information
     *
     * @param target  target of the request
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json head(WebTarget target, Json headers) throws ServiceException {
        return head(target, headers, false);
    }

    /**
     * Perform a HEAD request with the default target information
     *
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json head(Json headers, boolean fullResponse) throws ServiceException {
        return head(null, headers, fullResponse);
    }

    /**
     * Perform a HEAD request with the target information
     *
     * @param target       target of the request
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json head(WebTarget target, Json headers, boolean fullResponse) throws ServiceException {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFullResponse(fullResponse)
                .build();

        return head(target, headers, request);
    }

    /**
     * Perform a HEAD request with the target information
     *
     * @param target  target of the request
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json head(WebTarget target, Json headers, HttpRequest request) throws ServiceException {
        return execute(RestMethod.HEAD, target, null, headers, request);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // OPTIONS methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Perform a OPTIONS request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpOptions(Json request) throws ServiceException {
        return httpOptions(HttpRequest.fromJson(RestMethod.OPTIONS, request));
    }

    /**
     * Perform a OPTIONS request from a HTTP request.
     *
     * @param request request to be processed
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpOptions(HttpRequest request) throws ServiceException {
        return executeHttpRequest(RestMethod.OPTIONS, request);
    }

    /**
     * Perform a OPTIONS request with the default target information
     *
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json options() throws ServiceException {
        return options(null);
    }

    /**
     * Perform a OPTIONS request with the target information
     *
     * @param target target of the request
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json options(WebTarget target) throws ServiceException {
        return options(target, null, false);
    }

    /**
     * Perform a OPTIONS request with the default target information
     *
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json options(boolean fullResponse) throws ServiceException {
        return options((WebTarget) null, fullResponse);
    }

    /**
     * Perform a OPTIONS request with the target information
     *
     * @param target       target of the request
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json options(WebTarget target, boolean fullResponse) throws ServiceException {
        return options(target, null, fullResponse);
    }

    /**
     * Perform a OPTIONS request with the target information
     *
     * @param target  target of the request
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json options(WebTarget target, Json headers) throws ServiceException {
        return options(target, headers, false);
    }

    /**
     * Perform a OPTIONS request with the default target information
     *
     * @param headers      headers of HTTP request. the header on target with the same name will be override by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json options(Json headers, boolean fullResponse) throws ServiceException {
        return options(null, headers, fullResponse);
    }

    /**
     * Perform a OPTIONS request with the target information
     *
     * @param target       target of the request
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json options(WebTarget target, Json headers, boolean fullResponse) throws ServiceException {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFullResponse(fullResponse)
                .build();

        return options(target, headers, request);

    }

    /**
     * Perform a OPTIONS request with the target information
     *
     * @param target  target of the request
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json options(WebTarget target, Json headers, HttpRequest request) throws ServiceException {
        return execute(RestMethod.OPTIONS, target, null, headers, request);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // DOWNLOAD methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Perform a GET request from an HTTP request in order to download the remote HTTP resource.
     *
     * @param request request to be processed
     * @return processed downloaded file
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public DownloadedFile httpDownload(Json request) throws ServiceException {
        return httpDownload(HttpRequest.fromJson(RestMethod.GET, request));
    }

    /**
     * Perform a GET request from an HTTP request in order to download the remote HTTP resource.
     *
     * @param request request to be processed
     * @return processed downloaded file
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public DownloadedFile httpDownload(HttpRequest request) throws ServiceException {
        return downloadHttpRequest(request);
    }

    /**
     * Perform a GET request in order to download the remote HTTP resource
     *
     * @return processed downloaded file
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public DownloadedFile download() throws ServiceException {
        return download(null, null);
    }

    /**
     * Perform a GET request in order to download the remote HTTP resource
     *
     * @param target target of the request
     * @return processed downloaded file
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public DownloadedFile download(WebTarget target) throws ServiceException {
        return download(target, null);
    }

    /**
     * Perform a GET request in order to download the remote HTTP resource
     *
     * @param target  target of the request
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed downloaded file
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public DownloadedFile download(WebTarget target, Json headers) throws ServiceException {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .build();

        return download(target, headers, request);
    }

    /**
     * Perform a GET request in order to download the remote HTTP resource
     *
     * @param target  target of the request
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed downloaded file
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public DownloadedFile download(WebTarget target, Json headers, HttpRequest request) throws ServiceException {
        final Response response = request(RestMethod.GET, target, null, headers, request);
        return factory.processDownloadedFile(response);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // UPLOAD methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Perform a multipart POST request to a remote HTTP service in order. Usually needed when uploading
     * files.
     *
     * @param request request to be processed
     * @param files   the files service to download files from the app
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpMultipart(Json request, Files files) throws ServiceException {
        return httpMultipart(HttpRequest.fromJson(RestMethod.POST, request), files);
    }

    /**
     * Perform a multipart request to a remote HTTP service in order. Usually needed when uploading
     * files.
     *
     * @param request request to be processed
     * @param files   the files service to download files from the app
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json httpMultipart(HttpRequest request, Files files) throws ServiceException {
        if (request == null) {
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "Invalid request");
        }
        try {
            request.setBody(factory.processMultipart(request, files));
            if (request.getRestMethod() == RestMethod.POST) {
                return httpPost(request);
            } else if (request.getRestMethod() == RestMethod.PUT) {
                return httpPut(request);
            } else {
                throw ServiceException.permanent(ErrorCode.API, "HTTP method not supported for multipart requests");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn(String.format("An exception occurs when try to upload a file [%s] - Exception: %s", request.getPath(), ex.getMessage()), ex);
            throw ServiceException.permanent(ErrorCode.CLIENT, ex.getMessage(), ex);
        }
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param inputStream file part of the HTTP multipart request
     * @param filename    filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(InputStream inputStream, String filename, String contentType) throws ServiceException {
        return upload(null, inputStream, filename, contentType);
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param target      target of the request
     * @param inputStream file part of the HTTP multipart request
     * @param filename    filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(WebTarget target, InputStream inputStream, String filename, String contentType) throws ServiceException {
        return upload(target, false, inputStream, filename, contentType);
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param fullResponse true if the response must include extended information about response
     * @param inputStream  file part of the HTTP multipart request
     * @param filename     filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType  MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(boolean fullResponse, InputStream inputStream, String filename, String contentType) throws ServiceException {
        return upload(null, fullResponse, inputStream, filename, contentType);
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param target       target of the request
     * @param fullResponse true if the response must include extended information about response
     * @param inputStream  file part of the HTTP multipart request
     * @param filename     filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType  MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(WebTarget target, boolean fullResponse, InputStream inputStream, String filename, String contentType) throws ServiceException {
        return upload(target, null, null, fullResponse, inputStream, filename, contentType);
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param content     body part of the HTTP multipart request
     * @param inputStream file part of the HTTP multipart request
     * @param filename    filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(Object content, InputStream inputStream, String filename, String contentType) throws ServiceException {
        return upload(null, content, inputStream, filename, contentType);
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param target      target of the request
     * @param content     body part of the HTTP multipart request
     * @param inputStream file part of the HTTP multipart request
     * @param filename    filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(WebTarget target, Object content, InputStream inputStream, String filename, String contentType) throws ServiceException {
        return upload(target, content, false, inputStream, filename, contentType);
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param content      body part of the HTTP multipart request
     * @param fullResponse true if the response must include extended information about response
     * @param inputStream  file part of the HTTP multipart request
     * @param filename     filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType  MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(Object content, boolean fullResponse, InputStream inputStream, String filename, String contentType) throws ServiceException {
        return upload(null, content, fullResponse, inputStream, filename, contentType);
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param target       target of the request
     * @param content      body part of the HTTP multipart request
     * @param fullResponse true if the response must include extended information about response
     * @param inputStream  file part of the HTTP multipart request
     * @param filename     filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType  MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(WebTarget target, Object content, boolean fullResponse, InputStream inputStream, String filename, String contentType) throws ServiceException {
        return upload(target, content, null, fullResponse, inputStream, filename, contentType);
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param content      body part of the HTTP multipart request
     * @param headers      headers of HTTP request. the header on target with the same name will be overridden by these
     *                     properties
     * @param fullResponse true if the response must include extended information about response
     * @param inputStream  file part of the HTTP multipart request
     * @param filename     filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType  MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(Object content, Json headers, boolean fullResponse, InputStream inputStream, String filename, String contentType) throws ServiceException {
        return upload(null, content, headers, fullResponse, inputStream, filename, contentType);
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param target      target of the request
     * @param content     body part of the HTTP multipart request
     * @param headers     headers of HTTP request. the header on target with the same name will be overridden by these
     *                    properties
     * @param inputStream file part of the HTTP multipart request
     * @param filename    filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(WebTarget target, Object content, Json headers, boolean fullResponse, InputStream inputStream, String filename, String contentType) throws ServiceException {
        return upload(target, content, headers, fullResponse, null, null, null, null, inputStream, filename, contentType, null, null);
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param target            target of the request
     * @param content           body part of the HTTP multipart request
     * @param headers           headers of HTTP request. the header on target with the same name will be overridden by these
     *                          properties
     * @param fullResponse      true if the response must include extended information about response
     * @param connectionTimeout connect timeout interval, in milliseconds. null to use the default value (0: infinity).
     * @param readTimeout       read timeout interval, in milliseconds. null to use the default value (0: infinity).
     * @param followRedirects   automatic redirection. A value of {@code true} declares that the client will automatically
     *                          redirect to the URI declared in 3xx responses.
     * @param inputStream       file part of the HTTP multipart request
     * @param filename          filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType       MIME type of the {@code streamEntity} attachment.
     * @param uploadParameter   name of the parameter to use when upload a file to the REST service
     * @param uploadBody        name of the body part to use when upload a file to the REST service
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    public Json upload(WebTarget target, Object content, Json headers, boolean fullResponse, Integer connectionTimeout, Integer readTimeout, Boolean followRedirects, Boolean forceDisableCookies,
                       InputStream inputStream, String filename, String contentType, String uploadParameter, String uploadBody
    ) throws ServiceException {
        uploadParameter = uploadParameter != null ? uploadParameter : this.uploadParameter;
        uploadBody = uploadBody != null ? uploadBody : this.uploadBody;

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFullResponse(fullResponse)
                .setConnectionTimeout(connectionTimeout != null ? connectionTimeout : RestClient.DEFAULT_CONNECTION_TIMEOUT )
                .setReadTimeout(readTimeout != null ? readTimeout : RestClient.DEFAULT_READ_TIMEOUT)
                .setFollowRedirects(followRedirects != null ? followRedirects : RestClient.DEFAULT_FOLLOW_REDIRECTS)
                .setForceDisableCookies(forceDisableCookies != null ? forceDisableCookies : false)
                .build();

        try {
            content = factory.processUploadFile(inputStream, filename, contentType, uploadParameter, uploadBody, content);
            return execute(RestMethod.POST, target, content, headers, request);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn(String.format("An exception occurs when try to upload a file - Exception: %s", ex.getMessage()), ex);
            throw ServiceException.permanent(ErrorCode.CLIENT, ex.getMessage(), ex);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // helper methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Perform the specified HTTP request to the target from an HTTP request.
     *
     * @param request HTTP request
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    protected DownloadedFile downloadHttpRequest(HttpRequest request) throws ServiceException {
        final Response response = requestHttpRequest(RestMethod.GET, request);
        return factory.processDownloadedFile(response);
    }

    /**
     * Perform the specified HTTP request to the target from an HTTP request.
     *
     * @param method  HTTP method to execute on request
     * @param request HTTP request
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    protected Json executeHttpRequest(final RestMethod method, HttpRequest request) throws ServiceException {
        final Response response = requestHttpRequest(method, request);
        return factory.processResponse(response, method, request != null && request.isFullResponse());
    }

    /**
     * Perform the specified HTTP request to the target from an HTTP request.
     *
     * @param method  HTTP method to execute on request
     * @param request HTTP request
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    private Response requestHttpRequest(final RestMethod method, HttpRequest request) throws ServiceException {
        if (request == null) {
            request = new HttpRequest();
        }

        String path = request.getPath();
        if (StringUtils.isBlank(path)) {
            path = defaultEmptyPath;
        }
        path = path.trim();

        final Json params = request.getParams();
        if (path.contains("?")) {
            final String pq = path.substring(path.indexOf("?") + 1);
            path = path.substring(0, path.indexOf("?"));

            try {
                Strings.parseQueryString(pq).forEachMap(params::set);
            } catch (Exception e) {
                if (debug) {
                    logger.info(String.format("%s Exception when process HTTP request params: %s", Service.DEBUG, e.getMessage()));
                } else {
                    logger.debug("Exception when process HTTP request params: " + e.getMessage());
                }
            }
        }

        WebTarget target;
        if (path.startsWith("http://") || path.startsWith("https://")) {
            // external uri
            if (!allowExternalUrl) {
                throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("External URLs are not allowed as request path [%s]", path));
            } else {
                RestClientBuilder simpleClient = RestClient.builder(path);
                for (Map.Entry<String, Object> header : apiHeaders.entrySet()) {
                    simpleClient = simpleClient.header(header.getKey(), header.getValue());
                }
                for (Map.Entry<String, String> param : apiParams.entrySet()) {
                    simpleClient = simpleClient.parameter(param.getKey(), param.getValue());
                }
                if (request.getAuthorization().isNotEmpty()) {
                    simpleClient.setupAuthentication(request.getAuthorization().toMapString());
                }
                target = simpleClient.target();
            }
        } else {
            target = getApiTarget().path(path);
        }
        if (!params.isEmpty()) {
            this.skipEncodeParams = request.isEncodeUrl() == null ? this.skipEncodeParams : !request.isEncodeUrl();
            for (String key : params.keys()) {
                Object oValue = params.object(key);
                if (oValue != null) {
                    if (!(oValue instanceof String) && params.isList(key)) {
                        target = target.queryParam(key, skipEncodeParams ? params.strings(key).toArray() : Strings.urlEncode(params.strings(key)).toArray());
                    } else {
                        target = target.queryParam(key, skipEncodeParams ? oValue.toString() : Strings.urlEncode(oValue.toString()));
                    }
                }
            }
        }
        target = target.property(ClientProperties.FOLLOW_REDIRECTS, followRedirects);

        Object body = null;
        if (method == RestMethod.POST || method == RestMethod.PUT || method == RestMethod.PATCH) {
            body = request.getBody();
        }

        return request(method, target, body, request.getHeaders(), request);
    }

    /**
     * Perform the specified HTTP request to the target
     *
     * @param method  HTTP method to execute on request
     * @param target  target of the request
     * @param content body of the HTTP request. only processed for POST, PUT and PATCH methods.
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return processed response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    protected Json execute(RestMethod method, WebTarget target, Object content, Json headers, HttpRequest request) throws ServiceException {
        final Response response = request(method, target, content, headers, request);
        return factory.processResponse(response, method, request.isFullResponse());
    }

    /**
     * Perform the specified HTTP request to the target
     *
     * @param method  HTTP method to execute on request
     * @param target  target of the request
     * @param content body of the HTTP request. only processed for POST, PUT and PATCH methods.
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    protected Response request(RestMethod method, WebTarget target, Object content, Json headers, HttpRequest request) throws ServiceException {
        target = target != null ? target : apiTarget;

        final Json headersToSend = Json.map();
        apiHeaders.forEach(headersToSend::set);
        if (headers != null && headers.isMap()) {
            headers.forEachMap(headersToSend::set);
        }

        return factory.request(method, target, content, headersToSend, request);
    }
}
