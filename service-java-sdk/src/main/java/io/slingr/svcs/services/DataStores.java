package io.slingr.svcs.services;

import io.slingr.svcs.Svc;
import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.exceptions.ErrorCode;
import io.slingr.svcs.services.datastores.DataStore;
import io.slingr.svcs.services.datastores.DataStoreResponse;
import io.slingr.svcs.services.exchange.Parameter;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that helps to manage the Data Stores
 *
 * <p>Created by lefunes on 20/05/15.
 */
public class DataStores {
    private static final Logger logger = LoggerFactory.getLogger(DataStores.class);

    public static final String USER_DATA_STORE = "__services_users__";

    private final ExtensionBrokerApi api;
    private final List<String> dataStores = new ArrayList<>();
    private final boolean debug;

    /**
     * Constructor only can be called by Extension Broker class
     *
     * @param api extension broker implementation
     * @param validDataStores data stores names list
     * @param debug true if the service shows information useful for debug
     */
    public DataStores(ExtensionBrokerApi api, List<String> validDataStores, boolean debug) {
        this.api = api;
        this.debug = debug;

        if (validDataStores != null) {
            for (String dataStore : validDataStores) {
                if (StringUtils.isNotBlank(dataStore)) {
                    this.dataStores.add(dataStore);
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Data Stores
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets the User Data Store to use in the service.
     *
     * @throws SvcException if there is an issue with the user data store value
     * @return user data store
     */
    public DataStore getUserDataStore() throws SvcException {
        return getDataStore(USER_DATA_STORE);
    }

    /**
     * Gets the Data Store to use in the service.
     *
     * @param dataStoreName name of the data store to properties
     * @throws SvcException if there is an issue with the data store name value
     * @return data store
     */
    public DataStore getDataStore(String dataStoreName) throws SvcException {
        if(StringUtils.isBlank(dataStoreName)){
            throw SvcException.permanent(ErrorCode.ARGUMENT, "DataStores: Empty data store name");
        }
        dataStoreName = dataStoreName.trim();
        if(!dataStores.contains(dataStoreName)){
            throw SvcException.permanent(ErrorCode.ARGUMENT, String.format("DataStores: Invalid data store name to register [%s]", dataStoreName));
        }
        return new DataStore(dataStoreName, this);
    }

    /**
     * Finds all documents from a data store.
     *
     * <p>This method is used to find all stored documents on a data store.
     *
     * @param dataStoreName data store name
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public DataStoreResponse find(String dataStoreName) throws SvcException {
        return find(dataStoreName, null);
    }

    /**
     * Finds documents from a data store.
     *
     * <p>This method is used to find one or more stored documents on a data store. A list of key-value pairs is used as
     * filter of the documents to find.
     *
     * <p>Keep in mind that in this moment the filters only work over string fields.
     *
     * <p>If no filter is given, the request will return all the documents in the data store.
     *
     * @param dataStoreName data store name
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public DataStoreResponse find(String dataStoreName, Json filter) throws SvcException {
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");

        String log = "find all";
        if(filter == null || filter.isEmpty()){
            filter = null;
        } else {
            log = String.format("find [%s]", filter.toString());
        }

        if(debug) {
            logger.info(String.format("%s dataStore=%s - %s", Svc.DEBUG, dataStoreName, log));
        }
        try {
            DataStoreResponse response = api.findDocuments(dataStoreName, filter);
            if(response == null) {
                response = new DataStoreResponse(null, 0, null);
            }

            if(debug) {
                logger.info(String.format("%s dataStore=%s - %s - items [%s] - total [%s]", Svc.DEBUG, dataStoreName, log, response.getItems().size(), response.getTotal()));
            }
            return response;
        } catch (SvcException ex){
            throw ex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when find documents on data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
        }
    }

    /**
     * Finds documents from a data store.
     *
     * <p>This method is used to find one or more stored documents on a data store. A list of key-value pairs is used as
     * filter of the documents to find.
     *
     * <p>Keep in mind that in this moment the filters only work over string fields.
     *
     * <p>If no filter is given, the request will return all the documents in the data store.
     *
     * @param dataStoreName data store name
     * @param filter filter to search documents, empty means 'all documents'
     * @param offset pagination offset to use on query
     * @param size pagination size to use on query
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public DataStoreResponse find(String dataStoreName, Json filter, String offset, Integer size) throws SvcException {
        if(filter == null){
            filter = Json.map();
        }
        // pagination options
        if(StringUtils.isNotBlank(offset)){
            filter.set(Parameter.PAGINATION_OFFSET, offset);
        }
        if(size != null){
            filter.set(Parameter.PAGINATION_SIZE, size);
        }
        return find(dataStoreName, filter);
    }

    /**
     * Counts all documents from a data store.
     *
     * <p>This method is used to known how many documents there are stored on a data store.
     *
     * @param dataStoreName data store name
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public int count(String dataStoreName) throws SvcException {
        return count(dataStoreName, null);
    }

    /**
     * Counts documents from a data store.
     *
     * <p>This method is used to known how many documents there are stored on a data store. A list of key-value pairs is
     * used as filter of the documents to count.
     *
     * <p>Keep in mind that in this moment the filters only work over string fields.
     *
     * <p>If no filter is given, the request will return all the documents in the data store.
     *
     * @param dataStoreName data store name
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public int count(String dataStoreName, Json filter) throws SvcException {
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");

        String log = "count all";
        if(filter == null || filter.isEmpty()){
            filter = null;
        } else {
            log = String.format("count [%s]", filter.toString());
        }

        if(debug) {
            logger.info(String.format("%s dataStore=%s - %s", Svc.DEBUG, dataStoreName, log));
        }
        try {
            final Json response = api.countDocuments(dataStoreName, filter);
            if(response != null){
                try {
                    Integer total = response.integer(Parameter.DATA_STORE_TOTAL);
                    if(total == null || total < 0){
                        total = 0;
                    }

                    if(debug) {
                        logger.info(String.format("%s dataStore=%s - %s - total [%s]", Svc.DEBUG, dataStoreName, log, total));
                    }
                    return total;
                } catch (Exception ex){
                    throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when convert response of data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
                }
            }
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Invalid data store response [%s]", dataStoreName));
        } catch (SvcException ex){
            throw ex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when count documents on data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
        }
    }

    /**
     * Finds a document from a data store using an id.
     *
     * <p>This method is used by the service to get a stored document using its document id.
     *
     * <p>If document does not exist on data store, Extension Broker app returns an error HTTP 404
     *
     * @param dataStoreName data store name
     * @param documentId id of document
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public Json findById(String dataStoreName, String documentId) throws SvcException {
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        ExtensionBroker.isNotBlank(dataStoreName, "empty document id");

        final String log = String.format("find by id [%s]", documentId);

        if(debug) {
            logger.info(String.format("%s dataStore=%s - %s", Svc.DEBUG, dataStoreName, log));
        }
        try {
            Json response = api.getDocument(dataStoreName, documentId);

            if(debug) {
                if(response != null){
                    logger.info(String.format("%s dataStore=%s - %s - found document id [%s]", Svc.DEBUG, dataStoreName, log, response.string(Parameter.DATA_STORE_ID)));
                } else {
                    logger.info(String.format("%s dataStore=%s - %s - not found document", Svc.DEBUG, dataStoreName, log));
                }
            }
            return response;
        } catch (SvcException ex){
            throw ex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when find a document by id on data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
        }
    }

    /**
     * Gets the first document that matches the filter from a data store.
     *
     * <p>This method is used to find one stored documents on a data store. A list of key-value pairs is used as
     * filter of the documents to find.
     *
     * <p>Keep in mind that in this moment the filters only work over string fields.
     *
     * <p>If no filter is given, the request will return the first of all the documents in the data store.
     *
     * @param dataStoreName data store name
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public Json findOne(String dataStoreName, Json filter) throws SvcException {
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");

        String log = "find one";
        if(filter == null || filter.isEmpty()){
            filter = null;
        } else {
            log = String.format("find one [%s]", filter.toString());
        }

        if(debug) {
            logger.info(String.format("%s dataStore=%s - %s", Svc.DEBUG, dataStoreName, log));
        }
        try {
            final DataStoreResponse response = api.findDocuments(dataStoreName, filter);
            if(response == null){
                throw SvcException.retryable(ErrorCode.CLIENT, String.format("Invalid data store response [%s]", dataStoreName));
            }
            if(response.getItems() != null && !response.getItems().isEmpty()){
                Json item = response.getItems().get(0);
                if(item != null) {
                    if(debug) {
                        logger.info(String.format("%s dataStore=%s - %s - found document id [%s]", Svc.DEBUG, dataStoreName, log, item.string(Parameter.DATA_STORE_ID)));
                    }
                    return item;
                }
            }
            if(debug) {
                logger.info(String.format("%s dataStore=%s - %s - not found document", Svc.DEBUG, dataStoreName, log));
            }
            return null;
        } catch (SvcException ex){
            throw ex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when find one document on data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
        }
    }

    /**
     * Saves the document on a data store.
     *
     * <p>The service can save a document on the data store using this method. The stored document is included on the
     * response along with the identified assigned.
     *
     * <p>If the document does not include an _id field, this will be added before to save it, with a random value. If
     * there is another document stored with the same _id, this will be overwritten.
     *
     * <p>Keep in mind that the filter used on find, count and delete requests work only over string fields.
     *
     * @param dataStoreName data store name
     * @param document document to save on data store
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public Json save(String dataStoreName, Json document) throws SvcException {
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");

        if(document == null){
            document = Json.map();
        }
        if(StringUtils.isBlank(document.string(Parameter.DATA_STORE_ID))){
            document.set(Parameter.DATA_STORE_ID, Strings.randomUUIDString());
        }
        final String documentId = document.string(Parameter.DATA_STORE_ID);

        String log = "save document";
        if(StringUtils.isNotBlank(documentId)){
            log = String.format("save document [%s]", documentId);
        }

        if(debug) {
            logger.info(String.format("%s dataStore=%s - %s", Svc.DEBUG, dataStoreName, log));
        }
        try {
            final Json response = api.saveDocument(dataStoreName, document);
            if(response != null) {
                if(debug) {
                    logger.info(String.format("%s dataStore=%s - saved document - id [%s]", Svc.DEBUG, dataStoreName, response.string(Parameter.DATA_STORE_ID)));
                }
                return response;
            }
        } catch (SvcException ex){
            throw ex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when save document on data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
        }
        logger.warn(String.format("dataStore=%s - %s - Error: there is not response from Extension Broker", dataStoreName, document.string(Parameter.DATA_STORE_ID)));
        throw SvcException.permanent(ErrorCode.CLIENT, "There is not response from Extension Broker");
    }

    /**
     * Updates a document on a data store.
     *
     * <p>The service updates a document on the data store using this method. Previous stored document will be
     * overwritten. The stored document is included on the response.
     *
     * <p>Keep in mind that the filter used on find, count and delete requests work only over string fields.
     *
     * <p>If document does not exist on data store, Extension Broker app returns an error HTTP 404
     *
     * @param dataStoreName data store name
     * @param document document to update on data store
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public Json update(String dataStoreName, Json document) throws SvcException {
        return update(dataStoreName, null, document);
    }

    /**
     * Updates a document on a data store.
     *
     * <p>The service updates a document on the data store using this method. Previous stored document will be
     * overwritten. The stored document is included on the response.
     *
     * <p>Keep in mind that the filter used on find, count and delete requests work only over string fields.
     *
     * <p>If document does not exist on data store, Extension Broker app returns an error HTTP 404
     *
     * @param dataStoreName data store name
     * @param documentId id of document
     * @param document document to update on data store
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public Json update(String dataStoreName, String documentId, Json document) throws SvcException {
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");

        if(document == null){
            document = Json.map();
        }
        final String docId = StringUtils.isNotBlank(documentId) ? documentId.trim() : document.string(Parameter.DATA_STORE_ID);
        ExtensionBroker.isNotBlank(docId, "empty document id");
        document.set(Parameter.DATA_STORE_ID, docId);

        String log = String.format("update document [%s]", docId);

        if(debug) {
            logger.info(String.format("%s dataStore=%s - %s", Svc.DEBUG, dataStoreName, log));
        }
        try {
            final Json response = api.updateDocument(dataStoreName, docId, document);
            if(response != null) {
                if(debug) {
                    logger.info(String.format("%s dataStore=%s - updated document - id [%s]", Svc.DEBUG, dataStoreName, response.string(Parameter.DATA_STORE_ID)));
                }
                return response;
            }
        } catch (SvcException ex){
            throw ex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when update document on data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
        }
        logger.warn(String.format("dataStore=%s - %s - Error: there is not response from Extension Broker", dataStoreName, document.string(Parameter.DATA_STORE_ID)));
        throw SvcException.permanent(ErrorCode.CLIENT, "There is not response from Extension Broker");
    }

    /**
     * Deletes all documents on a data store.
     *
     * <p>The service can delete all documents through this method.
     *
     * @param dataStoreName data store name
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public boolean remove(String dataStoreName) throws SvcException {
        return remove(dataStoreName, null);
    }

    /**
     * Deletes documents on a data store.
     *
     * <p>The service can delete one or more documents through this method. A list of key-value pairs is used as filter
     * of the documents to delete.
     *
     * <p>Keep in mind that in this moment the filters only work over string fields.
     *
     * <p>If no filter is given, the request will remove all the documents in the data store.
     *
     * <p>If any document was removed from data store, Extension Broker app returns 0 as total.
     *
     * @param dataStoreName data store name
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public boolean remove(String dataStoreName, Json filter) throws SvcException {
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");

        String log = "remove all";
        if(filter == null || filter.isEmpty()){
            filter = null;
        } else {
            log = String.format("remove [%s]", filter.toString());
        }

        if(debug) {
            logger.info(String.format("%s dataStore=%s - %s", Svc.DEBUG, dataStoreName, log));
        }
        try {
            final Json response = api.removeDocuments(dataStoreName, filter);
            return processRemoveResponse(dataStoreName, response);
        } catch (SvcException ex){
            throw ex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when remove documents on data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
        }
    }

    /**
     * Deletes a document on a data store using an id.
     *
     * <p>This method is used by the service to remove a stored document using its document id.
     *
     * <p>If document does not exist on data store, Extension Broker app returns an error HTTP 404
     *
     * @param dataStoreName data store name
     * @param documentId id of document
     * @return result of the execution of the command on data store
     * @throws SvcException if there is an issue with the exchange
     */
    public boolean removeById(String dataStoreName, String documentId) throws SvcException {
        ExtensionBroker.isNotBlank(dataStoreName, "empty data store name");
        ExtensionBroker.isNotBlank(dataStoreName, "empty document id");

        final String log = String.format("remove by id [%s]", documentId);

        if(debug) {
            logger.info(String.format("%s dataStore=%s - %s", Svc.DEBUG, dataStoreName, log));
        }
        try {
            final Json response = api.removeDocument(dataStoreName, documentId);
            return processRemoveResponse(dataStoreName, response);
        } catch (SvcException ex){
            throw ex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when remove a document by id on data store [%s]: %s", dataStoreName, ex.getMessage()), ex);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // helper methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Process the response from a remove document method
     *
     * @param dataStoreName data store name
     * @param response result of the execution of the command on data store
     * @return true if one or more documents were deleted
     */
    private boolean processRemoveResponse(String dataStoreName, Json response) throws SvcException {
        if(response != null){
            try {
                Boolean result = response.bool(Parameter.DATA_STORE_RESULT);
                if(result != null){
                    if(debug) {
                        logger.info(String.format("%s dataStore=%s - removed [%s] - total [%s]", Svc.DEBUG, dataStoreName, result, response.integer(Parameter.DATA_STORE_TOTAL)));
                    }
                    return result;
                }
            } catch (Exception ex){
                throw SvcException.retryable(ErrorCode.CONVERSION, String.format("Exception when convert data store response [%s]: %s", dataStoreName, ex.getMessage()), ex);
            }
        }
        throw SvcException.retryable(ErrorCode.CLIENT, String.format("Invalid data store response [%s]: %s", dataStoreName, response));
    }
}
