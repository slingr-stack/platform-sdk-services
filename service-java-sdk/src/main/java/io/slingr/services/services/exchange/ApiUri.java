package io.slingr.services.services.exchange;

/**
 * URLs used in the communication between the Extension Broker and the services.
 *
 */
public final class ApiUri {
    // extension broker urls
    public static final String API_URL_PREFIX = "/api/";
    public static final String URL_FUNCTION = API_URL_PREFIX + "function";
    public static final String URL_CONFIGURATION = API_URL_PREFIX + "configuration";
    public static final String URL_SYSTEM_ALIVE = API_URL_PREFIX + "system/alive";
    public static final String URL_SYSTEM_TERMINATE = API_URL_PREFIX + "system/terminate";

    // service api urls (the /api is added by the ExtensionBrokerClient)

    public static final String EB_URL_SERVICES_PREFIX = "/services/";
    public static final String EB_URL_MANAGEMENT_PREFIX = "management/";
    public static final String EB_PART_LOCK = "locks";
    public static final String EB_PART_FILE = "files";
    public static final String EB_PART_DATA_STORE = "dataStores";
    public static final String EB_PART_METADATA = "metadata";
    public static final String EB_PART_COUNT = "count";
    public static final String EB_URL_ASYNC_EVENT = EB_URL_SERVICES_PREFIX + "events";
    public static final String EB_URL_SYNC_EVENT = EB_URL_SERVICES_PREFIX + "events/sync";
    public static final String EB_URL_APP_LOG = EB_URL_SERVICES_PREFIX + "logs";
    private static final String EB_URL_LOCK = EB_URL_SERVICES_PREFIX + EB_PART_LOCK + "/%s";
    public static final String EB_URL_FILE_UPLOAD = EB_URL_SERVICES_PREFIX + EB_PART_FILE;
    private static final String EB_URL_FILE_DOWNLOAD = EB_URL_SERVICES_PREFIX + EB_PART_FILE + "/%s";
    private static final String EB_URL_FILE_METADATA = EB_URL_SERVICES_PREFIX + EB_PART_FILE + "/%s/" + EB_PART_METADATA;
    private static final String EB_URL_DATA_STORE = EB_URL_SERVICES_PREFIX + EB_PART_DATA_STORE + "/%s";
    private static final String EB_URL_DATA_STORE_BY_ID = EB_URL_SERVICES_PREFIX + EB_PART_DATA_STORE + "/%s/%s";
    private static final String EB_URL_DATA_STORE_COUNT = EB_URL_SERVICES_PREFIX + EB_PART_DATA_STORE + "/%s/" + EB_PART_COUNT;
    public static final String EB_URL_CONFIGURATION = EB_URL_SERVICES_PREFIX + "configuration";
    public static final String EB_URL_USERS = EB_URL_SERVICES_PREFIX + "users";
    public static final String EB_URL_CLEAR_CACHE = EB_URL_SERVICES_PREFIX + EB_URL_MANAGEMENT_PREFIX + "clearCache";

    /**
     * Gets the Extension Broker URL for locks
     *
     * @param key key to lock
     * @return extension broker uri
     */
    public static String esLockUrl(String key) {
        return String.format(EB_URL_LOCK, key);
    }

    /**
     * Gets the Extension Broker URL for file downloads
     *
     * @param fileId id of tte file to download
     * @return extension broker uri
     */
    public static String esFileDownloadUrl(String fileId) {
        return String.format(EB_URL_FILE_DOWNLOAD, fileId);
    }

    /**
     * Gets the Extension Broker URL for file metadata
     *
     * @param fileId id of tte file to get metadata
     * @return extension broker uri
     */
    public static String esFileMetadataUrl(String fileId) {
        return String.format(EB_URL_FILE_METADATA, fileId);
    }

    /**
     * Gets the Extension Broker URL for data store finds
     *
     * @param dataStoreName data store name
     * @return extension broker uri
     */
    public static String esDataStoreUrl(String dataStoreName) {
        return String.format(EB_URL_DATA_STORE, dataStoreName);
    }

    /**
     * Gets the Extension Broker URL for data store finds by id
     *
     * @param dataStoreName data store name
     * @param documentId    id of the document to find
     * @return extension broker uri
     */
    public static String esDataStoreByIdUrl(String dataStoreName, String documentId) {
        return String.format(EB_URL_DATA_STORE_BY_ID, dataStoreName, documentId);
    }

    /**
     * Gets the Extension Broker URL for data store counts
     *
     * @param dataStoreName data store name
     * @return extension broker uri
     */
    public static String esDataStoreCountUrl(String dataStoreName) {
        return String.format(EB_URL_DATA_STORE_COUNT, dataStoreName);
    }
}