package io.slingr.services.services;

import io.slingr.services.Service;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.services.datastores.DataStore;
import io.slingr.services.services.exchange.ReservedName;
import io.slingr.services.utils.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Helper to execute commons operation over the users data store
 *
 * <p>Created by lefunes on 10/06/15.
 */
public class Users {
    private static final Logger logger = LoggerFactory.getLogger(Users.class);

    protected final DataStore users;
    protected final Events events;
    protected final boolean debug;

    /**
     * Creates a user manager over a data store
     *
     * @param users data store used for user configurations
     * @param events module to send event to the platform
     * @param debug true if the service shows information useful for debug
     */
    public Users(DataStore users, Events events, boolean debug) {
        this.users = users;
        this.events = events;
        this.debug = debug;
    }

    /**
     * Gets the Data Store for users to use in the service.
     *
     * @return Users data store
     */
    public DataStore getUserDataStore(){
        return users;
    }

    /**
     * Saves the user configuration. The new configuration is merged with the last stored on data store.
     *
     * @param userId id of the user
     * @param newConfiguration new configuration to save
     * @return configuration stored
     */
    public Json save(String userId, Json newConfiguration){
        return save(userId, newConfiguration, true);
    }

    /**
     * Saves the user configuration.
     *
     * @param userId id of the user
     * @param newConfiguration new configuration to save
     * @param mergeConfiguration true if the configuration must be merged with the last stored on data store.
     * @return configuration stored
     */
    public Json save(String userId, Json newConfiguration, boolean mergeConfiguration){
        if(StringUtils.isNotBlank(userId)){
            if(debug) {
                logger.info(String.format("%s save user configuration [%s]", Service.DEBUG, userId));
            }
            if(users == null) {
                logger.warn("User data store is not ready to be used yet");
            } else {
                try {
                    // check last user configuration
                    Json user = mergeConfiguration ? findById(userId) : null;
                    if (user == null) {
                        user = Json.map();
                    }

                    // check new user configuration
                    if (newConfiguration != null) {
                        user.merge(newConfiguration);
                    }

                    // save configuration
                    user.set("_id", userId);
                    users.save(user);

                    if(debug) {
                        logger.info(String.format("%s user configuration [%s] was saved [%s]", Service.DEBUG, userId, user.toString()));
                    }

                    return user;
                } catch (Exception ex) {
                    logger.warn(String.format("Error when try to save user configuration [%s] [%s]", userId, ex.getMessage()), ex);
                }
            }
        } else {
            logger.warn(String.format("User id is empty [%s]", userId));
        }
        return null;
    }

    /**
     * Removes the user configuration from data store.
     *
     * @param userId id of the user
     */
    public void removeById(String userId){
        if(StringUtils.isNotBlank(userId)){
            if(debug) {
                logger.info(String.format("%s remove user configuration [%s]", Service.DEBUG, userId));
            }
            if(users == null) {
                logger.warn("User data store is not ready to be used yet");
            } else {
                try {
                    // remove last user configuration
                    users.removeById(userId);

                    if(debug) {
                        logger.info(String.format("%s user configuration [%s] was deleted", Service.DEBUG, userId));
                    }
                } catch (Exception ex) {
                    logger.warn(String.format("Error when try to delete user configuration [%s] [%s]", userId, ex.getMessage()), ex);
                }
            }
        }
    }

    /**
     * Finds the user configuration on the data store.
     *
     * @param userId id of the user.
     * @return user configuration.
     */
    public Json findById(String userId){
        Json response = null;
        if(StringUtils.isNotBlank(userId)){
            if(debug) {
                logger.info(String.format("%s checking user configuration [%s]", Service.DEBUG, userId));
            }
            if(users == null){
                logger.warn("User data store is not ready to be used yet");
            } else {
                try {
                    // check last user configuration
                    response = users.findById(userId);
                } catch (Exception ex) {
                    if(debug) {
                        logger.info(String.format("%s Error when try to find user configuration [%s] [%s]", Service.DEBUG, userId, ex.getMessage()));
                    }
                }

                if (response != null && !response.isEmpty()) {
                    logger.info(String.format("User configuration [%s] was found", userId));
                } else {
                    logger.info(String.format("User configuration [%s] was not found", userId));
                }
            }
        }
        return response;
    }

