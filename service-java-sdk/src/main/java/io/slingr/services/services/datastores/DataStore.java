package io.slingr.services.services.datastores;

import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.services.DataStores;
import io.slingr.services.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Service class over the data stores
 *
 * <p>Created by lefunes on 29/05/15.
 */
public class DataStore {
    private static final Logger logger = LoggerFactory.getLogger(DataStore.class);

    private final String dataStoreName;
    private final DataStores dataStores;

    /**
     * Data Store helper class
     *
     * @param dataStoreName data store name
     * @param dataStores dataStores of the service
     */
    public DataStore(String dataStoreName, DataStores dataStores) {
        this.dataStoreName = dataStoreName;
        this.dataStores = dataStores;
    }

    /**
     * Gets the aname of the data store
     *
     * @return data store name
     */
    public String getName() {
        return this.dataStoreName;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // data stores management
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Finds all documents from a data store.
     *
     * <p>This method is used to find all stored documents on a data store.
     *
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public DataStoreResponse find() throws ServiceException {
        return dataStores.find(dataStoreName, null);
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
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public DataStoreResponse find(Json filter) throws ServiceException {
        return dataStores.find(dataStoreName, filter);
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
     * @param filter filter to search documents, empty means 'all documents'
     * @param offset pagination offset to use on query
     * @param size pagination size to use on query
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public DataStoreResponse find(Json filter, String offset, Integer size) throws ServiceException {
        return dataStores.find(dataStoreName, filter, offset, size);
    }

    /**
     * Counts all documents from a data store.
     *
     * <p>This method is used to known how many documents there are stored on a data store.
     *
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public int count() throws ServiceException {
        return dataStores.count(dataStoreName);
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
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public int count(Json filter) throws ServiceException {
        return dataStores.count(dataStoreName, filter);
    }

    /**
     * Finds a document from a data store using an id.
     *
     * <p>This method is used by the service to get a stored document using its document id.
     *
     * <p>If document does not exist on data store, Extension Broker app returns an error HTTP 404
     *
     * @param documentId id of document
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public Json findById(String documentId) throws ServiceException {
        return dataStores.findById(dataStoreName, documentId);
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
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public Json findOne(Json filter) throws ServiceException {
        return dataStores.findOne(dataStoreName, filter);
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
     * @param document document to save on data store
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public Json save(Json document) throws ServiceException {
        return dataStores.save(dataStoreName, document);
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
     * @param document document to update on data store
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public Json update(Json document) throws ServiceException {
        return dataStores.update(dataStoreName, document);
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
     * @param documentId id of document
     * @param document document to update on data store
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public Json update(String documentId, Json document) throws ServiceException {
        return dataStores.update(dataStoreName, documentId, document);
    }

    /**
     * Deletes all documents on a data store.
     *
     * <p>The service can delete all documents through this method.
     *
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public boolean remove() throws ServiceException {
        return dataStores.remove(dataStoreName);
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
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public boolean remove(Json filter) throws ServiceException {
        return dataStores.remove(dataStoreName, filter);
    }

    /**
     * Deletes a document on a data store using an id.
     *
     * <p>This method is used by the service to remove a stored document using its document id.
     *
     * <p>If document does not exist on data store, Extension Broker app returns an error HTTP 404
     *
     * @param documentId id of document
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    public boolean removeById(String documentId) throws ServiceException {
        return dataStores.removeById(dataStoreName, documentId);
    }
}
