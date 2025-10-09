package io.slingr.services.services.exchange;

/**
 * Parameters used to exchange information with the Extension Broker app
 *
 */
public final class Parameter {
    // generics
    public static final String APP = "app";
    public static final String ENV = "env";
    public static final String DATE = "date";
    public static final String API_VERSION = "version";
    public static final String TOKEN = "token";
    public static final String USER_ID = "userId";
    public static final String USER_EMAIL = "userEmail";

    // events
    public static final String EVENT_NAME = "event";
    public static final String DATA = "data";
    public static final String FROM_FUNCTION_ID = "fromFunction";

    // sync responses
    public static final String SYNC_RESPONSE = "__sync_response__";
    public static final String SYNC_ERROR_RESPONSE = "__sync_error_response__";
    
    // app logs
    public static final String APP_LOG_LEVEL = "level";
    public static final String APP_LOG_MESSAGE = "message";
    public static final String APP_LOG_ADDITIONAL_INFO = "additionalInfo";

    // distributed locks
    public static final String LOCK_ACQUIRED = "lockAcquired";
    public static final String LOCK_RELEASED = "lockReleased";

    // files
    public static final String FILE_ID = "fileId";
    public static final String FILE_NAME = "fileName";
    public static final String FILE_CONTENT_TYPE = "contentType";
    public static final String FILE_UPLOAD_PARAMETER = "file";
    public static final String FILE_UPLOAD_BODY = "data";

    // data stores
    public static final String DATA_STORE_ITEMS = "items";
    public static final String DATA_STORE_TOTAL = "total";
    public static final String PAGINATION_OFFSET = "_offset";
    public static final String DATA_STORE_RESULT = "result";
    public static final String PAGINATION_SIZE = "_size";
    public static final String DATA_STORE_TTL = "_ttl";
    public static final String DATA_STORE_ID = "_id";

    // configuration
    public static final String CONFIGURATION_PROXY = "proxy";
    public static final String CONFIGURATION_WEB_SERVICE_URI = "webServiceUri";

    // users
    public static final String USER_TOKEN = "userToken";

    // exceptions
    public static final String EXCEPTION_FLAG = "__service_exception__";
    public static final String EXCEPTION_CODE = "error";
    public static final String EXCEPTION_MESSAGE = "message";
    public static final String EXCEPTION_ADDITIONAL_INFO = "additionalInfo";
    public static final String EXCEPTION_DESCRIPTION = "description";
    public static final String EXCEPTION_RETRYABLE = "retryable";

    // http
    public static final String HOST = "host";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String REFERER = "Referer";

    // functions
    public static final String FUNCTION_ID = "id";
    public static final String FUNCTION_NAME = "function";
    public static final String PARAMS = "params";
    public static final String REQUEST_WRAPPED = "__request_params__";
    public static final String PARAMS_BODY = "body";

    // metadata
    public static final String METADATA_APP = "app";
    public static final String METADATA_NAME = "name";
    public static final String METADATA_ENV = "env";
    public static final String METADATA_PER_USER = "perUser";
    public static final String METADATA_CONFIGURATION = "configuration";
    public static final String METADATA_API_VERSION = "apiVersion";
    public static final String METADATA_HELP_URL = "configurationHelpUrl";
    public static final String METADATA_JS = "js";
    public static final String METADATA_LISTENERS = "listeners";
    public static final String METADATA_DATA_STORES = "dataStores";
    public static final String METADATA_FUNCTIONS = "functions";
    public static final String METADATA_EVENTS = "events";
    public static final String METADATA_CONF = "conf";
    public static final String METADATA_USER_CONF = "userConf";
    public static final String METADATA_USER_CONF_BUTTONS = "userConfButtons";

    // http requests
    public static final String HTTP_REQUEST_PATH = "url";
    public static final String HTTP_REQUEST_PARAMS = "params";
    public static final String HTTP_REQUEST_HEADERS = "headers";
    public static final String HTTP_REQUEST_BODY = "body";
    public static final String HTTP_REQUEST_SETTINGS = "settings";
    public static final String HTTP_REQUEST_CONNECTION_TIMEOUT = "connectionTimeout";
    public static final String HTTP_REQUEST_READ_TIMEOUT = "readTimeout";
    public static final String HTTP_REQUEST_MAX_REDIRECTS = "maxRedirects";
    public static final String HTTP_REQUEST_FOLLOW_REDIRECTS = "followRedirects";
    public static final String HTTP_REQUEST_FULL_RESPONSE = "fullResponse";
    public static final String HTTP_REQUEST_FILE_NAME = "fileName";
    public static final String HTTP_REQUEST_FORCE_DOWNLOAD = "forceDownload";
    public static final String HTTP_REQUEST_DOWNLOAD_SYNC = "downloadSync";
    public static final String HTTP_REQUEST_AUTHORIZATION = "authorization";
    public static final String HTTP_REQUEST_CALLBACK = "callback";
    public static final String HTTP_REQUEST_DEFAULT_CALLBACK = "defaultCallback";
    public static final String HTTP_REQUEST_FORCE_DISABLE_COOKIES = "forceDisableCookies";
    public static final String HTTP_ENCODE_URL = "encodeUrl";
    public static final String HTTP_REQUEST_FOLLOW_AUTHORIZATION_HEADER = "followAuthorizationHeader";
    public static final String HTTP_REQUEST_REMOVE_REFERER_HEADER_ON_REDIRECT = "removeRefererHeaderOnRedirect";
    public static final String HTTP_REQUEST_FOLLOW_ORIGINAL_HTTP_METHOD = "followOriginalHttpMethod";
    public static final String HTTP_USE_SSL = "useSSL";
    public static final String HTTP_USE_MULTI_PART = "multipart";
    public static final String HTTP_FILE_AS_BODY = "bodyAsFile";

    // events
    public static final String EVENT_PROCESSED_JOB = "job";

    // sync responses
    public static final String SYNC_ERROR_RESPONSE_VALUE = "__ERROR_RESPONSE__";
    public static final String SYNC_ID = "syncId";

    // lock
    public static final String LOCK_KEY = "key";

    // data stores
    public static final String DATA_STORE_NAME = "dataStoreName";
    public static final String DATA_STORE_DOCUMENT_ID = "documentId";
}