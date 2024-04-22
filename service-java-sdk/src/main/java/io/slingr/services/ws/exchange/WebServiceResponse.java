package io.slingr.services.ws.exchange;

import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.JsonSource;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Map;

/**
 * Class that represents a web service response on the service
 *
 * <p>Created by lefunes on 11/11/16.
 */
public class WebServiceResponse implements JsonSource {

    private int httpCode;
    private final Object body;
    private final Json headers;

    /**
     * Initialize the response object
     */
    public WebServiceResponse() {
        this(Json.map());
    }

    /**
     * Initialize the response object
     *
     * @param body an object containing the body of the response
     */
    public WebServiceResponse(Object body) {
        this(body, Json.map());
    }

    /**
     * Initialize the response object
     *
     * @param body an object containing the body of the response
     * @param contentType content type of the body
     */
    public WebServiceResponse(Object body, String contentType) {
        this(body, Json.map().setIfNotEmpty(Parameter.CONTENT_TYPE, contentType));
    }

    /**
     * Initialize the response object
     *
     * @param httpCode HTTP code
     * @param body an object containing the body of the response
     */
    public WebServiceResponse(int httpCode, Object body) {
        this(httpCode, body, Json.map());
    }

    /**
     * Initialize the response object
     *
     * @param httpCode HTTP code
     * @param body an object containing the body of the response
     * @param contentType content type of the body
     */
    public WebServiceResponse(int httpCode, Object body, String contentType) {
        this(httpCode, body, Json.map().setIfNotEmpty(Parameter.CONTENT_TYPE, contentType));
    }

    /**
     * Initialize the response object
     *
     * @param body an object containing the body of the response
     * @param headers an {@link Json} containing the values to set on the response header.
     */
    public WebServiceResponse(Object body, Json headers) {
        this(200, body, headers);
    }

    /**
     * Initialize the response object
     *
     * @param httpCode HTTP code
     * @param body an object containing the body of the response
     * @param headers an {@link Json} containing the values to set on the response header.
     */
    public WebServiceResponse(int httpCode, Object body, Json headers) {
        this.httpCode = httpCode;
        this.body = body;
        this.headers = headers != null ? headers : Json.map();
    }

    /**
     * Gets the HTTP code to use to respond to the HTTP client
     *
     * @return HTTP code
     */
    public int getHttpCode() {
        return httpCode;
    }

    /**
     * Sets the HTTP code used to respond to the HTTP client
     *
     * @param httpCode HTTP code
     */
    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode > 0 && httpCode < 700 ? httpCode : 200;
    }

    /**
     * Gets the object containing the body of the response
     *
     * @return object containing the body of the response
     */
    public Object getBody() {
        return body;
    }

    /**
     * Gets the {@link Json} containing the values of the response header.
     *
     * @return a {@link Json} containing the values of the response header.
     */
    public Json getHeaders() {
        return headers;
    }

    /**
     * Sets a header on the response header.
     *
     * @param header header name
     * @param value value to set
     */
    public void setHeader(String header, Object value){
        if(getHeaders() != null && StringUtils.isNotBlank(header)){
            getHeaders().set(header, value);
        }
    }

    /**
     * Gets the value of a header from the response header.
     *
     * @param header header name
     * @return header value
     */
    public Object getHeader(String header){
        if(getHeaders() != null && StringUtils.isNotBlank(header)){
            return getHeaders().object(header);
        }
        return null;
    }

    /**
     * Gets the value of a header from the response header converted to string.
     *
     * @param header header name
     * @return string header value
     */
    public String getStringHeader(String header){
        if(getHeaders() != null && StringUtils.isNotBlank(header)){
            return getHeaders().string(header);
        }
        return null;
    }

    @Override
    public Json toJson() {
        return toJson(false);
    }

    /**
     * Object is transformable to a Json object
     *
     * @param escapeBody true if the body must to be escaped if it is not a known type
     * @return Json object
     */
    public Json toJson(boolean escapeBody) {
        Object b = body;
        if(escapeBody){
            if(b == null){
                b = "null";
            } else if (!(b instanceof JsonSource || b instanceof Collection || b instanceof Map || b instanceof String || b instanceof Number || b instanceof Boolean)) {
                b = String.format("Body[%s]", b.getClass());
            }
        }
        return Json.map()
                .set("code", httpCode)
                .set("body", b)
                .setIfNotNull("headers", headers);
    }

    @Override
    public String toString() {
        return toJson(true).toString();
    }
}