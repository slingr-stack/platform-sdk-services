package io.slingr.svcs.utils.tests;

import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.exceptions.ErrorCode;
import io.slingr.svcs.services.ExtensionBroker;
import io.slingr.svcs.services.ExtensionBrokerApi;
import io.slingr.svcs.services.application.AppUser;
import io.slingr.svcs.services.datastores.DataStoreResponse;
import io.slingr.svcs.services.exchange.Parameter;
import io.slingr.svcs.services.logs.AppLogLevel;
import io.slingr.svcs.services.rest.DownloadedFile;
import io.slingr.svcs.utils.FilesUtils;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.Strings;
import io.slingr.svcs.utils.converters.JsonSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Implementation of the methods defined on the Extension Broker  API to be used on testing time
 *
 * <p>Created by lefunes on 18/06/18.
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
     * @throws SvcException if something fails when creates the Extension Broker APi instance
     */
    public ExtensionBrokerMock() throws SvcException {}

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Events
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void newEvent(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws SvcException {
        processEvent("event", date, event, data, fromFunctionId, userId, userEmail);
        logger.info(String.format("%s --------------", SvcTests.TEST));
    }

    @Override
    public Object newSyncEvent(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws SvcException {
        final Object response = processEvent("sync event", date, event, data, fromFunctionId, userId, userEmail);
        logger.info(String.format("%s ES: sync events response: %s", SvcTests.TEST, response));
        logger.info(String.format("%s --------------", SvcTests.TEST));
        return response;
    }

    /**
     * Checks and processes the received event
     */
    private Object processEvent(String type, Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws SvcException {
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

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: new %s: %s", SvcTests.TEST, type, eventContent));

        Object response = null;
        eventsLock.lock();
        try {
            receivedEvents.add(eventContent);

            if(eventProcessors.containsKey(event)){
                logger.info(String.format("%s ES: event processor found for [%s]", SvcTests.TEST, event));
                response = eventProcessors.get(event).processMessage(eventContent);
            } else {
                logger.info(String.format("%s ES: default event response for [%s]", SvcTests.TEST, event));
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

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: App logs
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void newAppLogs(Long date, String level, String message, Json additionalInfo) throws SvcException {
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

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: app log: %s", SvcTests.TEST, restContent));
        logger.info(String.format("%s --------------", SvcTests.TEST));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Distributed locks
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Json acquireLock(String key) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(key, "empty key to lock");

        boolean acquired = false;
        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: acquire lock: %s", SvcTests.TEST, key));
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
        logger.info(String.format("%s ES: lock: %s", SvcTests.TEST, jsonResponse));
        logger.info(String.format("%s --------------", SvcTests.TEST));
        return jsonResponse;
    }

    @Override
    public Json releaseLock(String key) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(key, "empty key to unlock");

        boolean released = false;
        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: release lock: %s", SvcTests.TEST, key));
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
        logger.info(String.format("%s ES: unlock: %s", SvcTests.TEST, jsonResponse));
        logger.info(String.format("%s --------------", SvcTests.TEST));
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
                    .collect(Collectors.toList())
            );
        } finally {
            locksLock.unlock();
        }
        return locks;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Files management
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Json uploadFile(String filename, InputStream content, String contentType) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(filename, "empty file name");
        ExtensionBroker.isNotNull(content, "empty file content");

        // save content in a temporal file
        final String fileContent = Strings.readAsString(content);
        if(fileContent == null){
            logger.warn(String.format("Local copy not created for file [%s]. The file will not be uploaded.", filename));
            throw SvcException.permanent(ErrorCode.CONVERSION, String.format("File [%s] was not uploaded. The file could not be downloaded.", filename));
        }
        final long fileLength = fileContent.length();
        if(fileLength < 1){
            logger.warn(String.format("Empty local copy of file [%s]. The file will not be uploaded.", filename));
            return null;
        }

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: upload file [%s], length [%s], content type [%s]", SvcTests.TEST, filename, fileLength, contentType));

        // send file to ES
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
        logger.info(String.format("%s ES: uploaded file: %s", SvcTests.TEST, response));
        logger.info(String.format("%s --------------", SvcTests.TEST));
        return response;
    }

    @Override
    public DownloadedFile downloadFile(String fileId) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(fileId, "empty file id");

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: download file [%s]", SvcTests.TEST, fileId));

        filesLock.lock();
        try {
            if(files.containsKey(fileId)){
                final FileMock file = files.get(fileId);
                final InputStream is = Strings.readAsInputStream(file.fileContent);

                final DownloadedFile response = new DownloadedFile(200, is, file.getHeaders());
                logger.info(String.format("%s ES: downloaded file: %s", SvcTests.TEST, response));
                logger.info(String.format("%s --------------", SvcTests.TEST));
                return response;
            }
        } finally {
            filesLock.unlock();
        }
        throw SvcException.permanent(ErrorCode.CLIENT, String.format("%s File does not exists on application [%s]", SvcTests.TEST, fileId));
    }

    @Override
    public Json getFileMetadata(String fileId) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(fileId, "empty file id");

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: get file metadata [%s]", SvcTests.TEST, fileId));

        Json response = null;
        filesLock.lock();
        try {
            if(files.containsKey(fileId)){
                response = files.get(fileId).getMetadata();
            }
        } finally {
            filesLock.unlock();
        }
        logger.info(String.format("%s ES: file metadata: %s", SvcTests.TEST, response));
        logger.info(String.format("%s --------------", SvcTests.TEST));
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
    public class FileMock implements JsonSource {

        private String fileContent;
        private String fileId;
        private String fileName;
        private String contentType;
        private Long length;
        private Long uploadDate;
        private Long expirationDate;
        private Boolean expired;

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

        public String getFileName() {
            return fileName;
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


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Data stores management
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public DataStoreResponse findDocuments(String dataStoreName, Json filter) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        filter = ExtensionBroker.checkDataStoreFilter(filter);

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: find documents [%s], filter [%s]", SvcTests.TEST, dataStoreName, filter));

        DataStoreResponse response;
        dataStoresLock.lock();
        try {
            response = getDataStoreItems(dataStoreName, filter);
        } finally {
            dataStoresLock.unlock();
        }
        logger.info(String.format("%s ES: found documents: %s", SvcTests.TEST, response));
        logger.info(String.format("%s --------------", SvcTests.TEST));
        return response;
    }

    @Override
    public Json countDocuments(String dataStoreName, Json filter) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        filter = ExtensionBroker.checkDataStoreFilter(filter);

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: count documents [%s], filter [%s]", SvcTests.TEST, dataStoreName, filter));

        Json response;
        dataStoresLock.lock();
        try {
            final DataStoreResponse items = getDataStoreItems(dataStoreName, filter);
            response = Json.map()
                    .set(Parameter.DATA_STORE_TOTAL, items.getTotal());
        } finally {
            dataStoresLock.unlock();
        }
        logger.info(String.format("%s ES: found documents count: %s", SvcTests.TEST, response));
        logger.info(String.format("%s --------------", SvcTests.TEST));
        return response;
    }

    @Override
    public Json getDocument(String dataStoreName, String documentId) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        ExtensionBroker.isNotBlank(documentId, "empty document id");

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: find document [%s], document id [%s]", SvcTests.TEST, dataStoreName, documentId));

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
        logger.info(String.format("%s ES: found document: %s", SvcTests.TEST, response));
        logger.info(String.format("%s --------------", SvcTests.TEST));
        return response;
    }

    @Override
    public Json saveDocument(String dataStoreName, Json document) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        ExtensionBroker.isNotNull(document, "empty document");

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: save document [%s], document [%s]", SvcTests.TEST, dataStoreName, document));

        String documentId = document.string(Parameter.DATA_STORE_ID);
        if(StringUtils.isBlank(documentId)){
            documentId = Strings.randomUUIDString();
        }

        document = saveDocument(dataStoreName, documentId, document);

        logger.info(String.format("%s ES: saved document: %s", SvcTests.TEST, document));
        logger.info(String.format("%s --------------", SvcTests.TEST));
        return document;
    }

    @Override
    public Json updateDocument(String dataStoreName, String documentId, Json document) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        ExtensionBroker.isNotBlank(documentId, "empty document id");
        ExtensionBroker.isNotNull(document, "empty document");

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: update document [%s], document id [%s], document [%s]", SvcTests.TEST, dataStoreName, documentId, document));

        document = saveDocument(dataStoreName, documentId, document);

        logger.info(String.format("%s ES: updated document: %s", SvcTests.TEST, document));
        logger.info(String.format("%s --------------", SvcTests.TEST));
        return document;
    }

    @Override
    public Json removeDocuments(String dataStoreName, Json filter) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        filter = ExtensionBroker.checkDataStoreFilter(filter);

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: remove documents [%s], filter [%s]", SvcTests.TEST, dataStoreName, filter));

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

        logger.info(String.format("%s ES: removed documents: %s", SvcTests.TEST, response));
        logger.info(String.format("%s --------------", SvcTests.TEST));
        return response;
    }

    @Override
    public Json removeDocument(String dataStoreName, String documentId) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        ExtensionBroker.isNotBlank(documentId, "empty document id");

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: remove document [%s], document id [%s]", SvcTests.TEST, dataStoreName, documentId));

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

        logger.info(String.format("%s ES: removed document: %s", SvcTests.TEST, response));
        logger.info(String.format("%s --------------", SvcTests.TEST));
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

    private Json saveDocument(String dataStoreName, String documentId, Json document) {
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
        return document;
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

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Properties
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Json getConfiguration() throws SvcException {
        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: get configuration", SvcTests.TEST));

        final Json jsonResponse = Json.map()
                .set(Parameter.CONFIGURATION_PROXY, false)
                .set(Parameter.CONFIGURATION_WEB_SERVICE_URI, null);

        logger.info(String.format("%s ES: configuration response: %s", SvcTests.TEST, jsonResponse));
        logger.info(String.format("%s --------------", SvcTests.TEST));

        return jsonResponse;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // ES API: Users
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public AppUser getUserInformationByToken(String token) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(token, "empty token");

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: get user information by token [%s]", SvcTests.TEST, token));

        AppUser response = null;
        usersLock.lock();
        try {
            if(users.containsKey(token)){
                response = users.get(token);
            }
        } finally {
            usersLock.unlock();
        }
        logger.info(String.format("%s ES: get user information by token [%s]: %s", SvcTests.TEST, token, response));
        logger.info(String.format("%s --------------", SvcTests.TEST));

        if(response == null){
            throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("User not found with token [%s]", token));
        }
        return response;
    }

    @Override
    public AppUser getUserInformationByEmail(String email) throws SvcException {
        // review parameters
        ExtensionBroker.isNotBlank(email, "empty email");

        logger.info(String.format("%s --------------", SvcTests.TEST));
        logger.info(String.format("%s ES: get user information by email [%s]", SvcTests.TEST, email));

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
        logger.info(String.format("%s ES: get user information by email [%s]: %s", SvcTests.TEST, email, response));
        logger.info(String.format("%s --------------", SvcTests.TEST));

        if(response == null){
            throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("User not found with email [%s]", email));
        }
        return response;
    }

    @Override
    public void clearCache() throws SvcException {

    }

    /**
     * Add an user to be used on tests
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
