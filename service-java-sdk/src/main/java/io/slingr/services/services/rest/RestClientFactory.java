package io.slingr.services.services.rest;

import io.slingr.services.Service;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.services.Files;
import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.services.rest.authentication.AuthenticationService;
import io.slingr.services.utils.FilesUtils;
import io.slingr.services.utils.FormUtils;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.XmlUtils;
import io.slingr.services.utils.converters.ContentTypeFormat;
import io.slingr.services.utils.converters.JsonConverter;
import io.slingr.services.utils.converters.JsonSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.*;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Multipart;
import javax.net.ssl.*;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Factory of the REST clients. This class helps to create clients over different URIs but without properties a different
 * Javax client for each one.
 *
 * <p>Created by lefunes on 14/06/16.
 */
public class RestClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(RestClientFactory.class);

    // FEFF because this is the Unicode char represented by the UTF-8 byte order mark (EF BB BF).
    public static final String UTF8_BOM = "\uFEFF";

    private final Client client;
    protected boolean debug = false;
    private boolean rememberCookies = false;
    private final ReentrantLock cookiesLock = new ReentrantLock();
    private final List<Cookie> cookies = new ArrayList<>();
    private final String[] acceptedMediaTypes;
    private final List<String> history = new ArrayList<>();

    private final AuthenticationService authService = new AuthenticationService();

    private static final String BOUNDARY = "my-boundary";

    /**
     * Initialize factory
     */
    public RestClientFactory(){
        try {
            final ClientConfig clientConfig = new ClientConfig();
            clientConfig.register(MultiPartFeature.class);
            clientConfig.register(JacksonFeature.class);
            clientConfig.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);
            clientConfig.property(ClientProperties.FOLLOW_REDIRECTS, true);

            // the request entity will be buffered in the memory in order to determine content length that will be sent as a Content-Length header in the request
            clientConfig.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED);

            final ConnectorProvider provider = new ApacheConnectorProvider();
            clientConfig.connectorProvider(provider);

            client = ClientBuilder.newClient(clientConfig);

            client.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);

            //By default, SSL certificate usage is enabled.
            enableSSL(true);

            acceptedMediaTypes = ContentTypeFormat.getAcceptedFormats();
        } catch (Exception e) {
            logger.error("Error creating rest client", e);
            throw new RuntimeException("Error creating rest client", e);
        }
    }

    /**
     * Configures TLS/SSL to be used on REST clients
     *
     * @return socket factory for TLS/SSL connections
     */
    private static SSLConnectionSocketFactory configureSSL() throws KeyManagementException, NoSuchAlgorithmException {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
        };
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return new SSLConnectionSocketFactory(sslContext, new TrustAllHostNameVerifier());
    }

    public RestClientFactory enableSSL(boolean useSSL) {
        try {
            final Registry<ConnectionSocketFactory> registry;
            if (useSSL) {
                registry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", configureSSL())
                        .register("http", new PlainConnectionSocketFactory())
                        .build();
            }else{
                registry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", new PlainConnectionSocketFactory())
                        .build();
            }
            final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
            connectionManager.setMaxTotal(100);
            connectionManager.setDefaultMaxPerRoute(100);
            client.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
            client.property(ApacheClientProperties.CONNECTION_MANAGER_SHARED, true);

        } catch (Exception e) {
            logger.error("Error enabling ssl certification", e);
            throw new RuntimeException("Error enabling ssl certification", e);
        }
        return this;
    }

    public WebTarget setupAuthentication(WebTarget apiTarget, HttpRequest request) {
        authService.setupAuthentication(request);
        authService.addAuthentication(client, apiTarget, request);
        return this.client.target(apiTarget.getUri());
    }

    /**
     * Hostname verifier implementation to accept all the hosts
     */
    private static class TrustAllHostNameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /**
     * Set true to enable debug logging
     *
     * @param debug true to enable logging
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * True if the client save and send cookies automatically
     */
    public void setRememberCookies(boolean rememberCookies) {
        this.rememberCookies = rememberCookies;
    }

    public WebTarget uri(String apiUri){
        return client.target(apiUri);
    }

    /**
     * Configures basic authentication in the client so calls will use it.
     *  @param username the username to authenticate
     * @param password the password of the user
     */
    public WebTarget setupBasicAuthentication(WebTarget uri, String username, String password) {
        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(username, password);
        client.register(feature);
        return this.client.target(uri.getUri());
    }

    /**
     * Configures digest authentication in the client so calls will use it.
     *  @param username the username to authenticate
     * @param password the password of the user
     */
    public WebTarget setupDigestAuthentication(WebTarget uri, String username, String password) {
        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.digest(username, password);
        client.register(feature);
        return this.client.target(uri.getUri());
    }

    /**
     * Processes the response to be returned to clients
     *
     * @param response response of HTTP request
     * @param method method used to obtain the response
     * @param fullResponse true if the response must include extended information about response
     * @return Json with the processed response
     * @throws ServiceException if there ir an error when process the response
     */
    Json  processResponse(Response response, RestMethod method, boolean fullResponse) throws ServiceException {
        if (response == null) {
            throw ServiceException.permanent(ErrorCode.CLIENT, "Invalid response");
        }
        try {
            final InputStream responseContent;
            if (method == RestMethod.HEAD) {
                final Object entity = response.getEntity();
                if (entity instanceof InputStream) {
                    responseContent = (InputStream) entity;
                } else if(entity != null){
                    responseContent = new ByteArrayInputStream(response.getEntity().toString().getBytes());
                } else {
                    responseContent = new ByteArrayInputStream("".getBytes());
                }
            } else {
                responseContent = response.readEntity(InputStream.class);
            }
            final byte[] responseAsBytes = IOUtils.toByteArray(responseContent);
            final String responseAsString = IOUtils.toString(responseAsBytes, "UTF-8");

            // try to convert string to json
            final String contentType = response.getHeaderString(Parameter.CONTENT_TYPE);
            Json responseAsJson = JsonConverter.convertString(removeUTF8BOM(responseAsString), contentType, true);

            boolean errorResponse = response.getStatus() < 200 || response.getStatus() > 299;
            if(errorResponse && !fullResponse && responseAsJson != null && (responseAsJson.isMap() && responseAsJson.json("data") != null && responseAsJson.json("data").isMap() && responseAsJson.json("data").bool(Parameter.EXCEPTION_FLAG))){
                // an Service Exception error when process the request
                response.close();

                final Json jsonData = responseAsJson.json("data");
                final Json errorData = jsonData.json("error");
                final ErrorCode errorCode = errorData == null || StringUtils.isBlank(errorData.string("code")) ? ErrorCode.CLIENT : ErrorCode.fromString(errorData.string("code"));

                throw ServiceException.permanent(
                        errorCode != null ? errorCode : ErrorCode.CLIENT,
                        jsonData.string(Parameter.EXCEPTION_MESSAGE),
                        jsonData.json(Parameter.EXCEPTION_ADDITIONAL_INFO)
                );

            } else if(errorResponse || fullResponse) {
                // if is an HTTP error or full request is required
                Object res;
                if(responseAsJson != null){
                    res = responseAsJson;
                } else {
                    boolean asBytes = false;
                    if(StringUtils.isNotBlank(contentType)) {
                        final String ct = contentType.toLowerCase();
                        if(ct.contains("image") || ct.contains("audio") || ct.contains("video") || ct.contains("application") || ct.contains("multipart")) {
                            // return content as bytes
                            asBytes = true;
                        }
                    }

                    if(asBytes){
                        res = responseAsBytes;
                    } else {
                        res = responseAsString;
                    }
                }
                responseAsJson = processFullResponse(response, res);
            }

            response.close();
            if(errorResponse){
                // these might be retryable status codes according to W3: http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html + Cloudflare Connection Timed Out (522)
                if (response.getStatus() == 408 || response.getStatus() == 500 || response.getStatus() == 502 || response.getStatus() == 503 || response.getStatus() == 504 || response.getStatus() == 522) {
                    throw ServiceException.retryable(ErrorCode.API, String.format("%s[%s]", ServiceException.REST_CODE_EXCEPTION, response.getStatus()), responseAsJson).returnCode(response.getStatus());
                } else {
                    throw ServiceException.permanent(ErrorCode.API, String.format("%s[%s]", ServiceException.REST_CODE_EXCEPTION, response.getStatus()), responseAsJson).returnCode(response.getStatus());
                }
            }

            return responseAsJson;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ServiceException.permanent(ErrorCode.CONVERSION, ex.getMessage(), ex);
        }
    }

    private static String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }

    /**
     * Converts the response to wrap a downloaded file
     *
     * @param response original HTTP response
     * @return wrapped downloaded file
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    DownloadedFile processDownloadedFile(Response response) throws ServiceException {
        try {
            if(response != null) {
                final Json headers = Json.map();
                for (String header : response.getHeaders().keySet()) {
                    headers.set(header, response.getHeaders().getFirst(header));
                }

                String contentType = headers.string(Parameter.CONTENT_TYPE);

                // workaround to download files from Fama
                if (StringUtils.isNotBlank(contentType) && contentType.startsWith("data:")) {
                    contentType = contentType.substring(5);
                    headers.set(Parameter.CONTENT_TYPE, contentType);
                    response.getHeaders().putSingle(Parameter.CONTENT_TYPE, contentType);
                }

                final InputStream inputStream = response.readEntity(InputStream.class);
                final int status = response.getStatus();
                if (inputStream != null && status >= 200 && status < 300) {
                    return new DownloadedFile(status, inputStream, headers);
                } else {
                    String message = "Exception when try to download a file";

                    Json json = null;
                    if (inputStream != null) {
                        json = JsonConverter.convertString(IOUtils.toString(inputStream, StandardCharsets.UTF_8), contentType);
                        if (json != null && json.contains("message")) {
                            message = json.string("message");
                        }
                    }
                    throw ServiceException.permanent(ErrorCode.CLIENT, message, json);
                }
            }
            throw ServiceException.permanent(ErrorCode.CLIENT, "Exception when try to download a file");
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ServiceException.permanent(ErrorCode.CONVERSION, ex.getMessage(), ex);
        }
    }

    /**
     * Converts the request to a multipart object
     *
     * @param request the request to convert to multipart
     * @param files the files service to download files from the app
     * @return content to send to REST service
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    Object processMultipart(HttpRequest request, Files files) throws ServiceException {
        if (!request.isMultipart()) {
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "Request is not multipart");
        }
        try {
            final FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
            for (HttpRequest.Part part : request.getParts()) {
                if (part.getType() == HttpRequest.PartType.FILE) {
                    final Json descriptor = files.metadata(part.getFileId());
                    if (descriptor != null && !descriptor.isEmpty()) {
                        final DownloadedFile file = files.download(part.getFileId());
                        final StreamDataBodyPart filePart = new StreamDataBodyPart(part.getName(), file.getFile(), descriptor.string(Parameter.FILE_NAME));
                        final MediaType mediaType = FilesUtils.getMediaTypeForMultipart(descriptor.string(Parameter.FILE_CONTENT_TYPE), descriptor.string(Parameter.FILE_NAME));
                        filePart.setMediaType(mediaType);
                        formDataMultiPart.bodyPart(filePart);
                    } else {
                        throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("File with id [%s] not found", part.getFileId()));
                    }
                } else {
                    Object content = part.getContent();
                    if (content instanceof JsonSource){
                        formDataMultiPart.field(part.getName(), ((JsonSource) content).toJson().toString(), MediaType.APPLICATION_JSON_TYPE);
                    } else {
                        if (!StringUtils.isBlank(part.getContentType())) {
                            formDataMultiPart.field(part.getName(), content.toString(), MediaType.valueOf(part.getContentType()));
                        } else {
                            formDataMultiPart.field(part.getName(), content.toString());
                        }
                    }
                }
            }
            return formDataMultiPart;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ServiceException.permanent(ErrorCode.CONVERSION, ex.getMessage(), ex);
        }
    }

    /**
     * Processes the multipart-related request and prepares the multipart data.
     *
     * @param request the HTTP request containing the parts.
     * @param files   the Files service to handle file metadata and downloads.
     * @return the response as a Json object.
     * @throws ServiceException if a file is not found.
     * @throws IOException      if an I/O error occurs.
     */
    public Json processMultipartRelated(HttpRequest request, Files files) throws ServiceException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (HttpRequest.Part part : request.getParts()) {
            outputStream.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
            if (part.getType() == HttpRequest.PartType.FILE) {
                Json descriptor = files.metadata(part.getFileId());
                if (descriptor != null && !descriptor.isEmpty()) {
                    DownloadedFile file = files.download(part.getFileId());
                    outputStream.write(("Content-Type: " + descriptor.string(Parameter.FILE_CONTENT_TYPE) + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    try (InputStream fileStream = file.getFile()) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fileStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                } else {
                    throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("File with id [%s] not found", part.getFileId()));
                }
            } else {
                String contentType = part.getContentType();
                Object content = part.getContent();
                outputStream.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                if (content instanceof JsonSource) {
                    outputStream.write(((JsonSource) content).toJson().toString().getBytes(StandardCharsets.UTF_8));
                } else {
                    outputStream.write(content.toString().getBytes(StandardCharsets.UTF_8));
                }
                outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
        }

        outputStream.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));

        byte[] multipartData = outputStream.toByteArray();
        return Json.fromObject(uploadFileMultipartRelated(request, multipartData));
    }

    /**
     * Uploads the file data to the specified URL.
     *
     * @param request       the HTTP request containing the URL and headers.
     * @param multipartData the byte array of the multipart data.
     * @return the server response as a String.
     * @throws IOException if an I/O error occurs.
     */
    public static String uploadFileMultipartRelated(HttpRequest request, byte[] multipartData) throws IOException {
        URL url = new URL(request.getPath());
        String accessToken = request.getHeaders().string("Authorization");
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setDoOutput(true);
        httpConn.setRequestMethod("POST");
        httpConn.setRequestProperty("Authorization", accessToken);
        httpConn.setRequestProperty("Content-Type", "multipart/related; boundary=" + BOUNDARY);
        httpConn.setRequestProperty("Content-Length", String.valueOf(multipartData.length));

        try (OutputStream outputStream = httpConn.getOutputStream()) {
            outputStream.write(multipartData);
        }

        int responseCode = httpConn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
            return response.toString();
        } else {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(httpConn.getErrorStream()))) {
                String inputLine;
                while ((inputLine = errorReader.readLine()) != null) {
                    errorResponse.append(inputLine);
                }
            }
            throw new IOException("Error uploading file: " + responseCode + ", " + errorResponse);
        }
    }

    /**
     * Converts the request to wrap the file to upload
     *
     * @param inputStream file part of the HTTP multipart request
     * @param filename filename of the sent attachment (to be set as a part of {@code content-disposition}).
     * @param contentType MIME type of the {@code streamEntity} attachment.
     * @param fileParameter name of the parameter to use when upload a file to the REST service
     * @param contentParameter name of the body part to use when upload a file to the REST service; can be null
     *                         no need to upload content
     * @param content body part of the HTTP multipart request; only will be sent if contentParameter is not null
     * @return content to send to REST service
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    Object processUploadFile(InputStream inputStream, String filename, String contentType, String fileParameter, String contentParameter, Object content) throws ServiceException {
        if (inputStream == null) {
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "Invalid file to upload");
        }
        try {
            final FormDataMultiPart formDataMultiPart = new FormDataMultiPart();

            final String paramName = StringUtils.isNotBlank(fileParameter) ? fileParameter : Parameter.FILE_UPLOAD_PARAMETER;
            final StreamDataBodyPart filePart = new StreamDataBodyPart(paramName, inputStream, filename);

            final MediaType mediaType = FilesUtils.getMediaTypeForMultipart(contentType, filename);
            if(mediaType != null){
                filePart.setMediaType(mediaType);
            }
            formDataMultiPart.bodyPart(filePart);

            // check if we also need to send more information together with the file
            if (content != null) {
                final String bodyName = StringUtils.isNotBlank(contentParameter) ? contentParameter : Parameter.FILE_UPLOAD_BODY;
                if(content instanceof JsonSource){
                    formDataMultiPart.field(bodyName, ((JsonSource) content).toJson().toString(), MediaType.APPLICATION_JSON_TYPE);
                } else {
                    formDataMultiPart.field(bodyName, content.toString());
                }
            }
            return formDataMultiPart;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw ServiceException.permanent(ErrorCode.CONVERSION, ex.getMessage(), ex);
        }
    }

    /**
     * Converts the response to include a complete HTTP response detail
     *
     * @param response original HTTP response
     * @param body original response body
     * @return complete response
     */
    static Json processFullResponse(Response response, Object body) {
        int status = 0;
        final Json headers = Json.map();

        if(response != null){
            status = response.getStatus();

            if(response.getHeaders() != null) {
                response.getHeaders()
                        .forEach((k, objects) -> {
                            if(objects != null && !objects.isEmpty()) {
                                final Object header;
                                if(objects.size() == 1){
                                    header = objects.get(0);
                                } else {
                                    header = Json.fromList(objects);
                                }
                                if(header != null) {
                                    headers.set(k, header);
                                }
                            }
                        });
            }
        }
        return processFullResponse(status, headers, body);
    }

    /**
     * Converts the response to include a complete HTTP response detail
     *
     * @param status HTTP status code
     * @param headers HTTP headers
     * @param body response body
     * @return complete response
     */
    static Json processFullResponse(int status, Json headers, Object body) {
        if(headers == null){
            headers = Json.map();
        }

        if(body instanceof JsonSource){
            // convert json source instances
            body = ((JsonSource) body).toJson();
        }
        if(body instanceof Json && ((Json) body).isMap() && ((Json) body).size() == 1 && ((Json) body).contains("body")){
            // remove additional body level
            body = ((Json) body).object( "body");
        } else if(body instanceof Map && ((Map) body).size() == 1 && ((Map) body).containsKey("body")){
            // remove additional body level
            body = ((Map) body).get("body");
        }
        if(body == null){
            body = Json.map();
        }

        return Json.map()
                .set("status", status)
                .set("headers", headers)
                .set("body", body);
    }

    /**
     * Perform the specified HTTP request to the target
     *
     * @param method HTTP method to execute on request
     * @param target target of the request
     * @param content body of the HTTP request. only processed for POST, PUT and PATCH methods.
     * @param headers headers of HTTP request. the header on target with the same name will be overridden by these
     *                properties
     * @return response of the request
     * @throws ServiceException if the request cannot be built or if the server returns an error message
     */
    protected Response request(RestMethod method, WebTarget target, Object content, Json headers, HttpRequest request) throws ServiceException {
        if (target == null) {
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "Web target is empty.");
        }
        final String uri = target.getUri().toString();

        if (this.debug) {
            logger.info(String.format("%s Preparing request [%s %s]...", Service.DEBUG, method.name(), uri));
        }

        if(method == null){
            // default HTTP method
            method = RestMethod.GET;
        }
        if(headers == null){
            headers = Json.map();
        }

        // prepare content to be sent on request
        Entity<?> postData = null;
        if(method == RestMethod.POST || method == RestMethod.PUT || method == RestMethod.PATCH) {
            if (content == null) {
                content = Json.map();
            }

            String contentType = headers.string(Parameter.CONTENT_TYPE);
            if (content instanceof JsonSource || content instanceof Map || content instanceof List || content instanceof Multipart) {
                content = Json.fromObject(content);

                if (StringUtils.isNotBlank(contentType)) {
                    // there are some cases where we send JSON but content type is different
                    if (ContentTypeFormat.isXmlContentType(contentType)) {
                        final String xml = XmlUtils.jsonToXml((Json) content);
                        postData = Entity.entity(xml, contentType);
                    } else if (ContentTypeFormat.isUrlEncodedFormContentType(contentType)) {
                        Form form = FormUtils.convertFromJsonToForm((Json) content);
                        postData = Entity.form(form);
                    }else if (ContentTypeFormat.isMultipartContentType(contentType)){
                        FormDataMultiPart formDataMultiPart = FormUtils.convertFromJsonToFormDataMultiPart((Json) content);
                        postData = Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE);

                    } else {
                        postData = Entity.entity(content.toString(), contentType);
                    }
                } else {
                    postData = Entity.json(content.toString());
                }
            } else if (content instanceof Form) {
                postData = Entity.form((Form) content);
            } else if (content instanceof MultiPart) {
                MediaType mediaType = MediaType.MULTIPART_FORM_DATA_TYPE;
                mediaType = Boundary.addBoundary(mediaType);
                postData = Entity.entity(content, mediaType);
            } else if (content instanceof String) {

                if (StringUtils.isNotBlank(contentType) && ContentTypeFormat.isXmlContentType(contentType)) {
                    postData = Entity.xml(content);
                } else {
                    postData = Entity.text(content);
                }

            }
        }
        target = target.property(ClientProperties.FOLLOW_REDIRECTS, false);

        // builder of the request created from the target
        final Invocation.Builder invocationBuilder = target.request();
        invocationBuilder.accept(acceptedMediaTypes);

        invocationBuilder.property(ClientProperties.CONNECT_TIMEOUT, request.getConnectionTimeout());

        invocationBuilder.property(ClientProperties.READ_TIMEOUT, request.getReadTimeout());

        // these headers override the previous defined headers on target with the same name
        headers.forEachMap(invocationBuilder::header);

        if(rememberCookies && !request.isForceDisableCookies()){
            // use cookies received on previous requests
            cookiesLock.lock();
            try {
                cookies.forEach(invocationBuilder::cookie);
            } catch (Exception ex){
                if(this.debug){
                    logger.info(String.format("%s Exception when try to process cookies [%s]", Service.DEBUG, ex.getMessage()), ex);
                } else {
                    logger.debug(String.format("Exception when try to process cookies [%s]", ex.getMessage()), ex);
                }
            } finally {
                cookiesLock.unlock();
            }
        }

        Response response;
        try {
            if (this.debug) {
                logger.info(String.format("%s Executing method [%s %s] - Content [%s]", Service.DEBUG, method.name(), uri, postData));
            }

            switch (method) {
                case POST:
                    response = invocationBuilder.post(postData);
                    break;
                case PUT:
                    response = invocationBuilder.put(postData);
                    break;
                case PATCH:
                    response = invocationBuilder.method(RestMethod.PATCH.name(), postData);
                    break;
                case DELETE:
                    response = invocationBuilder.delete();
                    break;
                case HEAD:
                    response = invocationBuilder.head();
                    break;
                case OPTIONS:
                    response = invocationBuilder.options();
                    break;
                default:
                    // GET by default
                    response = invocationBuilder.get();
                    break;
            }

            if (this.debug) {
                logger.info(String.format("%s Response to method [%s %s] - Response [%s]", Service.DEBUG, method.name(), uri, response.getStatus()));
            }

            if (request.getMaxRedirects() > 0 && request.isFollowRedirects() && response.getStatus() >= 300 && response.getStatus() < 400) {
                request.setMaxRedirects(request.getMaxRedirects() - 1);
                String locationHeader = response.getHeaderString("Location");
                if (locationHeader == null || locationHeader.trim().isEmpty()) {
                    logger.info("Exception when trying to process redirect: Location is null or empty.");
                    throw ServiceException.permanent(ErrorCode.GENERAL, "Location is null or empty.");
                } else if (!locationHeader.startsWith("http")) {
                    // handle relative URLs
                    URI baseUri = target.getUri();
                    URI resolvedUri = baseUri.resolve(locationHeader);
                    target = client.target(resolvedUri);
                } else {
                    target = client.target(locationHeader);
                }

                //Remove Authorization if Follow Authorization header is false
                if(!request.isFollowAuthorizationHeader()){
                    headers.remove("Authorization");
            }

                if(!request.isRemoveRefererHeaderOnRedirect()){
                    //Add Referer header
                    this.history.add(this.history.isEmpty() ? request.getPath() : uri);
                    headers.set(HttpHeaders.REFERER, this.history.get(this.history.size() - 1));
                }

                if (!request.isFollowOriginalHttpMethod()) method = RestMethod.GET;

                response = request(method, target, content, headers, request);
                if(!request.isRemoveRefererHeaderOnRedirect()) this.history.remove(this.history.size() - 1);
            }

            if(rememberCookies && !request.isForceDisableCookies()){
                // save cookies for the following requests
                cookiesLock.lock();
                try {
                    response.getCookies()
                            .forEach((s, newCookie) -> cookies.add(newCookie));
                } catch (Exception ex){
                    if(this.debug) {
                        logger.info(String.format("%s Exception when try to process cookies [%s]", Service.DEBUG, ex.getMessage()), ex);
                    } else {
                        logger.debug(String.format("Exception when try to process cookies [%s]", ex.getMessage()), ex);
                    }
                } finally {
                    cookiesLock.unlock();
                }
            }
            // Clear cookies if any mechanism is enabled
            if(!rememberCookies || request.isForceDisableCookies()) cookies.clear();

        } catch (ServiceException ee) {
            throw ee;
        } catch (ResponseProcessingException rpe) {
            throw ServiceException.permanent(ErrorCode.API, String.format("Error processing response [%s]: %s", rpe.getMessage(), rpe.getResponse() != null ? rpe.getResponse() : "-"), rpe).returnCode(500);
        } catch (ProcessingException pe) {
            if(pe.getCause() instanceof IOException){
                throw ServiceException.permanent(ErrorCode.CLIENT, String.format("Error processing request [%s]", ServiceException.getProcessingExceptionMessage(pe)), pe).returnCode(500);
            } else {
                throw ServiceException.retryable(ErrorCode.API, String.format("Error processing request [%s]", pe.getMessage()), pe).returnCode(400);
            }
        } catch (WebApplicationException wae) {
            Response r = wae.getResponse();
            // these might be retryable status codes according to W3: http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html + Cloudflare Connection Timed Out (522)
            if (r != null && (r.getStatus() == 408 || r.getStatus() == 500 || r.getStatus() == 502 || r.getStatus() == 503 || r.getStatus() == 504 || r.getStatus() == 522)) {
                throw ServiceException.retryable(ErrorCode.API, wae.getMessage(), wae).returnCode(r.getStatus());
            } else {
                throw ServiceException.permanent(ErrorCode.API, wae.getMessage(), wae).returnCode(r.getStatus());
            }
        } catch (Exception e) {
            // we assume this is an unhandled exception is a programming error
            throw ServiceException.permanent(ErrorCode.GENERAL, e.getMessage(), e).returnCode(500);
        }
        return response;
    }
}
