package io.slingr.services.utils.tests;

import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.services.ExtensionBroker;
import io.slingr.services.services.ExtensionBrokerApi;
import io.slingr.services.services.application.AppUser;
import io.slingr.services.services.datastores.DataStoreResponse;
import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.services.logs.AppLogLevel;
import io.slingr.services.services.rest.DownloadedFile;
import io.slingr.services.utils.FilesUtils;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.Strings;
import io.slingr.services.utils.converters.JsonSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the methods defined on the Extension Broker API to be used on testing time
 *
 */
public class ExtensionBrokerMock implements ExtensionBrokerApi {
    private static final Logger logger = LoggerFactory.getLogger(ExtensionBrokerMock.class);

    // events
    private final ReentrantLock eventsLock = new ReentrantLock();
    private final List<Json> receivedEvents = new ArrayList<>();
    private final Map<String, MessageProcessor> eventProcessors = new HashMap<>();

    // locks
    private final ReentrantLock locksLock = new ReentrantLock();
    private final Map<String, Boolean> locks = new HashMap<>();

    // files
    private final ReentrantLock filesLock = new ReentrantLock();
    private final Map<String, FileMock> files = new HashMap<>();

    // data stores
    private final ReentrantLock dataStoresLock = new ReentrantLock();
    private final Map<String, List<Json>> dataStores = new HashMap<>();

    // users
    private final ReentrantLock usersLock = new ReentrantLock();
    private final Map<String, AppUser> users = new HashMap<>();

    /**
     * Creates an instance of the Extension Broker API and its services that is used on tests
     *
     * @throws ServiceException if something fails when creates the Extension Broker APi instance
     */
    public ExtensionBrokerMock() throws ServiceException {}

    /************************
     EB API: Events
     ************************/

    @Override
    public void newEvent(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws ServiceException {
        processEvent("event", date, event, data, fromFunctionId, userId, userEmail);
        logger.info(String.format("%s --------------", ServiceTests.TEST));
    }

    @Override
    public Object newSyncEvent(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws ServiceException {
        final Object response = processEvent("sync event", date, event, data, fromFunctionId, userId, userEmail);
        logger.info(String.format("%s EB: sync events response: %s", ServiceTests.TEST, response));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return response;
    }

    /**
     * Checks and processes the received event
     */
    private Object processEvent(String type, Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(event, "empty event name");
        if(date == null){
            date = System.currentTimeMillis();
        }

        final Json eventContent = Json.map()
                .set(Parameter.DATE, date)
                .set(Parameter.EVENT_NAME, event)
                .setIfNotNull(Parameter.DATA, data)
                .setIfNotEmpty(Parameter.FROM_FUNCTION_ID, fromFunctionId)
                .setIfNotEmpty(Parameter.USER_ID, userId)
                .setIfNotEmpty(Parameter.USER_EMAIL, userEmail);

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: new %s: %s", ServiceTests.TEST, type, eventContent));

        Object response = null;
        eventsLock.lock();
        try {
            receivedEvents.add(eventContent);

            if(eventProcessors.containsKey(event)){
                logger.info(String.format("%s EB: event processor found for [%s]", ServiceTests.TEST, event));
                response = eventProcessors.get(event).processMessage(eventContent);
            } else {
                logger.info(String.format("%s EB: default event response for [%s]", ServiceTests.TEST, event));
            }
        } finally {
            eventsLock.unlock();
        }
        if(response == null){
            response = Json.map();
        }
        return response;
    }

    /**
     * Clears the list of received events
     */
    public void clearReceivedEvents(){
        eventsLock.lock();
        try {
            receivedEvents.clear();
        } finally {
            eventsLock.unlock();
        }
    }

    /**
     * Gets the list of received events
     *
     * @return list of received events
     */
    public List<Json> getReceivedEvents(){
        final List<Json> events = new ArrayList<>();
        eventsLock.lock();
        try {
            events.addAll(receivedEvents);
        } finally {
            eventsLock.unlock();
        }
        return events;
    }

    /**
     * Registers a processor for the event name
     */
    public void registerEventProcessor(String event, MessageProcessor processor){
        if(StringUtils.isEmpty(event)){
            throw new IllegalArgumentException("Invalid event name");
        }
        if(processor == null){
            throw new IllegalArgumentException("Invalid processor");
        }

        eventsLock.lock();
        try {
            eventProcessors.put(event, processor);
        } finally {
            eventsLock.unlock();
        }
    }

    /**
     * Removes the event processor for the given event name
     */
    public void removeEventProcessor(String event){
        if(StringUtils.isEmpty(event)){
            throw new IllegalArgumentException("Invalid event name");
        }

        eventsLock.lock();
        try {
            eventProcessors.remove(event);
        } finally {
            eventsLock.unlock();
        }
    }

    /**
     * Clears the event processors
     */
    public void clearEventProcessors(){
        eventsLock.lock();
        try {
            eventProcessors.clear();
        } finally {
            eventsLock.unlock();
        }
    }

    /************************
     EB API: App logs
     ************************/

    @Override
    public void newAppLogs(Long date, String level, String message, Json additionalInfo) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(message, "empty app log message");
        if(date == null){
            date = System.currentTimeMillis();
        }
        level = AppLogLevel.checkStringValue(level);
        if(additionalInfo == null){
            additionalInfo = Json.map();
        }

        final Json restContent = Json.map()
                .set(Parameter.DATE, date)
                .set(Parameter.APP_LOG_LEVEL, level)
                .set(Parameter.APP_LOG_MESSAGE, message)
                .setIfNotEmpty(Parameter.APP_LOG_ADDITIONAL_INFO, additionalInfo);

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: app log: %s", ServiceTests.TEST, restContent));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
    }


    /************************
     EB API: Distributed locks
     ************************/

    @Override
    public Json acquireLock(String key) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(key, "empty key to lock");

        boolean acquired = false;
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: acquire lock: %s", ServiceTests.TEST, key));
        locksLock.lock();
        try {
            if(!locks.containsKey(key) || !Boolean.TRUE.equals(locks.get(key))){
                acquired = true;
                locks.put(key, true);
            }
        } finally {
            locksLock.unlock();
        }

        final Json jsonResponse = Json.map()
                .set(Parameter.LOCK_ACQUIRED, acquired);
        logger.info(String.format("%s EB: lock: %s", ServiceTests.TEST, jsonResponse));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return jsonResponse;
    }

