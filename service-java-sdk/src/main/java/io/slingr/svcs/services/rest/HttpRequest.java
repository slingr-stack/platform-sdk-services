package io.slingr.svcs.services.rest;

import io.slingr.svcs.services.exchange.Parameter;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.converters.JsonSource;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class HttpRequest implements JsonSource {

    private RestMethod restMethod;
    private String path = null;
    private final Json params = Json.map();
    private final Json headers = Json.map();
    private final Json settings = Json.map();
    private Object body = null;

    private int connectionTimeout = RestClient.DEFAULT_CONNECTION_TIMEOUT;
    private int readTimeout = RestClient.DEFAULT_READ_TIMEOUT;
    private boolean followRedirects = RestClient.DEFAULT_FOLLOW_REDIRECTS;
    private boolean fullResponse = false;

    private String filename = RestClient.DEFAULT_FILE_NAME;
    private boolean forceDownload = false;
    private boolean downloadSync = false;
    private boolean defaultCallback = false;
    private boolean forceDisableCookies = false;
    private int maxRedirects = RestClient.DEFAULT_MAX_REDIRECTS;
    private Boolean encodeUrl = null;
    private boolean followAuthorizationHeader = false;
    private boolean useSSL = true;


    public HttpRequest(HttpRequestBuilder builder) {
        this.restMethod = builder.restMethod;
        this.path = builder.path;
        this.body = builder.body;
        this.connectionTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
        this.followRedirects = builder.followRedirects;
        this.fullResponse = builder.fullResponse;
        this.filename = builder.filename;
        this.forceDownload = builder.forceDownload;
        this.downloadSync = builder.downloadSync;
        this.defaultCallback = builder.defaultCallback;
        this.forceDisableCookies = builder.forceDisableCookies;
        this.maxRedirects = builder.maxRedirects;
        this.encodeUrl = builder.encodeUrl;
        this.followAuthorizationHeader = builder.followAuthorizationHeader;
        this.useSSL = builder.useSSL;
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

    public enum PartType {
        FILE("file"), OTHER("other");

        private String jsonValue;

        PartType(String jsonValue) {
            this.jsonValue = jsonValue;
        }

        public String getJsonValue() {
            return jsonValue;
        }

        public static PartType fromString(String string) {
            try {
                return valueOf(string);
            } catch (Exception e) {
                return null;
            }
        }

        public static PartType fromJsonValue(String string) {
            for (PartType type : values()) {
                if (type.getJsonValue().equals(string)) {
                    return type;
                }
            }
            return null;
        }
    }

    public static class Part {
        private String name;
        private PartType type;
        private String fileId; // when type is FILE
        private String contentType; // when type is OTHER
        private Object content; // when type is OTHER

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public PartType getType() {
            return type;
        }

        public void setType(PartType type) {
            this.type = type;
        }

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public Object getContent() {
            return content;
        }

        public void setContent(Object content) {
            this.content = content;
        }

        public void fromJson(Json json) {
            name = json.string("name");
            type = PartType.fromJsonValue(json.string("type"));
            if (type == PartType.FILE) {
                fileId = json.string("fileId");
            } else {
                contentType = json.string("contentType");
                content = json.object("content");
            }
        }

        public Object toJson() {
            Json json = Json.map();
            json.set("name", name);
            json.set("type", type.getJsonValue());
            if (type == PartType.FILE) {
                json.set("fileId", fileId);
            } else {
                json.setIfNotEmpty("contentType", json.string("contentType"));
                json.setIfNotEmpty("content", json.string("content"));
            }
            return json;
        }
    }

    private boolean multipart;
    private List<Part> parts = new ArrayList<>();

    /**
     * Build a default HTTP request
     */
    public HttpRequest() {
    }

    /**
     * Build a HTTP request
     *
     * @param path path to access to the HTTP resource
     */
    public HttpRequest(RestMethod restMethod, String path) {
        this.restMethod = restMethod;
        this.path = path;
    }

    /**
     * Build a HTTP request
     *
     * @param path path to access to the HTTP resource
     * @param body body of the HTTP request. only processed for POST, PUT and PATCH methods.
     */
    public HttpRequest(RestMethod restMethod, String path, Object body) {
        this.restMethod = restMethod;
        this.path = path;
        this.body = body;
    }

    public RestMethod getRestMethod() {
        return restMethod;
    }

    public void setRestMethod(RestMethod restMethod) {
        this.restMethod = restMethod;
    }

    /**
     * Gets the path to access to the HTTP resource
     *
     * @return path to access to the HTTP resource
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path to access to the HTTP resource
     *
     * @param path path to access to the HTTP resource
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the params to send to the HTTP service
     *
     * @return params to send to the HTTP service
     */
    public Json getParams() {
        return params;
    }

    /**
     * Gets the headers to send to the HTTP service
     *
     * @return headers to send to the HTTP service
     */
    public Json getHeaders() {
        return headers;
    }

    /**
     * Gets the settings to send to the HTTP service
     *
     * @return settings to send to the HTTP service
     */
    public Json getSettings() {
        return settings;
    }

    /**
     * Gets the content to send to the HTTP service
     *
     * @return body of the HTTP request. only processed for POST, PUT and PATCH methods.
     */
    public Object getBody() {
        return body;
    }

    /**
     * Sets the content to send to the HTTP service
     *
     * @param body body of the HTTP request. only processed for POST, PUT and PATCH methods.
     */
    public void setBody(Object body) {
        this.body = body;
    }

    /**
     * Gets the connection timeout from the HTTP request
     *
     * @return connect timeout interval, in milliseconds. null to use the default value (0: infinity).
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the connection timeout from the HTTP request
     *
     * @param connectionTimeout connect timeout interval, in milliseconds. null to use the default value (0: infinity).
     */
    public void setConnectionTimeout(int connectionTimeout) {
        if(connectionTimeout >= 0) {
            this.connectionTimeout = connectionTimeout;
        } else {
            this.connectionTimeout = RestClient.DEFAULT_CONNECTION_TIMEOUT;
        }
    }

    /**
     * Gets the read timeout from the HTTP request
     *
     * @return read timeout interval, in milliseconds. null to use the default value (0: infinity).
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Set a cap on the maximum number of redirects to follow.
     *
     * @param maxRedirects Set a cap on the maximum number of redirects to follow.
     */
    public void setMaxRedirects(int maxRedirects) {
        if(maxRedirects >= 0) {
            this.maxRedirects = maxRedirects;
        } else {
            this.maxRedirects = RestClient.DEFAULT_MAX_REDIRECTS;
        }
    }

    /**
     * Gets the max redirects
     *
     * @return read the maximum number of redirects to follow.
     */
    public int getMaxRedirects() {
        return maxRedirects;
    }

    /**
     * Sets the read timeout to the HTTP request
     *
     * @param readTimeout read timeout interval, in milliseconds. null to use the default value (0: infinity).
     */
    public void setReadTimeout(int readTimeout) {
        if(readTimeout >= 0) {
            this.readTimeout = readTimeout;
        } else {
            this.readTimeout = RestClient.DEFAULT_READ_TIMEOUT;
        }
    }

    /**
     * Gets the configuration of the client to follow the redirects when an HTTP request is executed
     *
     * @return automatic redirection. A value of {@code true} declares that the client will automatically
     * redirect to the URI declared in 3xx responses.
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * Configure the client to follow the redirects when an HTTP request is executed
     *
     * @param followRedirects automatic redirection. A value of {@code true} declares that the client will automatically
     *                        redirect to the URI declared in 3xx responses.
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Gets true if the response must include extended information about response
     *
     * @return true if the response must include extended information about response
     */
    public boolean isFullResponse() {
        return fullResponse;
    }

    /**
     * Sets true if the response must include extended information about response
     *
     * @param fullResponse true if the response must include extended information about response
     */
    public void setFullResponse(boolean fullResponse) {
        this.fullResponse = fullResponse;
    }

    /**
     * Gets the name to a file to download
     *
     * @return name of the file to download
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Sets the name to a file to download
     *
     * @param filename name of the file to download
     */
    public void setFilename(String filename) {
        this.filename = StringUtils.isNotBlank(filename) ? filename : RestClient.DEFAULT_FILE_NAME;
    }

    /**
     * Gets the value that determines if the HTTP resource will be downloaded
     *
     * @return true to download the resource as a file
     */
    public boolean isForceDownload() {
        return forceDownload;
    }

    /**
     * Sets true if the HTTP resource will be downloaded
     *
     * @param forceDownload true to download the resource as a file
     */
    public void setForceDownload(boolean forceDownload) {
        this.forceDownload = forceDownload;
    }

    /**
     * Gets true if the URL params will be encoded
     *
     * @return true to URL params encode
     */
    public Boolean isEncodeUrl() {
        return encodeUrl;
    }


    /**
     * Sets true if the URL params will be encoded
     *
     * @param encodeUrl true to encode url parameters
     */
    public void setEncodeUrl(Boolean encodeUrl) {
        this.encodeUrl = encodeUrl;
    }

    /**
     * Gets the value that determines if the file must be downloaded synchronously
     *
     * @return true if the file must be downloaded synchronously
     */
    public boolean isDownloadSync() {
        return downloadSync;
    }

    /**
     * Gets the value that determines if contain Callback
     *
     * @return true if contain default callback.
     */
    public boolean isDefaultCallback() {
        return defaultCallback;
    }

    /**
     * Sets if the file must be downloaded synchronously
     *
     * @param downloadSync true if the file must be downloaded synchronously
     */
    public void setDownloadSync(boolean downloadSync) {
        this.downloadSync = downloadSync;
    }

    /**
     * Sets if contain default callback
     *
     * @param defaultCallback true if contain default callback.
     */
    public void setDefaultCallback(boolean defaultCallback) {
        this.defaultCallback = defaultCallback;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public void setMultipart(boolean multipart) {
        this.multipart = multipart;
    }

    /**
     * Gets the value that determines if disable cookies.
     *
     * @return true if you want force cookies over the configuration.
     */
    public boolean isForceDisableCookies() {
        return forceDisableCookies;
    }

    /**
     * Sets if contain can contain cookie in request/response
     *
     * @param forceDisableCookies true if you need discard cookies.
     */
    public void setForceDisableCookies(boolean forceDisableCookies) {
        this.forceDisableCookies = forceDisableCookies;
    }

    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    public boolean isFollowAuthorizationHeader() {
        return followAuthorizationHeader;
    }

    public void setFollowAuthorizationHeader(boolean followAuthorizationHeader) {
        this.followAuthorizationHeader = followAuthorizationHeader;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    @Override
    public Json toJson() {
        final Json json = Json.map()
                .setIfNotEmpty(Parameter.HTTP_REQUEST_PATH, getPath())
                .setIfNotEmpty(Parameter.HTTP_REQUEST_PARAMS, getParams())
                .setIfNotEmpty(Parameter.HTTP_REQUEST_HEADERS, getHeaders())
                .setIfNotNull(Parameter.HTTP_REQUEST_BODY, getBody())
                .setIf(getConnectionTimeout() != RestClient.DEFAULT_CONNECTION_TIMEOUT, Parameter.HTTP_REQUEST_CONNECTION_TIMEOUT, getConnectionTimeout())
                .setIf(getReadTimeout() != RestClient.DEFAULT_READ_TIMEOUT, Parameter.HTTP_REQUEST_READ_TIMEOUT, getReadTimeout())
                .setIf(getMaxRedirects() != RestClient.DEFAULT_MAX_REDIRECTS, Parameter.HTTP_REQUEST_MAX_REDIRECTS, getMaxRedirects())
                .setIf(!isFollowRedirects(), Parameter.HTTP_REQUEST_FOLLOW_REDIRECTS, isFollowRedirects())
                .setIf(isFullResponse(), Parameter.HTTP_REQUEST_FULL_RESPONSE, isFullResponse())
                .setIf(isForceDownload(), Parameter.HTTP_REQUEST_FORCE_DOWNLOAD, isForceDownload())
                .setIf(isDownloadSync(), Parameter.HTTP_REQUEST_DOWNLOAD_SYNC, isDownloadSync())
                .setIf(isForceDisableCookies(), Parameter.HTTP_REQUEST_FORCE_DISABLE_COOKIES, isForceDisableCookies())
                .setIf(isEncodeUrl(), Parameter.HTTP_ENCODE_URL, isEncodeUrl())
                .setIf(isFollowAuthorizationHeader(), Parameter.HTTP_REQUEST_FOLLOW_AUTHORIZATION_HEADER, isFollowAuthorizationHeader())
                .setIf(isUseSSL(), Parameter.HTTP_USE_SSL,isUseSSL())
                .setIf(isMultipart(), "multipart", isMultipart());

        if(!RestClient.DEFAULT_FILE_NAME.equals(getFilename()) && StringUtils.isNotBlank(getFilename())){
            json.set(Parameter.HTTP_REQUEST_FILE_NAME, getFilename());
        }
        if(isMultipart()){
            json.set("parts", Json.list(getParts(), new Json.ListGenerator<Part>() {
                @Override
                public Object element(Part item) {
                    return item.toJson();
                }
            }));
        }
        return json;
    }

    /**
     * Convert a json file to a {@code HttpRequest} object
     *
     * @param json json object to convert
     * @return equivalent http request
     */
    public static HttpRequest fromJson(RestMethod restMethod, Json json){
        final HttpRequest request = new HttpRequest();
        request.restMethod = restMethod;
        if(json != null) {
            final String path = json.string(Parameter.HTTP_REQUEST_PATH);
            if (StringUtils.isNotBlank(path)) {
                request.setPath(path);
            }

            final Json params = json.json(Parameter.HTTP_REQUEST_PARAMS);
            if (params != null && params.isMap()) {
                params.forEachMap((k, o) -> request.getParams().set(k, o));
            }

            final Json headers = json.json(Parameter.HTTP_REQUEST_HEADERS);
            if (headers != null && headers.isMap()) {
                headers.forEachMap((k, o) -> request.getHeaders().set(k, o));
            }

            final Object body = json.object(Parameter.HTTP_REQUEST_BODY);
            if (body != null) {
                request.setBody(body);
            }

            final Json settings = json.json(Parameter.HTTP_REQUEST_SETTINGS);
            if (settings != null) {

                final Integer connectionTimeout = settings.integer(Parameter.HTTP_REQUEST_CONNECTION_TIMEOUT);
                if (connectionTimeout != null) {
                    request.setConnectionTimeout(connectionTimeout);
                }

                final Integer readTimeout = settings.integer(Parameter.HTTP_REQUEST_READ_TIMEOUT);
                if (readTimeout != null) {
                    request.setReadTimeout(readTimeout);
                }

                final Boolean followRedirects = settings.bool(Parameter.HTTP_REQUEST_FOLLOW_REDIRECTS);
                if (followRedirects != null) {
                    request.setFollowRedirects(followRedirects);
                }

                final Integer maxRedirects = settings.integer(Parameter.HTTP_REQUEST_MAX_REDIRECTS);
                if (maxRedirects != null) {
                    request.setMaxRedirects(maxRedirects);
                }

                final Boolean fullResponse = settings.bool(Parameter.HTTP_REQUEST_FULL_RESPONSE);
                if (fullResponse != null) {
                    request.setFullResponse(fullResponse);
                }

                final Boolean forceDownload = settings.bool(Parameter.HTTP_REQUEST_FORCE_DOWNLOAD);
                if (forceDownload != null) {
                    request.setForceDownload(forceDownload);
                }

                final Boolean downloadSync = settings.bool(Parameter.HTTP_REQUEST_DOWNLOAD_SYNC);
                if (downloadSync != null) {
                    request.setDownloadSync(downloadSync);
                }

                final Boolean encodeUrl = settings.bool(Parameter.HTTP_ENCODE_URL);
                if (encodeUrl != null) {
                    request.setEncodeUrl(encodeUrl);
                }

                final Boolean forceDisableCookies = settings.bool(Parameter.HTTP_REQUEST_FORCE_DISABLE_COOKIES);
                if (forceDisableCookies != null) {
                    request.setForceDisableCookies(forceDisableCookies);
                }

                final Boolean defaultCallback = settings.bool(Parameter.HTTP_REQUEST_CALLBACK);
                if (defaultCallback != null) {
                    request.setDefaultCallback(defaultCallback);
                }

                final Boolean  followAuthorizationHeader = settings.bool(Parameter.HTTP_REQUEST_FOLLOW_AUTHORIZATION_HEADER);
                if (followAuthorizationHeader != null) {
                    request.setFollowAuthorizationHeader(followAuthorizationHeader);
                }

                final Boolean  useSSL = settings.bool(Parameter.HTTP_USE_SSL);
                if (useSSL != null) {
                    request.setUseSSL(useSSL);
                }

            }

            final String filename = json.string(Parameter.HTTP_REQUEST_FILE_NAME);
            if (filename != null) {
                request.setFilename(filename);
            }

            Boolean multipart = json.bool("multipart");
            if (multipart != null && multipart) {
                request.setMultipart(true);
                for (Json partJson : json.jsons("parts")) {
                    Part part = new Part();
                    part.fromJson(partJson);
                    request.getParts().add(part);
                }
            } else {
                request.setMultipart(false);
            }
        }
        return request;
    }

    public static class HttpRequestBuilder {

        private RestMethod restMethod;
        private String path = null;
        private Json params = Json.map();
        private Json headers = Json.map();
        private Object body = null;

        private int connectionTimeout = RestClient.DEFAULT_CONNECTION_TIMEOUT;
        private int readTimeout = RestClient.DEFAULT_READ_TIMEOUT;
        private boolean followRedirects = RestClient.DEFAULT_FOLLOW_REDIRECTS;
        private boolean fullResponse = false;

        private String filename = RestClient.DEFAULT_FILE_NAME;
        private boolean forceDownload = false;
        private boolean downloadSync = false;
        private boolean defaultCallback = false;
        private boolean forceDisableCookies = false;
        private int maxRedirects = RestClient.DEFAULT_MAX_REDIRECTS;
        private Boolean encodeUrl = null;
        private boolean followAuthorizationHeader = false;
        private boolean useSSL = true;

        public HttpRequestBuilder() {

        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }


        public HttpRequestBuilder setRestMethod(RestMethod restMethod) {
            this.restMethod = restMethod;
            return this;
        }

        public HttpRequestBuilder setParams(Json params) {
            this.params = params;
            return this;
        }

        public HttpRequestBuilder setHeaders(Json headers) {
            this.headers = headers;
            return this;
        }

        public HttpRequestBuilder setBody(Object body) {
            this.body = body;
            return this;
        }

        public HttpRequestBuilder setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public HttpRequestBuilder setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public HttpRequestBuilder setFollowRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public HttpRequestBuilder setMaxRedirects(int maxRedirects) {
            this.maxRedirects = maxRedirects;
            return this;
        }

        public HttpRequestBuilder setFullResponse(boolean fullResponse) {
            this.fullResponse = fullResponse;
            return this;
        }

        public HttpRequestBuilder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        public HttpRequestBuilder setForceDownload(boolean forceDownload) {
            this.forceDownload = forceDownload;
            return this;
        }

        public HttpRequestBuilder setDownloadSync(boolean downloadSync) {
            this.downloadSync = downloadSync;
            return this;
        }

        public HttpRequestBuilder setDefaultCallback(boolean defaultCallback) {
            this.defaultCallback = defaultCallback;
            return this;
        }

        public HttpRequestBuilder setForceDisableCookies(boolean forceDisableCookies) {
            this.forceDisableCookies = forceDisableCookies;
            return this;
}

        public HttpRequestBuilder setEncodeUrl(boolean encodeUrl) {
            this.encodeUrl = encodeUrl;
            return this;
        }

        public HttpRequestBuilder setFollowAuthorizationHeader(boolean followAuthorizationHeader) {
            this.followAuthorizationHeader = followAuthorizationHeader;
            return this;
        }

        public HttpRequestBuilder setUseSSL(boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }
    }
}