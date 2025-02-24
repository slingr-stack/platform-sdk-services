package io.slingr.services.services;

import io.slingr.services.Service;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.services.application.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that helps to find information about application users
 *
 * <p>Created by lefunes on 04/07/18.
 */
public class AppUsers {
    private static final Logger logger = LoggerFactory.getLogger(AppUsers.class);

    private final ExtensionBrokerApi api;
    private final boolean debug;

    /**
     * Constructor only can be called by Extension Broker class
     *
     * @param api extension broker implementation
     * @param debug true if the service shows information useful for debug
     */
    public AppUsers(ExtensionBrokerApi api, boolean debug) {
        this.api = api;
        this.debug = debug;
    }

    /**
     * Gets the user information using an active token
     *
     * @param token user token
     * @return the json of the user if the token is valid and active
     */
    public AppUser findByToken(String token) throws ServiceException {
        if(debug) {
            logger.info(String.format("%s find app user by token [%s]", Service.DEBUG, token));
        }
        try {
            final AppUser user = api.getUserInformationByToken(token);
            if(debug) {
                logger.info(String.format("%s found app user by token [%s]: %s", Service.DEBUG, token, user != null ? user.toString() : "-"));
            }
            return user;
        } catch (ServiceException ex){
            logger.warn(String.format("Exception when find app user by token: %s", ex.getMessage()), ex);
            throw ex;
        } catch (Exception ex){
            final String log = String.format("Exception when find app user by token: %s", ex.getMessage());
            logger.warn(log, ex);
            throw ServiceException.retryable(ErrorCode.CLIENT, log, ex);
        }
    }

    /**
     * Gets the user information using a valid email
     *
     * @param email user email
     * @return the json of the user if the email is valid
     */
    public AppUser findByEmail(String email) throws ServiceException {
        if(debug) {
            logger.info(String.format("%s find app user by email [%s]", Service.DEBUG, email));
        }
        try {
            final AppUser user = api.getUserInformationByEmail(email);
            if(debug) {
                logger.info(String.format("%s found app user by email [%s]: %s", Service.DEBUG, email, user != null ? user.toString() : "-"));
            }
            return user;
        } catch (ServiceException ex){
            logger.warn(String.format("Exception when find app user by email: %s", ex.getMessage()), ex);
            throw ex;
        } catch (Exception ex){
            final String log = String.format("Exception when find app user by email: %s", ex.getMessage());
            logger.warn(log, ex);
            throw ServiceException.retryable(ErrorCode.CLIENT, log, ex);
        }
    }
}