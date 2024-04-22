package io.slingr.services.services;

import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.services.application.AppUser;
import io.slingr.services.services.datastores.DataStoreResponse;
import io.slingr.services.services.rest.DownloadedFile;
import io.slingr.services.utils.Json;

import java.io.InputStream;

/**
 * Interface that defines all the methods defined by the Extension Broker API
 *
 */
public interface ExtensionBrokerApi {

    /************************
     Events
     ************************/

    /**
     * Sends an event to the application.
     *
     * <p>The service uses this method to trigger an event to the application.
     * Extension Broker returns immediately an HTTP 200 code and the event is sent asynchronously to the app.
     *
     * @param date Timestamp that represents the moment when the event was generated. It takes the value of the
     *             milliseconds from '01/01/1970 12:00 AM'. Per example, 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param event Name of the event.
     *              This must be a valid event name (an event name is valid when it is declared on the appService.json file)
     * @param data Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId Id of the user that generates the event. When we know this information, this is the preferred
     *               method for identifying a user
     * @param userEmail Email of the user that generates the event. This is an alternative method for identifying a
     *                  user. Services can send both at the same time, but the application starts to search the user by
     *                  id.
     * @throws ServiceException if there is an issue with the exchange
     */
    void newEvent(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws ServiceException;

    /**
     * Sends an event to the application and waits the response.
     *
     * <p>The service uses this method to trigger an event to the application and waits that the application returns a
     * response.
     * Extension Broker immediately sends the event to the app.
     *
     * <p>The response depends of the event processing on application side.
     * It can be a string, json or list
     *
     * @param date Timestamp that represents the moment when the event was generated. It takes the value of the
     *             milliseconds from '01/01/1970 12:00 AM'. Per example, 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param event Name of the event.
     *              This must be a valid event name (an event name is valid when it is declared on the appService.json file)
     * @param data Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId Id of the user that generates the event. When we know this information, this is the preferred
     *               method for identifying a user
     * @param userEmail Email of the user that generates the event. This is an alternative method for identifying a
     *                  user. Services can send both at the same time, but the application starts to search the user by
     *                  id.
     * @return response from application
     * @throws ServiceException if there is an issue with the exchange
     */
    Object newSyncEvent(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws ServiceException;

    /************************
     App logs
     ************************/

    /**
     * Sends an app log to the application in order to show the message to the app developer.
     *
     * <p>The service uses this method to register app logs on the application in order to show messages to the app
     * developer about change of status on the external service, errors that happened inside service, etc. Extension
     * Broker immediately returns an HTTP 200 code and the app log is sent asynchronously to the app.
     *
     * @param date Timestamp that represents the moment when the app log was generated. It takes the value of the
     *             milliseconds from '01/01/1970 12:00 AM'. Per example, 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param level Level of the app log. This must be one of 'INFO' (default value), 'WARN' or 'ERROR'.
     * @param message Message to show as app log to the developer.
     * @param additionalInfo Complementary information related to the message like external service responses, exception
     *                       stack trace, etc.
     * @throws ServiceException if there is an issue with the exchange
     */
    void newAppLogs(Long date, String level, String message, Json additionalInfo) throws ServiceException;

    /************************
     Distributed locks
     ************************/

    /**
     * Tries to acquire the lock for the specified key.
     *
     * <p>This method is used when a service needs to perform a lock over a key in order to synchronize processes
     * between service instances.
     *
     * @param key Key to be used to perform a lock.
     * @return response from application
     */
    Json acquireLock(String key) throws ServiceException;

    /**
     * Releases the lock for the specified key.
     *
     * <p>This method is used when a service needs to unlock a key in order to permit to other synchronized service
     * instances locking it.
     *
     * @param key Key to be used to perform an unlocking.
     * @return response from application
     * @throws ServiceException if there is an issue with the exchange
     */
    Json releaseLock(String key) throws ServiceException;

    /************************
     Files management
     ************************/

    /**
     * Uploads the given file to the platform.
     *
     * <p>The services use this method to upload files to the application.
     *
     * <p>The file content must be sent on the request as file parameter of a multipart/form-data.
     * As a result, the service receives a json that includes the assigned fileId.
     *
     * @param filename name of the file with extension
     * @param content content of the file
     * @param contentType content type to use when the file is uploaded
     * @return json that includes the file identifier (fileId) of the file on the platform
     * @throws ServiceException if there is an issue with the exchange
     */
    Json uploadFile(String filename, InputStream content, String contentType) throws ServiceException;

    /**
     * Downloads from the application a file using its file ID.
     *
     * <p>The service gets the content of a file stored on the application using this method. The file id value is used
     * to identify the file on app.
     * As a result, we obtain the file content as a stream of bytes.
     *
     * @param fileId identifier of the file to download
     * @return file data that includes the stream of the file content
     * @throws ServiceException if there is an issue with the exchange
     */
    DownloadedFile downloadFile(String fileId) throws ServiceException;

    /**
     * Gets the file metadata using its ID from the application.
     *
     * <p>The service gets the metadata of a file stored on the application using this method. The file id value is
     * used to identify the file on app.
     * As a result, we receive a json that includes the file metadata.
     *
     * @param fileId identifier of the file
     * @return json that contains file metadata
     * @throws ServiceException if there is an issue with the exchange
     */
    Json getFileMetadata(String fileId) throws ServiceException;

    /************************
     Data stores management
     ************************/

    /**
     * Finds documents from a data store.
     *
     * <p>This method is used to find one or more stored documents on a data store. A list of key-value pairs is used as
     * a filter of the documents to find.
     *
     * <p>Keep in mind that at this moment the filters only work over string fields.
     *
     * <p>If no filter is given, the request will return all the documents in the data store.
     *
     * @param dataStoreName data store name
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    DataStoreResponse findDocuments(String dataStoreName, Json filter) throws ServiceException;

    /**
     * Counts documents from a data store.
     *
     * <p>This method is used to know how many documents there are stored on a data store.
     * A list of key-value pairs is used as a filter of the documents to count.
     *
     * <p>Keep in mind that at this moment the filters only work over string fields.
     *
     * <p>If no filter is given, the request will return all the documents in the data store.
     *
     * @param dataStoreName data store name
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    Json countDocuments(String dataStoreName, Json filter) throws ServiceException;

    /**
     * Finds a document from a data store using an id.
     *
     * <p>This method is used by the service to get a stored document using its document id.
     *
     * <p>If a document does not exist on data store, Extension Broker app returns an error HTTP 404
     *
     * @param dataStoreName data store name
     * @param documentId id of document
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    Json getDocument(String dataStoreName, String documentId) throws ServiceException;

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
     * @throws ServiceException if there is an issue with the exchange
     */
    Json saveDocument(String dataStoreName, Json document) throws ServiceException;

    /**
     * Updates a document on a data store.
     *
     * <p>The service updates a document on the data store using this method. Previous stored document will be
     * overwritten. The stored document is included in the response.
     *
     * <p>Keep in mind that the filter used on find, count and delete requests work only over string fields.
     *
     * <p>If a document does not exist on data store, Extension Broker app returns an error HTTP 404
     *
     * @param dataStoreName data store name
     * @param documentId id of document
     * @param document document to update on data store
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    Json updateDocument(String dataStoreName, String documentId, Json document) throws ServiceException;

    /**
     * Deletes documents on a data store.
     *
     * <p>The service can delete one or more documents through this method.
     * A list of key-value pairs is used as a filter of the documents to delete.
     *
     * <p>Keep in mind that at this moment the filters only work over string fields.
     *
     * <p>If no filter is given, the request will remove all the documents in the data store.
     *
     * <p>If any document was removed from data store, Extension Broker app returns 0 as total.
     *
     * @param dataStoreName data store name
     * @param filter filter to search documents, empty means 'all documents'
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    Json removeDocuments(String dataStoreName, Json filter) throws ServiceException;

    /**
     * Deletes a document on a data store using an id.
     *
     * <p>This method is used by the service to remove a stored document using its document id.
     *
     * <p>If a document does not exist on data store, Extension Broker app returns an error HTTP 404
     *
     * @param dataStoreName data store name
     * @param documentId id of document
     * @return result of the execution of the command on data store
     * @throws ServiceException if there is an issue with the exchange
     */
    Json removeDocument(String dataStoreName, String documentId) throws ServiceException;

    /************************
     Properties
     ************************/

    /**
     * Receive the configuration about the Extension Broker .
     *
     * <p>This method is used by the service to know if it is working directly with Extension Broker or through a
     * Proxy
     * service.
     *
     * @return a json map that contains configuration and metadata about the service
     * @throws ServiceException if there is an issue with the exchange
     */
    Json getConfiguration() throws ServiceException;

    /************************
     Users
     ************************/

    /**
     * Gets the user information using an active token
     *
     * @param token user token
     * @return information about a user on the application if the token is valid and active
     */
    AppUser getUserInformationByToken(String token) throws ServiceException;

    /**
     * Gets the user information using a valid email
     *
     * @param email user email
     * @return information about a user on the application if the email is valid
     */
    AppUser getUserInformationByEmail(String email) throws ServiceException;

    /************************
     Management
     ************************/

    /**
     * Clears the cache of the app.
     *
     * @throws ServiceException if there is an exception, trying to clear cache
     */
    void clearCache() throws ServiceException;
}
