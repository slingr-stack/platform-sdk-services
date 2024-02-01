package io.slingr.services.services;

import io.slingr.services.Service;
import io.slingr.services.configurations.ServiceContext;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.utils.converters.JsonSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages all messages related to events exchanged with the Extension Broker
 *
 * <p>Created by lefunes on 19/03/18.
 */
public class Events {
    private static final Logger logger = LoggerFactory.getLogger(Events.class);

    private static final int SYNC_MAX_RETRIES = 5;
    private static final int SYNC_SECONDS_BETWEEN_RETRIES = 5;
    private static final int ASYNC_MAX_RETRIES = 20;
    private static final int ASYNC_SECONDS_BETWEEN_RETRIES = 5;

    private final ExtensionBrokerApi api;
    private final boolean debug;

    /**
     * Constructor only can be called by Extension Broker class
     *
     * @param api   extension broker implementation
     * @param debug true if the service shows information useful for debug
     */
    public Events(ExtensionBrokerApi api, boolean debug) {
        this.api = api;
        this.debug = debug;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Events
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sends a service event to the Extension Broker app.
     *
     * @param event Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *              appService.json file)
     * @throws ServiceException if there is an issue with the exchange
     */
    public void send(String event) throws ServiceException {
        send(null, event, null, null, null, null, null);
    }

    /**
     * Sends a service event to the Extension Broker app.
     *
     * @param event Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *              appService.json file)
     * @param data  Information related to the event that we pretend to send to the application
     * @throws ServiceException if there is an issue with the exchange
     */
    public void send(String event, Object data) throws ServiceException {
        send(null, event, data, null, null, null, null);
    }

    /**
     * Sends a service event to the Extension Broker specifying the target app. This is being used in shared @ServiceUserDataStore.
     *
     * @param event Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *              appService.json file)
     * @param app   the app to send event. This is being used for in shared services.
     * @param env   the env to send event. This is being used for in shared services.
     * @param data  Information related to the event that we pretend to send to the application
     * @throws ServiceException if there is an issue with the exchange
     */
    public void send(String event, String app, String env, Object data) throws ServiceException {
        send(null, event, data, null, null, null, app, env, null);
    }

    /**
     * Sends a service event to the Extension Broker app.
     *
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @throws ServiceException if there is an issue with the exchange
     */
    public void send(String event, Object data, String fromFunctionId) throws ServiceException {
        send(null, event, data, fromFunctionId, null, null, null);
    }

    /**
     * Sends a service event to the Extension Broker app.
     *
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param app            the app to send event. This is being used for in shared services.
     * @param env            the env to send event. This is being used for in shared services.
     * @throws ServiceException if there is an issue with the exchange
     */
    public void send(String event, String app, String env, Object data, String fromFunctionId) throws ServiceException {
        send(null, event, data, fromFunctionId, null, null, app, env, null);
    }

    /**
     * Sends a service event to the Extension Broker app.
     *
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event.
     * @throws ServiceException if there is an issue with the exchange
     */
    public void send(String event, Object data, String fromFunctionId, String userId) throws ServiceException {
        send(null, event, data, fromFunctionId, userId, null, null);
    }

    /**
     * Sends a service event to the Extension Broker app.
     *
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event. When we know this information, this is the preferred
     *                       method for identifying a user
     * @param userEmail      Email of the user that generates the event. This is an alternative method for identifying a
     *                       user. Services can send both at the same time but the application starts to search the user by
     *                       id.
     * @throws ServiceException if there is an issue with the exchange
     */
    public void send(String event, Object data, String fromFunctionId, String userId, String userEmail) throws ServiceException {
        send(null, event, data, fromFunctionId, userId, userEmail, null);
    }

