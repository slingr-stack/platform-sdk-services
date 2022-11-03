package io.slingr.svcs.services.rest;

import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import java.io.InputStream;

/**
 * Rest client builder to use over an URI in a unique request
 *
 * <p>Created by lefunes on 09/03/16.
 */
public class RestClientBuilder extends RestClient {
    private static final Logger logger = LoggerFactory.getLogger(RestClientBuilder.class);

    /**
     * Rest client builder to use over an URI in a unique request.
     *
     * @param apiUri base URI to build the requests to the API.
     * @param factory factory to create rest clients.
     */
    RestClientBuilder(String apiUri, RestClientFactory factory) throws SvcException {
        super(apiUri, factory);
    }

    /**
     * Configures a header that will be sent in all requests. For example a token is a typical case where you
     * need this.
     *
     * @param name name of header
     * @param value header value
     */
    public RestClientBuilder header(String name, Object value) {
        super.setupDefaultHeader(name, value);
        return this;
    }

    /**
     * Configures a parameter that will be sent in all requests. For example a token or format value is a typical
     * case where you need this.
     *
     * @param name parameter name
     * @param value parameter value
     */
    public RestClientBuilder parameter(String name, String value) {
        super.setupDefaultParam(name, value);
        return this;
    }

    /**
     * Configures a path to the base target API
     *
     * @param path path to add to api target
     */
    public RestClientBuilder path(String path) {
        super.setPath(path);
        return this;
    }

    /**
     * Set connect timeout interval, in milliseconds. The default value is infinity (0).
     *
     * @param connectionTimeout connection timeout in milliseconds
     */
    public RestClientBuilder connectionTimeout(Integer connectionTimeout) {
        this.setConnectionTimeout(connectionTimeout);
        return this;
    }

    /**
     * Set read timeout interval, in milliseconds. The default value is infinity (0).
     *
     * @param readTimeout read timeout in milliseconds
     */
    public RestClientBuilder readTimeout(Integer readTimeout) {
        this.setReadTimeout(readTimeout);
        return this;
    }

    /**
     * Enable/disable the automatic redirection. The default value is {@code true}.
     *
     * @param followRedirects true if enable automatic redirection.
     */
    public RestClientBuilder followRedirects(Boolean followRedirects) {
        super.setFollowRedirects(followRedirects);
        return this;
    }

    /**
     * Configures the default path for requests with an empty path. By default is '/'
     *
     * @param defaultEmptyPath default for requests with an empty path
     */
    public RestClientBuilder emptyPath(String defaultEmptyPath) {
        this.setDefaultEmptyPath(defaultEmptyPath);
        return this;
    }

    /**
     * True if the client accept external URLs
     *
     * @param allowExternalUrl true if the client accept external URLs
     */
    public RestClientBuilder allowExternalUrl(boolean allowExternalUrl) {
        this.setAllowExternalUrl(allowExternalUrl);
        return this;
    }

    /**
     * Sets the name of the parameter used to upload a file to the REST service
     *
     * @param uploadParameter name of the parameter to use when upload a file to the REST service
     */
    public RestClientBuilder uploadParameter(String uploadParameter) {
        this.setUploadParameter(uploadParameter);
        return this;
    }

    /**
     * Sets the name of the body part used to upload a file to the REST service
     *
     * @param uploadBody name of the body part to use when upload a file to the REST service
     */
    public RestClientBuilder uploadBody(String uploadBody) {
        this.setUploadBody(uploadBody);
        return this;
    }

    /**
     * Disables some options to avoid issues per example when using the websocket over SSL
     */
    public RestClientBuilder disableSslOptions() {
        this.setDisabledSslOptions();
        return this;
    }

    /**
     * Configures basic authentication in the client so calls will use it.
     *
     * @param username the username to authenticate
     * @param password the password of the user
     */
    public RestClientBuilder basicAuthentication(String username, String password) {
        this.setupBasicAuthentication(username, password);
        return this;
    }

    /**
     * Configures digest authentication in the client so calls will use it.
     *
     * @param username the username to authenticate
     * @param password the password of the user
     */
    public RestClientBuilder digestAuthentication(String username, String password) {
        this.setupDigestAuthentication(username, password);
        return this;
    }