    @Override
    public Json releaseLock(String key) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(key, "empty key to unlock");

        boolean released = false;
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: release lock: %s", ServiceTests.TEST, key));
        locksLock.lock();
        try {
            if(locks.containsKey(key) && Boolean.TRUE.equals(locks.get(key))){
                released = true;
                locks.put(key, false);
            }
        } finally {
            locksLock.unlock();
        }

        final Json jsonResponse = Json.map()
                .set(Parameter.LOCK_RELEASED, released);
        logger.info(String.format("%s EB: unlock: %s", ServiceTests.TEST, jsonResponse));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return jsonResponse;
    }

    /**
     * Clears the list of locks
     */
    public void clearLocks(){
        locksLock.lock();
        try {
            locks.clear();
        } finally {
            locksLock.unlock();
        }
    }

    /**
     * Gets the list of locks
     *
     * @return list of locks
     */
    public List<String> getLocks(){
        final List<String> locks = new ArrayList<>();
        locksLock.lock();
        try {
            locks.addAll(this.locks.entrySet().stream()
                    .filter(lock -> Boolean.TRUE.equals(lock.getValue()))
                    .map(Map.Entry::getKey)
                    .toList()
            );
        } finally {
            locksLock.unlock();
        }
        return locks;
    }


    /************************
     EB API: Files management
     ************************/

    @Override
    public Json uploadFile(String filename, InputStream content, String contentType) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(filename, "empty file name");
        ExtensionBroker.isNotNull(content, "empty file content");

        // save content in a temporal file
        final String fileContent = Strings.readAsString(content);
        if(fileContent == null){
            logger.warn(String.format("Local copy not created for file [%s]. The file will not be uploaded.", filename));
            throw ServiceException.permanent(ErrorCode.CONVERSION, String.format("File [%s] was not uploaded. The file could not be downloaded.", filename));
        }
        final long fileLength = fileContent.length();
        if(fileLength < 1){
            logger.warn(String.format("Empty local copy of file [%s]. The file will not be uploaded.", filename));
            return null;
        }

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: upload file [%s], length [%s], content type [%s]", ServiceTests.TEST, filename, fileLength, contentType));

        // send file to EB
        final MediaType mediaType = FilesUtils.getMediaTypeForMultipart(contentType, filename);
        if(mediaType != null){
            contentType = mediaType.toString();
        }

        final String fileId = Strings.randomUUIDString();
        final FileMock fileMock = new FileMock(fileContent, fileId, filename, contentType, fileLength);

        filesLock.lock();
        try {
            files.put(fileId, fileMock);
        } finally {
            filesLock.unlock();
        }

        final Json response = fileMock.getFileDescriptor();
        logger.info(String.format("%s EB: uploaded file: %s", ServiceTests.TEST, response));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return response;
    }

    @Override
    public DownloadedFile downloadFile(String fileId) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(fileId, "empty file id");

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: download file [%s]", ServiceTests.TEST, fileId));

        filesLock.lock();
        try {
            if(files.containsKey(fileId)){
                final FileMock file = files.get(fileId);
                final InputStream is = Strings.readAsInputStream(file.fileContent);

                final DownloadedFile response = new DownloadedFile(200, is, file.getHeaders());
                logger.info(String.format("%s EB: downloaded file: %s", ServiceTests.TEST, response));
                logger.info(String.format("%s --------------", ServiceTests.TEST));
                return response;
            }
        } finally {
            filesLock.unlock();
        }
        throw ServiceException.permanent(ErrorCode.CLIENT, String.format("%s File does not exists on application [%s]", ServiceTests.TEST, fileId));
    }

    @Override
    public Json getFileMetadata(String fileId) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(fileId, "empty file id");

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: get file metadata [%s]", ServiceTests.TEST, fileId));

        Json response = null;
        filesLock.lock();
        try {
            if(files.containsKey(fileId)){
                response = files.get(fileId).getMetadata();
            }
        } finally {
            filesLock.unlock();
        }
        logger.info(String.format("%s EB: file metadata: %s", ServiceTests.TEST, response));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return response;
    }

    /**
     * Add a file to be used on tests
     */
    public void addFile(String fileId, FileMock file){
        if(StringUtils.isEmpty(fileId)){
            throw new IllegalArgumentException("Invalid file id");
        }
        if(file == null){
            throw new IllegalArgumentException("Invalid file");
        }

        filesLock.lock();
        try {
            files.put(fileId, file);
        } finally {
            filesLock.unlock();
        }
    }

    /**
     * Clears the list of files
     */
    public void clearFiles(){
        filesLock.lock();
        try {
            files.clear();
        } finally {
            filesLock.unlock();
        }
    }

    /**
     * Gets the list of files
     *
     * @return list of files
     */
    public List<FileMock> getFiles(){
        final List<FileMock> response = new ArrayList<>();
        filesLock.lock();
        try {
            response.addAll(files.values());
        } finally {
            filesLock.unlock();
        }
        return response;
    }

    /**
     * Represents a file on the application
     */
    public static class FileMock implements JsonSource {

        private String fileContent;
        private String fileId;
        private String fileName;
        private String contentType;
        private Long length;
        private final Long uploadDate;
        private final Long expirationDate;
        private final Boolean expired;

        public FileMock() {
            uploadDate = System.currentTimeMillis();
            expirationDate = System.currentTimeMillis();
            expired = false;
        }

        public FileMock(String fileContent, String fileId, String fileName, String contentType, Long length) {
            this();
            this.fileContent = fileContent;
            this.fileId = fileId;
            this.fileName = fileName;
            this.contentType = contentType;
            this.length = length;
        }

        public Json getMetadata() {
            return Json.map()
                    .setIfNotNull("fileName", fileName)
                    .setIfNotNull("contentType", contentType)
                    .setIfNotNull("length", length)
                    .setIfNotNull("uploadDate", uploadDate)
                    .setIfNotNull("expirationDate", expirationDate)
                    .setIfNotNull("expired", expired);
        }

        public Json getHeaders() {
            return Json.map()
                    .setIfNotNull(Parameter.CONTENT_TYPE, contentType)
                    .setIfNotNull(Parameter.CONTENT_LENGTH, length);
        }

        public Json getFileDescriptor() {
            return Json.map()
                    .setIfNotEmpty(Parameter.FILE_ID, fileId)
                    .setIfNotEmpty(Parameter.FILE_NAME, fileName)
                    .setIfNotEmpty(Parameter.FILE_CONTENT_TYPE, contentType);
        }

        @Override
        public Json toJson() {
            return Json.map()
                    .setIfNotNull("fileId", fileId)
                    .setIfNotNull("fileName", fileName)
                    .setIfNotNull("fileContent", fileContent)
                    .setIfNotNull("contentType", contentType)
                    .setIfNotNull("length", length)
                    .setIfNotNull("uploadDate", uploadDate)
                    .setIfNotNull("expirationDate", expirationDate)
                    .setIfNotNull("expired", expired);
        }
    }


    /************************
     EB API: Data stores management
     ************************/

    @Override
    public DataStoreResponse findDocuments(String dataStoreName, Json filter) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        filter = ExtensionBroker.checkDataStoreFilter(filter);

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: find documents [%s], filter [%s]", ServiceTests.TEST, dataStoreName, filter));

        DataStoreResponse response;
        dataStoresLock.lock();
        try {
            response = getDataStoreItems(dataStoreName, filter);
        } finally {
            dataStoresLock.unlock();
        }
        logger.info(String.format("%s EB: found documents: %s", ServiceTests.TEST, response));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return response;
    }

    @Override
    public Json countDocuments(String dataStoreName, Json filter) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        filter = ExtensionBroker.checkDataStoreFilter(filter);

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: count documents [%s], filter [%s]", ServiceTests.TEST, dataStoreName, filter));

        Json response;
        dataStoresLock.lock();
        try {
            final DataStoreResponse items = getDataStoreItems(dataStoreName, filter);
            response = Json.map()
                    .set(Parameter.DATA_STORE_TOTAL, items.getTotal());
        } finally {
            dataStoresLock.unlock();
        }
        logger.info(String.format("%s EB: found documents count: %s", ServiceTests.TEST, response));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return response;
    }

    @Override
    public Json getDocument(String dataStoreName, String documentId) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        ExtensionBroker.isNotBlank(documentId, "empty document id");

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: find document [%s], document id [%s]", ServiceTests.TEST, dataStoreName, documentId));

        Json response = null;
        dataStoresLock.lock();
        try {
            final List<Json> ds = getDataStore(dataStoreName);
            for (Json r : ds) {
                if(documentId.equals(r.string(Parameter.DATA_STORE_ID))){
                    response = r;
                }
            }
        } finally {
            dataStoresLock.unlock();
        }
        logger.info(String.format("%s EB: found document: %s", ServiceTests.TEST, response));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return response;
    }

    @Override
    public Json saveDocument(String dataStoreName, Json document) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        ExtensionBroker.isNotNull(document, "empty document");

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: save document [%s], document [%s]", ServiceTests.TEST, dataStoreName, document));

        String documentId = document.string(Parameter.DATA_STORE_ID);
        if(StringUtils.isBlank(documentId)){
            documentId = Strings.randomUUIDString();
        }

        saveDocument(dataStoreName, documentId, document);

        logger.info(String.format("%s EB: saved document: %s", ServiceTests.TEST, document));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return document;
    }

    @Override
    public Json updateDocument(String dataStoreName, String documentId, Json document) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        ExtensionBroker.isNotBlank(documentId, "empty document id");
        ExtensionBroker.isNotNull(document, "empty document");

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: update document [%s], document id [%s], document [%s]", ServiceTests.TEST, dataStoreName, documentId, document));

        saveDocument(dataStoreName, documentId, document);

        logger.info(String.format("%s EB: updated document: %s", ServiceTests.TEST, document));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return document;
    }

    @Override
    public Json removeDocuments(String dataStoreName, Json filter) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        filter = ExtensionBroker.checkDataStoreFilter(filter);

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: remove documents [%s], filter [%s]", ServiceTests.TEST, dataStoreName, filter));

        final Json response = Json.map()
                .set(Parameter.DATA_STORE_RESULT, false)
                .set(Parameter.DATA_STORE_TOTAL, 0);

        filter.remove(Parameter.PAGINATION_SIZE);
        filter.remove(Parameter.PAGINATION_OFFSET);

        dataStoresLock.lock();
        try {
            final List<Json> items = new ArrayList<>();
            int removed = 0;
            final List<Json> ds = getDataStore(dataStoreName);

            if(filter.isEmpty()){
                removed = ds.size();
            } else {
                for (Json r : ds) {
                    boolean remove = true;
                    for (String k : filter.keys()) {
                        if(!filter.string(k).equals(r.string(k))){
                            remove = false;
                            break;
                        }
                    }

                    if(!remove){
                        items.add(r);
                    } else {
                        removed++;
                    }
                }
            }

            dataStores.put(dataStoreName, items);
            response.set(Parameter.DATA_STORE_RESULT, removed > 0);
            response.set(Parameter.DATA_STORE_TOTAL, removed);

        } finally {
            dataStoresLock.unlock();
        }

        logger.info(String.format("%s EB: removed documents: %s", ServiceTests.TEST, response));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return response;
    }

    @Override
    public Json removeDocument(String dataStoreName, String documentId) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        ExtensionBroker.isNotBlank(documentId, "empty document id");

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: remove document [%s], document id [%s]", ServiceTests.TEST, dataStoreName, documentId));

        final Json response = Json.map()
                .set(Parameter.DATA_STORE_RESULT, false)
                .set(Parameter.DATA_STORE_TOTAL, 0);

        dataStoresLock.lock();
        try {
            final List<Json> items = new ArrayList<>();
            int removed = 0;

            final List<Json> ds = getDataStore(dataStoreName);
            for (Json r : ds) {
                if(!documentId.equals(r.string(Parameter.DATA_STORE_ID))){
                    items.add(r);
                } else {
                    removed++;
                }
            }

            dataStores.put(dataStoreName, items);
            response.set(Parameter.DATA_STORE_RESULT, removed > 0);
            response.set(Parameter.DATA_STORE_TOTAL, removed);

        } finally {
            dataStoresLock.unlock();
        }

        logger.info(String.format("%s EB: removed document: %s", ServiceTests.TEST, response));
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        return response;
    }


    private List<Json> getDataStore(String dataStoreName){
        List<Json> ds = dataStores.get(dataStoreName);
        if(ds == null){
            ds = new ArrayList<>();
        }
        dataStores.put(dataStoreName, ds);
        return ds;
    }

    private DataStoreResponse getDataStoreItems(String dataStoreName, Json filter) {
        final List<Json> ds = getDataStore(dataStoreName);

        final List<Json> items = new ArrayList<>();
        int total = 0;
        String newOffset = null;
        if(!ds.isEmpty()){
            Integer size = filter.integer(Parameter.PAGINATION_SIZE);
            if(size == null){
                size = ds.size()+1;
            }
            filter.remove(Parameter.PAGINATION_SIZE);

            final String offset = filter.string(Parameter.PAGINATION_OFFSET);
            filter.remove(Parameter.PAGINATION_OFFSET);

            boolean checkOffset = StringUtils.isNotBlank(offset);
            for (Json r : ds) {
                boolean include = true;
                if(!filter.isEmpty()){
                    for (String k : filter.keys()) {
                        if(!filter.string(k).equals(r.string(k))){
                            include = false;
                            break;
                        }
                    }
                }
                if(include){
                    total++;
                }

                if(size < 0){
                    include = false;
                } else {
                    if (checkOffset) {
                        if (offset.compareTo(r.string(Parameter.DATA_STORE_ID)) >= 0) {
                            include = false;
                        } else {
                            checkOffset = false;
                        }
                    }
                }

                if(include){
                    items.add(r);
                    size--;
                    newOffset = r.string(Parameter.DATA_STORE_ID);
                }
            }
        }
        return new DataStoreResponse(
                items,
                total,
                newOffset
        );
    }

    private void saveDocument(String dataStoreName, String documentId, Json document) {
        document.set(Parameter.DATA_STORE_ID, documentId);
        dataStoresLock.lock();
        try {
            final List<Json> newDs = new ArrayList<>();
            final List<Json> ds = getDataStore(dataStoreName);
            boolean saved = false;
            for (Json r : ds) {
                if(documentId.equals(r.string(Parameter.DATA_STORE_ID))){
                    newDs.add(document);
                    saved = true;
                } else {
                    newDs.add(r);
                }
            }
            if(!saved){
                newDs.add(document);
            }
            dataStores.put(dataStoreName, newDs);
        } finally {
            dataStoresLock.unlock();
        }
    }

    /**
     * Clears the list of data stores
     */
    public void clearDataStores(){
        dataStoresLock.lock();
        try {
            dataStores.clear();
        } finally {
            dataStoresLock.unlock();
        }
    }

    /**
     * Clears the data store
     *
     * @param dataStoreName data store name
     */
    public void clearDataStore(String dataStoreName){
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");

        dataStoresLock.lock();
        try {
            dataStores.put(dataStoreName, new ArrayList<>());
        } finally {
            dataStoresLock.unlock();
        }
    }

    /**
     * Gets the list of data store items
     *
     * @param dataStoreName data store name
     * @return list of data stores items
     */
    public List<Json> getDataStoreItems(String dataStoreName){
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");

        final List<Json> response = new ArrayList<>();
        dataStoresLock.lock();
        try {
            response.addAll(getDataStore(dataStoreName));
        } finally {
            dataStoresLock.unlock();
        }
        return response;
    }

    /**
     * Gets the list of data store names
     *
     * @return list of data stores names
     */
    public List<String> getDataStoreNames(){
        final List<String> response = new ArrayList<>();
        dataStoresLock.lock();
        try {
            response.addAll(dataStores.keySet());
        } finally {
            dataStoresLock.unlock();
        }
        return response;
    }

    /************************
     EB API: Properties
     ************************/

    @Override
    public Json getConfiguration() throws ServiceException {
        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: get configuration", ServiceTests.TEST));

        final Json jsonResponse = Json.map()
                .set(Parameter.CONFIGURATION_PROXY, false)
                .set(Parameter.CONFIGURATION_WEB_SERVICE_URI, null);

        logger.info(String.format("%s EB: configuration response: %s", ServiceTests.TEST, jsonResponse));
        logger.info(String.format("%s --------------", ServiceTests.TEST));

        return jsonResponse;
    }


    /************************
     EB API: Users
     ************************/

    @Override
    public AppUser getUserInformationByToken(String token) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(token, "empty token");

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: get user information by token [%s]", ServiceTests.TEST, token));

        AppUser response = null;
        usersLock.lock();
        try {
            if(users.containsKey(token)){
                response = users.get(token);
            }
        } finally {
            usersLock.unlock();
        }
        logger.info(String.format("%s EB: get user information by token [%s]: %s", ServiceTests.TEST, token, response));
        logger.info(String.format("%s --------------", ServiceTests.TEST));

        if(response == null){
            throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("User not found with token [%s]", token));
        }
        return response;
    }

    @Override
    public AppUser getUserInformationByEmail(String email) throws ServiceException {
        // review parameters
        ExtensionBroker.isNotBlank(email, "empty email");

        logger.info(String.format("%s --------------", ServiceTests.TEST));
        logger.info(String.format("%s EB: get user information by email [%s]", ServiceTests.TEST, email));

        AppUser response = null;
        usersLock.lock();
        try {
            for (String token : users.keySet()) {
                AppUser user = users.get(token);
                if(email.equalsIgnoreCase(user.getEmail())){
                    response = user;
                    break;
                }
            }
        } finally {
            usersLock.unlock();
        }
        logger.info(String.format("%s EB: get user information by email [%s]: %s", ServiceTests.TEST, email, response));
        logger.info(String.format("%s --------------", ServiceTests.TEST));

        if(response == null){
            throw ServiceException.permanent(ErrorCode.ARGUMENT, String.format("User not found with email [%s]", email));
        }
        return response;
    }

    @Override
    public void clearCache() throws ServiceException {

    }

    /**
     * Add a user to be used on tests
     */
    public void addAppUser(String token, AppUser appUser){
        if(StringUtils.isEmpty(token)){
            throw new IllegalArgumentException("Invalid token");
        }
        if(appUser == null){
            throw new IllegalArgumentException("Invalid app user");
        }

        usersLock.lock();
        try {
            users.put(token, appUser);
        } finally {
            usersLock.unlock();
        }
    }

    /**
     * Clears the list of users
     */
    public void clearAppUsers(){
        usersLock.lock();
        try {
            users.clear();
        } finally {
            usersLock.unlock();
        }
    }

    /**
     * Gets the list of users
     *
     * @return list of users
     */
    public List<AppUser> getAppUsers(){
        final List<AppUser> response = new ArrayList<>();
        usersLock.lock();
        try {
            response.addAll(users.values());
        } finally {
            usersLock.unlock();
        }
        return response;
    }
}
