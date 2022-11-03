package io.slingr.svcs.ws.exchange;

import io.slingr.svcs.services.rest.RestMethod;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.converters.JsonSource;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that represents a web service on the svc
 *
 * <p>Created by lefunes on 11/11/16.
 */
public class WebServiceRequest implements JsonSource {

    private final RestMethod method;
    private final String path;
    private final Object body;
    private final Json headers;
    private final Json parameters;
    private final Json requestInfo;
    private final List<UploadedFile> files;
    private final Json variables = Json.map();
    private String rawBody;

    /**
     * Initialize the request object
     *
     * @param method specifying the name of the method with which this request was made, for example, GET, POST, or PUT.
     * @param path the target of the servletRequest.
     * @param body an object containing the body of the request.
     * @param headers an {@link Json} containing the values of the requested header.
     * @param parameters an {@link Json} containing the values of the query string.
     * @param requestInfo an {@link Json} containing the metadata of the HTTP request.
     * @param files list of files received on the HTTP request.
     */
    public WebServiceRequest(RestMethod method, String path, Object body, Json headers, Json parameters, Json requestInfo, List<UploadedFile> files) {
        this.method = method;
        this.path = path;
        this.body = body;
        if (headers != null) {
            Json lowerCaseHeaders = Json.map();
            headers.forEachMap((key, value) -> {
                lowerCaseHeaders.set(key.toLowerCase(), value);
            });
            this.headers = lowerCaseHeaders;
        } else {
            this.headers = null;
        }
        this.parameters = parameters;
        this.requestInfo = requestInfo;
        this.files = files == null ? new ArrayList<>() : files;
    }

    /**
     * Gets the name of the method with which this request was made, for example, GET, POST, or PUT.
     *
     * @return name of the method with which this request was made, for example, GET, POST, or PUT.
     */
    public RestMethod getMethod() {
        return method;
    }

    /**
     * Gets the target of the servlet request
     *
     * @return the target of the servlet request
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the object containing the body of the request
     *
     * @return object containing the body of the request
     */
    public Object getBody() {
        return body;
    }

    /**
     * Gets the string containing the raw body of the request
     *
     * @return string containing the raw body of the request
     */
    public String getRawBody() {
        return rawBody;
    }

    public void setRawBody(String rawBody) {
        this.rawBody = rawBody;
    }

    /**
     * Gets the object containing the body of the request converted on Json
     *
     * @return json object containing the body of the request
     */
    public Json getJsonBody() {
        final Object bd = getBody();
        if(bd == null){
            return Json.map();
        } else if(bd instanceof Json){
            return (Json) bd;
        }
        return Json.fromObject(bd);
    }

    /**
     * Gets the {@link Json} containing the values of the requested header. This is a copy and
     * changes made there won't affect headers in the request. Use {@link #setHeader(String, String)}
     * to change value of headers.
     *
     * @return a {@link Json} containing the values of the requested header.
     */
    public Json getHeaders() {
        if (headers == null) {
            return null;
        }
        // we return a copy so it cannot be modified
        return headers.cloneJson();
    }

    /**
     * Gets the value of a header from the requested header converted to string.
     *
     * @param header header name
     * @return string header value
     */
    public String getHeader(String header){
        if(headers != null && StringUtils.isNotBlank(header)){
            return headers.string(header.toLowerCase());
        }
        return null;
    }

    /**
     * Gets the value of a header from the requested header.
     *
     * @param header header name
     * @return header value
     */
    public Object getObjectHeader(String header){
        if(headers != null && StringUtils.isNotBlank(header)){
            return headers.object(header.toLowerCase());
        }
        return null;
    }

    /**
     * Sets the value of a header of the request.
     *
     * @param header header name
     * @param value the value of the header
     */
    public void setHeader(String header, String value) {
        if(headers != null && StringUtils.isNotBlank(header)) {
            headers.set(header.toLowerCase(), value);
        }
    }

    /**
     * Gets the {@link Json} containing the values of the query string
     *
     * @return {@link Json} containing the values of the query string
     */
    public Json getParameters() {
        return parameters;
    }

    /**
     * Gets the value of a parameters from the query string
     *
     * @param parameter parameter name
     * @return parameter value
     */
    public String getParameter(String parameter){
        if(parameters != null && StringUtils.isNotBlank(parameter)){
            return parameters.string(parameter);
        }
        return null;
    }

    /**
     * Gets the metadata of the HTTP request.
     *
     * @return metadata of the HTTP request.
     */
    public Json getRequestInfo() {
        return requestInfo;
    }

    /**
     * Gets the list of files received on the HTTP request.
     *
     * @return list of files received on the HTTP request.
     */
    public List<UploadedFile> getFiles() {
        return files;
    }

    /**
     * Gets the {@link Json} containing the values of the path variables
     *
     * @return {@link Json} containing the values of the path variables
     */
    public Json getPathVariables() {
        return variables;
    }

    /**
     * Sets the variable value from extracted from the path
     *
     * @param variable path variable name
     * @param value value to set
     */
    public void setPathVariable(String variable, String value){
        if(StringUtils.isNotBlank(variable)){
            variables.set(variable, value);
        }
    }

    /**
     * Gets the value of a variable extracted from the path
     *
     * @param variable path variable name
     * @return path variable value
     */
    public String getPathVariable(String variable){
        if(StringUtils.isNotBlank(variable)){
            return variables.string(variable);
        }
        return null;
    }

    @Override
    public Json toJson() {
        final Object bd = getBody();
        return Json.map()
                .set("method", method.toString())
                .set("path", getPath())
                .setIfNotNull("body", bd instanceof JsonSource ? ((JsonSource) bd).toJson() : bd != null ? bd.toString() : null)
                .setIfNotEmpty("headers", getHeaders())
                .setIfNotEmpty("parameters", getParameters())
                .setIfNotEmpty("requestInfo", getRequestInfo())
                .setIfNotEmpty("files", getFiles().stream().map(UploadedFile::toJson).collect(Collectors.toList()))
                .setIfNotEmpty("pathVariables", getPathVariables());
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}