    /**
     * Configures basic authentication as a header in the client so calls will use it.
     *
     * @param username the username to authenticate
     * @param password the password of the user
     */
    public RestClientBuilder basicAuthenticationHeader(String username, String password) {
        this.setupBasicAuthenticationHeader(username, password);
        return this;
    }

    /**
     * Configures basic authentication as a header in the client so calls will use it.
     *
     * @param authorizationToken token generated between the user name and password values
     */
    public RestClientBuilder basicAuthenticationHeader(String authorizationToken) {
        this.setupBasicAuthenticationHeader(authorizationToken);
        return this;
    }

    /**
     * Configures bearer authentication as a header in the client so calls will use it.
     *
     * @param username the username to authenticate
     * @param password the password of the user
     */
    public RestClientBuilder bearerAuthenticationHeader(String username, String password) {
        this.setupBearerAuthenticationHeader(username, password);
        return this;
    }

    /**
     * Configures bearer authentication as a header in the client so calls will use it.
     *
     * @param authorizationToken token generated between the user name and password values
     */
    public RestClientBuilder bearerAuthenticationHeader(String authorizationToken) {
        this.setupBearerAuthenticationHeader(authorizationToken);
        return this;
    }

    /**
     * Configures authentication as a header in the client so calls will use it.
     *
     * @param type type of authentication to include in the header, per example {@code Basic}, {@code Bearer}, etc
     * @param username the username to authenticate
     * @param password the password of the user
     */
    public RestClientBuilder authenticationHeader(String type, String username, String password) {
        this.setupAuthenticationHeader(type, username, password);
        return this;
    }

    /**
     * Configures authentication as a header in the client so calls will use it.
     *
     * @param type type of authentication to include in the header, per example {@code Basic}, {@code Bearer}, etc
     * @param authorizationToken token generated between the user name and password values
     */
    public RestClientBuilder authenticationHeader(String type, String authorizationToken) {
        this.setupAuthenticationHeader(type, authorizationToken);
        return this;
    }

    /**
     * Perform a GET request with the default target information.
     *
     * @return processed response of the request
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    @Override
    public Json get() throws SvcException {
        return super.get();
    }

    /**
     * Perform a POST request with the default target information and without content.
     *
     * @return processed response of the request
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    @Override
    public Json post() throws SvcException {
        return super.post();
    }

    /**
     * Perform a POST request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content body of the HTTP request.
     * @return processed response of the request
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    public Json post(Json content) throws SvcException {
        return super.post(content);
    }

    /**
     * Perform a PUT request with the default target information and without content.
     *
     * @return processed response of the request
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    @Override
    public Json put() throws SvcException {
        return super.put();
    }

    /**
     * Perform a PUT request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content body of the HTTP request.
     * @return processed response of the request
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    public Json put(Json content) throws SvcException {
        return super.put(content);
    }

    /**
     * Perform a PATCH request with the default target information and without content.
     *
     * @return processed response of the request
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    @Override
    public Json patch() throws SvcException {
        return super.patch();
    }

    /**
     * Perform a PATCH request with the default target information and content. The content can be a Json, Form, MultiPart or
     * any object that it is possible convert to text.
     *
     * @param content body of the HTTP request.
     * @return processed response of the request
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    public Json patch(Json content) throws SvcException {
        return super.patch(content);
    }

    /**
     * Perform a DELETE request with the default target information and without content
     *
     * @return processed response of the request
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    @Override
    public Json delete() throws SvcException {
        return super.delete();
    }

    /**
     * Perform a OPTIONS request with the default target information
     *
     * @return processed response of the request
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    @Override
    public Json options() throws SvcException {
        return super.options();
    }

    /**
     * Perform a GET request in order to download the remote HTTP resource
     *
     * @return processed downloaded file
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    @Override
    public DownloadedFile download() throws SvcException {
        return super.download();
    }

    /**
     * Perform a POST request to a remote HTTP service in order to upload a file.
     *
     * @param inputStream file part of the HTTP multipart request
     * @param filename filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType MIME type of the {@code streamEntity} attachment.
     * @return processed response of the request
     * @throws SvcException if the request cannot be built or if the server returns an error message
     */
    public Json upload(InputStream inputStream, String filename, String contentType) throws SvcException {
        return super.upload(inputStream, filename, contentType);
    }

    /**
     * Gets the configured api target
     *
     * @return api target
     */
    public WebTarget target(){
        return getApiTarget();
    }
}