    /**
     * Checks if the user is connected.
     * <ul>
     * <li>If the user is connected, a merge between the information stored and the given json is returned.</li>
     * <li>If the user is not connected, an exception is thrown.</li>
     * </ul>
     *
     * @param userId user id
     * @param newConfiguration configuration to merge with the stored configuration
     * @return merged configuration
     * @throws ServiceException if fails the check
     */
    public Json checkUserConnection(String userId, Json newConfiguration) throws ServiceException {
        Json userConf = null;
        String error = null;

        if(StringUtils.isNotBlank(userId)) {
            final Json conf = findById(userId);
            if (conf != null && !conf.isEmpty()) {
                userConf = conf;
            } else {
                error = String.format("User [%s] is not connected", userId);
            }
        } else {
            error = String.format("Invalid user id [%s]", userId);
        }
        if(userConf == null){
            throw ServiceException.permanent(ErrorCode.CLIENT, error!=null ? error: "User validation error").returnCode(404);
        }
        final Json data;
        if(newConfiguration == null){
            data = Json.map();
        } else {
            data = newConfiguration;
        }
        userConf.forEachMap(data::set);
        return data;
    }

    /**
     * Sends an user connected event to the app runtime
     *
     * @param userId id of the connected user
     * @param userConfiguration saved user configuration
     */
    public void sendUserConnectedEvent(String userId, Json userConfiguration){
        sendUserConnectedEvent(null, userId, userConfiguration);
    }

    /**
     * Sends an user connected event to the app runtime
     *
     * @param functionId function id that generates the event (used to identify the callbacks)
     * @param userId id of the connected user
     * @param userConfiguration saved user configuration
     */
    public void sendUserConnectedEvent(String functionId, String userId, Json userConfiguration){
        events.send(ReservedName.USER_CONNECTED, userConfiguration, functionId, userId);
    }

    /**
     * Sends an user disconnected event to the app runtime
     *
     * @param userId id of the disconnected user
     */
    public void sendUserDisconnectedEvent(String userId){
        sendUserDisconnectedEvent(null, userId);
    }

    /**
     * Sends an user disconnected event to the app runtime
     *
     * @param functionId function id that generates the event (used to identify the callbacks)
     * @param userId id of the disconnected user
     */
    public void sendUserDisconnectedEvent(String functionId, String userId){
        events.send(null, ReservedName.USER_DISCONNECTED, null, functionId, userId, null);
    }

    /**
     * Connect a user and saves the information on the users data store.
     * <p>Generates a 'user connected' or 'user disconnected' event as result.
     *
     * @param userId user id
     * @param userConfiguration user configuration
     * @return saved configuration after the connection
     * @throws ServiceException if the connection fails
     */
    public Json connect(String userId, Json userConfiguration) throws ServiceException {
        return connect(userId, userConfiguration, null);
    }

    /**
     * Connect a user and saves the information on the users data store.
     * <p>Generates a 'user connected' or 'user disconnected' event as result.
     *
     * @param userId user id
     * @param userConfiguration user configuration
     * @param functionId id of the function that generates the connection
     * @return saved configuration after the connection
     * @throws ServiceException if the connection fails
     */
    public Json connect(String userId, Json userConfiguration, String functionId) throws ServiceException {
        if(StringUtils.isBlank(userId)) {
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "User ID is required").returnCode(400);
        }

        // saves the information on the users data store
        final Json savedConfiguration = save(userId, userConfiguration);
        if(savedConfiguration == null){
            throw ServiceException.permanent(ErrorCode.CLIENT, "Error when try to connect to Extension Broker app");
        }
        logger.info(String.format("User connected [%s] [%s]", userId, savedConfiguration.toString()));

        // sends connected user event
        sendUserConnectedEvent(functionId, userId, savedConfiguration);

        return savedConfiguration;
    }

    /**
     * Disconnect a user and deletes the information stored on the users data store.
     * <p>Generates a 'user disconnected' event as result.
     *
     * @param userId user id
     * @throws ServiceException if the disconnection fails
     */
    public void disconnect(String userId) throws ServiceException {
        disconnect(userId, null);
    }

    /**
     * Disconnect a user and deletes the information stored on the users data store.
     * <p>Generates a 'user disconnected' event as result.
     *
     * @param userId user id
     * @param functionId id of the function that generates the disconnection
     * @throws ServiceException if the disconnection fails
     */
    public void disconnect(String userId, String functionId) throws ServiceException {
        if(StringUtils.isBlank(userId)) {
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "User ID is required").returnCode(400);
        }

        // deletes the information stored on the users data store
        removeById(userId);

        // send disconnected user event
        sendUserDisconnectedEvent(functionId, userId);
    }
}