    /**
     * Sends a service event to the Extension Broker app.
     *
     * @param date           Timestamp that represents the moment when the event was generated. It takes the value of the
     *                       milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event. When we know this information, this is the preferred
     *                       method for identifying a user
     * @param userEmail      Email of the user that generates the event. This is an alternative method for identifying a
     *                       user. Services can send both at the same time but the application starts to search the user by
     *                       id.
     * @throws ServiceException if there is an issue with the exchange
     */
    public void send(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws ServiceException {
        send(date, event, data, fromFunctionId, userId, userEmail, null);
    }

    /**
     * Sends a service event to the Extension Broker app.
     *
     * @param date           Timestamp that represents the moment when the event was generated. It takes the value of the
     *                       milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event. When we know this information, this is the preferred
     *                       method for identifying a user
     * @param userEmail      Email of the user that generates the event. This is an alternative method for identifying a
     *                       user. Services can send both at the same time but the application starts to search the user by
     *                       id.
     * @param retries        max retries to execute before to discard the message
     * @throws ServiceException if there is an issue with the exchange
     */
    public void send(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail, Integer retries) throws ServiceException {
        send(date, event, data, fromFunctionId, userId, userEmail, null, null, retries);
    }

    /**
     * Sends a service event to the Extension Broker app.
     *
     * @param date           Timestamp that represents the moment when the event was generated. It takes the value of the
     *                       milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event. When we know this information, this is the preferred
     *                       method for identifying a user
     * @param userEmail      Email of the user that generates the event. This is an alternative method for identifying a
     *                       user. Services can send both at the same time but the application starts to search the user by
     *                       id.
     * @param app            the app to send event. This is being used for in shared services.
     * @param env            the env to send event. This is being used for in shared services.
     * @param retries        max retries to execute before to discard the message
     * @throws ServiceException if there is an issue with the exchange
     */
    public void send(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail, String app, String env, Integer retries) throws ServiceException {
        ExtensionBroker.isNotBlank(event, "empty event name");
        if (date == null) {
            date = System.currentTimeMillis();
        }
        if (retries == null) {
            retries = ASYNC_MAX_RETRIES;
        }
        if (retries < 0) {
            retries = 0;
        }
        sendAsyncEvent(date, event, data, fromFunctionId, userId, userEmail, app, env, retries, retries);
    }

    /**
     * Sends a service event to the Extension Broker app and waits a JSON response.
     *
     * @param event Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *              appService.json file)
     * @return response from application
     * @throws ServiceException if there is an issue with the exchange
     */
    public Object sendSync(String event) throws ServiceException {
        return sendSync(null, event, null, null, null, null, null);
    }

    /**
     * Sends a service event to the Extension Broker app and waits a JSON response.
     *
     * @param event Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *              appService.json file)
     * @param data  Information related to the event that we pretend to send to the application
     * @return response from application
     * @throws ServiceException if there is an issue with the exchange
     */
    public Object sendSync(String event, Object data) throws ServiceException {
        return sendSync(null, event, data, null, null, null, null);
    }

    /**
     * Sends a service event to the Extension Broker specifying the target app. This is being used in shared services.
     *
     * @param event Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *              appService.json file)
     * @param app   the app to send event. This is being used for in shared services.
     * @param env   the env to send event. This is being used for in shared services.
     * @param data  Information related to the event that we pretend to send to the application
     * @throws ServiceException if there is an issue with the exchange
     */
    public Object sendSync(String event, String app, String env, Object data) throws ServiceException {
        return sendSync(null, event, data, null, null, null, app, env, null);
    }

    /**
     * Sends a service event to the Extension Broker app and waits a JSON response.
     *
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     */
    public Object sendSync(String event, Object data, String fromFunctionId) throws ServiceException {
        return sendSync(null, event, data, fromFunctionId, null, null, null);
    }

    /**
     * Sends a service event to the Extension Broker app and waits a JSON response.
     *
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event.
     * @return response from application
     * @throws ServiceException if there is an issue with the exchange
     */
    public Object sendSync(String event, Object data, String fromFunctionId, String userId) throws ServiceException {
        return sendSync(null, event, data, fromFunctionId, userId, null, null);
    }

    /**
     * Sends a service event to the Extension Broker app and waits a JSON response.
     *
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event. When we know this information, this is the preferred
     *                       method for identifying a user
     * @param userEmail      Email of the user that generates the event. This is an alternative method for identifying a
     *                       user. Services can send both at the same time but the application starts to search the user by
     *                       id.
     * @return response from application
     * @throws ServiceException if there is an issue with the exchange
     */
    public Object sendSync(String event, Object data, String fromFunctionId, String userId, String userEmail) throws ServiceException {
        return sendSync(null, event, data, fromFunctionId, userId, userEmail, null);
    }

    /**
     * Sends a service event to the Extension Broker app and waits a JSON response.
     *
     * @param date           Timestamp that represents the moment when the event was generated. It takes the value of the
     *                       milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event. When we know this information, this is the preferred
     *                       method for identifying a user
     * @param userEmail      Email of the user that generates the event. This is an alternative method for identifying a
     *                       user. Services can send both at the same time but the application starts to search the user by
     *                       id.
     * @return response from application
     * @throws ServiceException if there is an issue with the exchange
     */
    public Object sendSync(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail) throws ServiceException {
        return sendSync(date, event, data, fromFunctionId, userId, userEmail, null);
    }

    /**
     * Sends a service event to the Extension Broker app and waits a JSON response.
     *
     * @param date           Timestamp that represents the moment when the event was generated. It takes the value of the
     *                       milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event. When we know this information, this is the preferred
     *                       method for identifying a user
     * @param userEmail      Email of the user that generates the event. This is an alternative method for identifying a
     *                       user. Services can send both at the same time but the application starts to search the user by
     *                       id.
     * @param retries        max retries to execute before to discard the message
     * @return response from application
     * @throws ServiceException if there is an issue with the exchange
     */
    public Object sendSync(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail, Integer retries) throws ServiceException {
        return sendSync(date, event, data, fromFunctionId, userId, userEmail, null, null, retries);
    }


    /**
     * Sends a service event to the Extension Broker app and waits a JSON response.
     *
     * @param date           Timestamp that represents the moment when the event was generated. It takes the value of the
     *                       milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event. When we know this information, this is the preferred
     *                       method for identifying a user
     * @param userEmail      Email of the user that generates the event. This is an alternative method for identifying a
     *                       user. Services can send both at the same time but the application starts to search the user by
     *                       id.
     * @param app            the app to send event. This is being used for in shared services.
     * @param env            the env to send event. This is being used for in shared services.
     * @param retries        max retries to execute before to discard the message
     * @return response from application
     * @throws ServiceException if there is an issue with the exchange
     */
    public Object sendSync(Long date, String event, Object data, String fromFunctionId, String userId, String userEmail, String app, String env, Integer retries) throws ServiceException {
        ExtensionBroker.isNotBlank(event, "empty event name");
        if (date == null) {
            date = System.currentTimeMillis();
        }
        if (retries == null) {
            retries = SYNC_MAX_RETRIES;
        }
        if (retries < 0) {
            retries = 0;
        }

        if (debug) {
            logger.info(String.format("%s Sending sync event [%s] [%s]", Service.DEBUG, event, data == null ? "-" : (data instanceof String || data instanceof JsonSource ? data.toString() : data.getClass().getName())));
        }
        return sendSyncEvent(date, event, data, fromFunctionId, userId, userEmail, app, env, retries, retries);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // helper methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Tries to send the event to the Extension Broker app.
     * If it fails, the next try will be executed on i*SECONDS_BETWEEN_RETRIES seconds, where 'i' is the next try number
     *
     * @param date           Timestamp that represents the moment when the event was generated. It takes the value of the
     *                       milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event. When we know this information, this is the preferred
     *                       method for identifying a user
     * @param userEmail      Email of the user that generates the event. This is an alternative method for identifying a
     *                       user. Services can send both at the same time but the application starts to search the user by
     *                       id.
     * @param app            the app to send event. This is being used for in shared services.
     * @param env            the env to send event. This is being used for in shared services.
     * @param retries        resting retries before to discard the message
     * @param maxRetries     max retries to execute before to discard the message
     */
    private void sendAsyncEvent(final long date, final String event, final Object data, final String fromFunctionId, final String userId, final String userEmail, String app, String env, int retries, final int maxRetries) {
        boolean received = false;
        final String eventName = StringUtils.isNotBlank(event) ? event : "-";
        /*String originalApp = ServiceContext.getCurrentApp();
        String originalEnv = ServiceContext.getCurrentEnv();*/
        ServiceContext.initContext(app, env);
        try {
            try {
                if (retries == maxRetries) {
                    if (debug) {
                        logger.info(String.format("%s Sending async event [%s] [%s]", Service.DEBUG, eventName, data == null ? "-" : (data instanceof String || data instanceof JsonSource ? data.toString() : data.getClass().getName())));
                    }
                } else {
                    logger.info(String.format("Retrying async event [%s], retry [%s/%s]", eventName, (maxRetries - retries) + 1, maxRetries));
                }
                api.newEvent(date, event, data, fromFunctionId, userId, userEmail);

                if (debug) {
                    logger.info(String.format("%s Async event [%s] sent", Service.DEBUG, eventName));
                }
                received = true;
            } catch (ServiceException ex) {
                logger.warn(String.format("Exception when try to send async event [%s]: %s", eventName, ex.getMessage()));
            } catch (Exception ex) {
                logger.warn(String.format("Exception when try to send async event [%s]: %s", eventName, ex.getMessage()), ex);
            }
            if (!received && retries > 0) {
                // retry the message
                if (retries > ASYNC_MAX_RETRIES) {
                    retries = ASYNC_MAX_RETRIES;
                }
                final int newRetries = retries - 1;
                if (newRetries > 0) {
                    Executors.newSingleThreadScheduledExecutor().schedule(() -> Events.this.sendAsyncEvent(date, event, data, fromFunctionId, userId, userEmail, app, env, newRetries, maxRetries),
                            ASYNC_SECONDS_BETWEEN_RETRIES * (1 + ASYNC_MAX_RETRIES - retries),
                            TimeUnit.SECONDS
                    );
                } else {
                    logger.warn(String.format("Async event can not be sent to application [%s]", eventName));
                }
            }
        } finally {
            /*ServiceContext.setCurrentApp(originalApp);
            ServiceContext.setCurrentEnv(originalEnv);*/
        }
    }

    /**
     * Tries to send the event to the Extension Broker app.
     * If it fails, the next try will be executed on i*SECONDS_BETWEEN_RETRIES seconds, where 'i' is the next try number
     *
     * @param date           Timestamp that represents the moment when the event was generated. It takes the value of the
     *                       milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param event          Name of the event. This must be a valid event name (an event name is valid when is declared on the
     *                       appService.json file)
     * @param data           Information related to the event that we pretend to send to the application
     * @param fromFunctionId Id of a function related to the event. It is useful to permit to known to the application
     *                       if the event is a callback of a previous executed function
     * @param userId         Id of the user that generates the event. When we know this information, this is the preferred
     *                       method for identifying a user
     * @param userEmail      Email of the user that generates the event. This is an alternative method for identifying a
     *                       user. Services can send both at the same time but the application starts to search the user by
     *                       id.
     * @param app            the app to send event. This is being used for in shared services.
     * @param env            the env to send event. This is being used for in shared services.
     * @param retries        resting retries before to discard the message
     * @param maxRetries     max retries to execute before to discard the message
     * @return response from application
     * @throws ServiceException if there is an issue with the exchange
     */
    private Object sendSyncEvent(final long date, final String event, final Object data, final String fromFunctionId, final String userId, final String userEmail, String app, String env, int retries, final int maxRetries) throws ServiceException {
        ServiceContext.initContext(app, env);
        final String eventName = StringUtils.isNotBlank(event) ? event : "-";
        Object response = null;
        ServiceException lastError = null;
        try {
            try {
                response = api.newSyncEvent(date, event, data, fromFunctionId, userId, userEmail);
            } catch (ServiceException rex) {
                throw rex;
            } catch (Exception ex) {
                throw ServiceException.retryable(ErrorCode.CLIENT, String.format("Exception when try to send sync event [%s]: %s", eventName, ex.getMessage()), ex);
            }
        } catch (ServiceException ex) {
            logger.warn(String.format("Exception when try to send sync event [%s]: %s", eventName, ex.getMessage()));
            if (ex.isRetryable()) {
                lastError = ex;
            } else {
                logger.warn(ex.getMessage());
                throw ex;
            }
        }

        if (response == null) {
            if (retries > 0) {
                // retry the message
                if (retries > SYNC_MAX_RETRIES) {
                    retries = SYNC_MAX_RETRIES;
                }
                final int newRetries = retries - 1;
                if (newRetries > 0) {
                    try {
                        Thread.sleep(SYNC_SECONDS_BETWEEN_RETRIES * (1 + SYNC_MAX_RETRIES - retries) * 1000);
                    } catch (InterruptedException e) {
                        //do nothing
                    }
                    logger.info(String.format("Retrying sync event [%s], retry [%s/%s]", eventName, (maxRetries - newRetries) + 1, maxRetries));
                    response = sendSyncEvent(date, event, data, fromFunctionId, userId, userEmail, app, env, newRetries, maxRetries);
                } else {
                    lastError = ServiceException.permanent(ErrorCode.CLIENT, String.format("Sync event can not be sent to application [%s]", event));
                }
            }
            if (response == null && lastError != null) {
                logger.warn(lastError.getMessage(), lastError);
                throw lastError;
            }
        } else {
            if (debug) {
                logger.info(String.format("%s Sync event [%s] sent - response [%s]", Service.DEBUG, eventName, response instanceof JsonSource ? ((JsonSource) response).toJson() : response.toString()));
            }
        }
        return response;
    }

}
