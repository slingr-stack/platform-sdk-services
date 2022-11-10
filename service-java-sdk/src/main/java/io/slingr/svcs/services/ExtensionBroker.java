package io.slingr.svcs.services;

import io.slingr.svcs.configurations.SvcContext;
import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.exceptions.ErrorCode;
import io.slingr.svcs.services.application.AppUser;
import io.slingr.svcs.services.datastores.DataStoreResponse;
import io.slingr.svcs.services.exchange.ApiUri;
import io.slingr.svcs.services.exchange.Parameter;
import io.slingr.svcs.services.logs.AppLogLevel;
import io.slingr.svcs.services.rest.DownloadedFile;
import io.slingr.svcs.services.rest.RestClient;
import io.slingr.svcs.utils.FilesUtils;
import io.slingr.svcs.utils.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the methods defined on the Extension Broker  API
 *
 * <p>Created by lefunes on 14/03/18.
 */
public class ExtensionBroker extends RestClient implements ExtensionBrokerApi {
    private static final Logger logger = LoggerFactory.getLogger(ExtensionBroker.class);

    /**
     * Creates an instance of the Extension Broker API and its services
     *
     * @param uri     base URI to build the requests to the API.
     * @param token   token to exchange information with the Extension Broker app
     * @param version extension broker api version
     * @throws SvcException if something fails when creates the Extension Broker APi instance
     */
    public ExtensionBroker(String uri, String token, String version) throws SvcException {
        super(uri);

        setupDefaultHeader(Parameter.TOKEN, token);
        setupDefaultHeader(Parameter.API_VERSION, version);

        // default rest client values
        setDefaultEmptyPath("");
        setFollowRedirects(true);
        setAllowExternalUrl(false);
        setUploadParameter(Parameter.FILE_UPLOAD_PARAMETER);
        setUploadBody(Parameter.FILE_UPLOAD_BODY);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Events
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void newEvent(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws SvcException {
        // review parameters
        isNotBlank(event, "empty event name");
        if (date == null) {
            date = System.currentTimeMillis();
        }

        final Json restContent = Json.map()
                .set(Parameter.DATE, date)
                .set(Parameter.EVENT_NAME, event)
                .setIfNotNull(Parameter.DATA, data)
                .setIfNotEmpty(Parameter.FROM_FUNCTION_ID, fromFunctionId)
                .setIfNotEmpty(Parameter.USER_ID, userId)
                .setIfNotEmpty(Parameter.USER_EMAIL, userEmail);

        try {
            post(target(ApiUri.ES_URL_ASYNC_EVENT), restContent, buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to send the event [%s]: %s", event, ex.getMessage()), ex);
        }
    }

    @Override
    public Object newSyncEvent(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws SvcException {
        // review parameters
        isNotBlank(event, "empty event name");
        if (date == null) {
            date = System.currentTimeMillis();
        }

        final Json restContent = Json.map()
                .set(Parameter.DATE, date)
                .set(Parameter.EVENT_NAME, event)
                .setIfNotNull(Parameter.DATA, data)
                .setIfNotEmpty(Parameter.FROM_FUNCTION_ID, fromFunctionId)
                .setIfNotEmpty(Parameter.USER_ID, userId)
                .setIfNotEmpty(Parameter.USER_EMAIL, userEmail);
        try {
            final Json jsonResponse = post(target(ApiUri.ES_URL_SYNC_EVENT), restContent, buildHeaders());
            return getSyncObject(jsonResponse);
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to send the sync event [%s]: %s", event, ex.getMessage()), ex);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: App logs
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void newAppLogs(Long date, String level, String message, Json additionalInfo) throws SvcException {
        // review parameters
        isNotBlank(message, "empty app log message");
        if (date == null) {
            date = System.currentTimeMillis();
        }
        level = AppLogLevel.checkStringValue(level);
        if (additionalInfo == null) {
            additionalInfo = Json.map();
        }

        final Json restContent = Json.map()
                .set(Parameter.DATE, date)
                .set(Parameter.APP_LOG_LEVEL, level)
                .set(Parameter.APP_LOG_MESSAGE, message)
                .setIfNotEmpty(Parameter.APP_LOG_ADDITIONAL_INFO, additionalInfo);
        try {
            post(target(ApiUri.ES_URL_APP_LOG), restContent, buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to send an app log: %s", ex.getMessage()), ex);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Distributed locks
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Json acquireLock(String key) throws SvcException {
        // review parameters
        isNotBlank(key, "empty key to lock");

        try {
            return post(target(ApiUri.esLockUrl(key)), null, buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to lock key [%s]: %s", key, ex.getMessage()), ex);
        }
    }

    @Override
    public Json releaseLock(String key) throws SvcException {
        // review parameters
        isNotBlank(key, "empty key to unlock");

        try {
            return delete(target(ApiUri.esLockUrl(key)), buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to lock key [%s]: %s", key, ex.getMessage()), ex);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Files management
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Json uploadFile(String filename, InputStream content, String contentType) throws SvcException {
        // review parameters
        isNotBlank(filename, "empty file name");
        isNotNull(content, "empty file content");

        // save content in a temporal file
        final File tmp = FilesUtils.copyInputStreamToTemporaryFile(filename, content);
        if (tmp == null || !tmp.exists()) {
            logger.warn(String.format("Local copy not created for file [%s]. The file will not be uploaded.", filename));
            throw SvcException.permanent(ErrorCode.CONVERSION, String.format("File [%s] was not uploaded. The file could not be downloaded.", filename));
        }
        final long fileLength = tmp.length();
        if (fileLength < 1) {
            logger.warn(String.format("Empty local copy of file [%s]. The file will not be uploaded.", filename));
            return null;
        }

        // send file to ES
        final FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(tmp);
        } catch (Exception ex) {
            throw SvcException.permanent(ErrorCode.CONVERSION, String.format("File [%s] was not uploaded. The local copy of file was not readable - Exception: %s", filename, ex.getMessage()), ex);
        }

        final MediaType mediaType = FilesUtils.getMediaTypeForMultipart(contentType, filename);
        if (mediaType != null) {
            contentType = mediaType.toString();
        }
        final Json uploadResult = upload(target(ApiUri.ES_URL_FILE_UPLOAD), null, buildHeaders(), false, inputStream, filename, contentType);
        if (uploadResult == null || uploadResult.isEmpty()) {
            throw SvcException.permanent(ErrorCode.CLIENT, String.format("File [%s] was not uploaded. Empty response from extension broker.", filename));
        }
        final String fileId = uploadResult.string(Parameter.FILE_ID);
        if (StringUtils.isBlank(fileId)) {
            throw SvcException.permanent(ErrorCode.CLIENT, String.format("File [%s] was not uploaded. Result message: %s", filename, uploadResult));
        }

        return Json.map()
                .setIfNotEmpty(Parameter.FILE_ID, fileId)
                .setIfNotEmpty(Parameter.FILE_NAME, filename)
                .setIfNotEmpty(Parameter.FILE_CONTENT_TYPE, contentType);
    }

    @Override
    public DownloadedFile downloadFile(String fileId) throws SvcException {
        // review parameters
        isNotBlank(fileId, "empty file id");

        try {
            return download(target(ApiUri.esFileDownloadUrl(fileId)), buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to download file [%s]: %s", fileId, ex.getMessage()), ex);
        }
    }

    @Override
    public Json getFileMetadata(String fileId) throws SvcException {
        // review parameters
        isNotBlank(fileId, "empty file id");

        try {
            return get(target(ApiUri.esFileMetadataUrl(fileId)), buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to get file [%s] metadata: %s", fileId, ex.getMessage()), ex);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Data stores management
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public DataStoreResponse findDocuments(String dataStoreName, Json filter) throws SvcException {
        // review parameters
        isNotBlank(dataStoreName, "empty data store name");
        filter = checkDataStoreFilter(filter);

        DataStoreResponse dsResponse = null;
        try {
            final WebTarget path = target(ApiUri.esDataStoreUrl(dataStoreName), filter);

            final Json response = get(path, buildHeaders());
            if (response != null && response.contains(Parameter.DATA_STORE_ITEMS)) {
                try {
                    dsResponse = new DataStoreResponse(
                            response.jsons(Parameter.DATA_STORE_ITEMS),
                            response.integer(Parameter.DATA_STORE_TOTAL),
                            response.string(Parameter.PAGINATION_OFFSET)
                    );
                } catch (Exception ex) {
                    throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when convert data store response [%s]: %s", dataStoreName, ex.getMessage()), ex);
                }
            }
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (!message.startsWith("Exception when try to find documents on data store")) {
                message = String.format("Exception when try to find documents on data store [%s]: %s", dataStoreName, ex.getMessage());
            }
            throw SvcException.retryable(ErrorCode.CLIENT, message, ex);
        }
        if (dsResponse == null) {
            dsResponse = new DataStoreResponse(null, 0, null);
        }
        return dsResponse;
    }

    @Override
    public Json countDocuments(String dataStoreName, Json filter) throws SvcException {
        // review parameters
        isNotBlank(dataStoreName, "empty data store name");
        filter = checkDataStoreFilter(filter);

        try {
            final WebTarget path = target(ApiUri.esDataStoreCountUrl(dataStoreName), filter);
            return get(path, buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (!message.startsWith("Exception when try to count documents on data store")) {
                message = String.format("Exception when try to count documents on data store [%s]: %s", dataStoreName, ex.getMessage());
            }
            throw SvcException.retryable(ErrorCode.CLIENT, message, ex);
        }
    }

    @Override
    public Json getDocument(String dataStoreName, String documentId) throws SvcException {
        // review parameters
        isNotBlank(dataStoreName, "empty data store name");
        isNotBlank(documentId, "empty document id");

        try {
            return get(target(ApiUri.esDataStoreByIdUrl(dataStoreName, documentId)), buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to find document [%s] on data store [%s]: %s", documentId, dataStoreName, ex.getMessage()), ex);
        }
    }

    @Override
    public Json saveDocument(String dataStoreName, Json document) throws SvcException {
        // review parameters
        isNotBlank(dataStoreName, "empty data store name");
        isNotNull(document, "empty document");

        try {
            return post(target(ApiUri.esDataStoreUrl(dataStoreName)), document, buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to save document on data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
        }
    }

    @Override
    public Json updateDocument(String dataStoreName, String documentId, Json document) throws SvcException {
        // review parameters
        isNotBlank(dataStoreName, "empty data store name");
        isNotBlank(documentId, "empty document id");
        isNotNull(document, "empty document");

        try {
            return put(target(ApiUri.esDataStoreByIdUrl(dataStoreName, documentId)), document, buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to update document [%s] on data store [%s]: %s", documentId, dataStoreName, ex.getMessage()), ex);
        }
    }

    @Override
    public Json removeDocuments(String dataStoreName, Json filter) throws SvcException {
        // review parameters
        isNotBlank(dataStoreName, "empty data store name");
        filter = checkDataStoreFilter(filter);

        try {
            final WebTarget path = target(ApiUri.esDataStoreUrl(dataStoreName), filter);
            return delete(path, buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to remove document on data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
        }
    }

    @Override
    public Json removeDocument(String dataStoreName, String documentId) throws SvcException {
        // review parameters
        isNotBlank(dataStoreName, "empty data store name");
        isNotBlank(documentId, "empty document id");

        try {
            final WebTarget path = target(ApiUri.esDataStoreByIdUrl(dataStoreName, documentId));
            return delete(path, buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to remove document [%s] on data store [%s]: %s", documentId, dataStoreName, ex.getMessage()), ex);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Properties
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Json getConfiguration() throws SvcException {
        try {
            return get(target(ApiUri.ES_URL_CONFIGURATION), buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to request the configuration: %s", ex.getMessage()), ex);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Users
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public AppUser getUserInformationByToken(String token) throws SvcException {
        // review parameters
        isNotBlank(token, "empty token");

        try {
            final Json response = get(target(ApiUri.ES_URL_USERS).queryParam(Parameter.USER_TOKEN, token), buildHeaders());
            final AppUser user = AppUser.fromJson(response);
            if (user != null) {
                return user;
            }
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to find user by token [%s]: %s", token, ex.getMessage()), ex);
        }

        throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("User not found with token [%s]", token));
    }

    @Override
    public AppUser getUserInformationByEmail(String email) throws SvcException {
        // review parameters
        isNotBlank(email, "empty email");

        try {
            final Json response = get(target(ApiUri.ES_URL_USERS).queryParam(Parameter.USER_EMAIL, email), buildHeaders());
            final AppUser user = AppUser.fromJson(response);
            if (user != null) {
                return user;
            }
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to find user by email [%s]: %s", email, ex.getMessage()), ex);
        }

        throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("User not found with email [%s]", email));
    }

    @Override
    public void clearCache() throws SvcException {
        try {
            put(target(ApiUri.ES_URL_CLEAR_CACHE), null, buildHeaders());
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.retryable(ErrorCode.CLIENT, "Exception when trying to clear cache of the app", ex);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Utilities
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Throws an exception if the value is blank
     *
     * @param value   value to check
     * @param message error message if the value is invalid
     * @throws SvcException if the value is blank
     */
    public static void isNotBlank(String value, String message) throws SvcException {
        if (StringUtils.isBlank(value)) {
            logger.error(message);
            throw SvcException.permanent(ErrorCode.ARGUMENT, message);
        }
    }

    /**
     * Throws an exception if the value is null
     *
     * @param value   value to check
     * @param message error message if the value is null
     * @throws SvcException if the value is null
     */
    public static void isNotNull(Object value, String message) throws SvcException {
        if (value == null) {
            logger.error(message);
            throw SvcException.permanent(ErrorCode.ARGUMENT, message);
        }
    }

    /**
     * Checks that the data store filter is not a null object or a json list
     *
     * @param filter value to check
     * @return checked filter
     */
    public static Json checkDataStoreFilter(Json filter) {
        if (filter == null) {
            filter = Json.map();
        } else if (filter.isList()) {
            if (!filter.isEmpty()) {
                logger.warn(String.format("data store filter will be ignored because is a list: %s", filter.toString()));
            }
            filter = Json.map();
        }
        return filter;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // function helper methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Extracts the object wrapped on the sync response received from the Extension Broker .
     *
     * @param jsonResponse json response received from the Extension Broker
     * @return object response
     */
    private Object getSyncObject(Json jsonResponse) {
        Object response = null;
        if (jsonResponse != null) {
            response = jsonResponse;
            if (jsonResponse.isMap()) {
                // unwrap non json responses
                if (jsonResponse.contains(Parameter.SYNC_RESPONSE)) {
                    response = jsonResponse.object(Parameter.SYNC_RESPONSE);
                } else if (jsonResponse.contains(Parameter.SYNC_ERROR_RESPONSE)) {
                    throw SvcException.permanent(ErrorCode.CLIENT, jsonResponse.object(Parameter.SYNC_ERROR_RESPONSE));
                }
            }
        }
        if (response == null) {
            response = Json.map();
        }
        if (response instanceof Map || response instanceof List) {
            response = Json.fromObject(response);
        }
        return response;
    }

    private Json buildHeaders() {
        Json headers = Json.map();
        headers.setIfNotEmpty(Parameter.APP, SvcContext.getCurrentApp());
        headers.setIfNotEmpty(Parameter.ENV, SvcContext.getCurrentEnv());
        return headers;
    }

}
