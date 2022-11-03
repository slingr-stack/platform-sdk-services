package io.slingr.svcs.services;

import io.slingr.svcs.Svc;
import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.exceptions.ErrorCode;
import io.slingr.svcs.utils.converters.JsonSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all messages related to configuration scripts exchanged with the Extension Broker
 *
 * <p>Created by lefunes on 19/03/18.
 */
public class Scripts {
    private static final Logger logger = LoggerFactory.getLogger(Scripts.class);

    private static final int SYNC_MAX_RETRIES = 5;
    private static final int SYNC_SECONDS_BETWEEN_RETRIES = 5;

    private final ExtensionBrokerApi api;
    private final boolean debug;

    /**
     * Constructor only can be called by Extension Broker class
     *
     * @param api extension broker implementation
     * @param debug true if the service shows information useful for debug
     */
    public Scripts(ExtensionBrokerApi api, boolean debug) {
        this.api = api;
        this.debug = debug;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Execute scripts
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Sends a configuration script execution request to the Extension Broker app and waits a response.
     *
     * @param scriptName Name of the execution script. This must be a valid script name defined on the service
     *                   configuration.
     * @return response from application
     * @throws SvcException if there is an issue with the exchange
     */
    public Object execute(final String scriptName) throws SvcException {
        return execute(null, scriptName, null, null);
    }

    /**
     * Sends a configuration script execution request to the Extension Broker app and waits a response.
     *
     * @param scriptName Name of the execution script. This must be a valid script name defined on the service
     *                   configuration.
     * @param parameters Parameters of the script
     * @return response from application
     * @throws SvcException if there is an issue with the exchange
     */
    public Object execute(final String scriptName, final Object parameters) throws SvcException {
        return execute(null, scriptName, parameters, null);
    }

    /**
     * Sends a configuration script execution request to the Extension Broker app and waits a response.
     *
     * @param date Timestamp that represents the moment when the execution request was generated. It takes the value of
     *             the milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param scriptName Name of the execution script. This must be a valid script name defined on the service
     *                   configuration.
     * @param parameters Parameters of the script
     * @return response from application
     * @throws SvcException if there is an issue with the exchange
     */
    public Object execute(Long date, final String scriptName, final Object parameters) throws SvcException {
        return execute(date, scriptName, parameters, null);
    }

    /**
     * Sends a configuration script execution request to the Extension Broker app and waits a response.
     *
     * @param date Timestamp that represents the moment when the execution request was generated. It takes the value of
     *             the milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param scriptName Name of the execution script. This must be a valid script name defined on the service
     *                   configuration.
     * @param parameters Parameters of the script
     * @param retries max retries to execute before to discard the message
     * @return response from application
     * @throws SvcException if there is an issue with the exchange
     */
    public Object execute(Long date, final String scriptName, final Object parameters, Integer retries) throws SvcException {
        ExtensionBroker.isNotBlank(scriptName, "Empty configuration script");
        if(date == null){
            date = System.currentTimeMillis();
        }
        if(retries == null){
            retries = SYNC_MAX_RETRIES;
        }
        if(retries < 0){
            retries = 0;
        }

        if(debug) {
            logger.info(String.format("%s executing script [%s].", Svc.DEBUG, scriptName));
        }
        return executeScript(date, scriptName, parameters, retries, retries);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // helper methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Tries to send the configuration script request to the Extension Broker app.
     * If it fails, the next try will be executed on i*SECONDS_BETWEEN_RETRIES seconds, where 'i' is the next try number
     *
     * @param date Timestamp that represents the moment when the execution request was generated. It takes the value of
     *             the milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param scriptName Name of the execution script. This must be a valid script name defined on the service
     *                   configuration.
     * @param parameters Parameters of the script
     * @param retries resting retries before to discard the message
     * @param maxRetries max retries to execute before to discard the message
     * @return response from application
     * @throws SvcException if there is an issue with the exchange
     */
    private Object executeScript(final long date, final String scriptName, final Object parameters, int retries, int maxRetries) throws SvcException {
        final String script = StringUtils.isNotBlank(scriptName)  ? scriptName : "-";

        Object response = null;
        SvcException lastError = null;
        try {
            response = api.executeScript(date, scriptName, parameters);
        } catch (SvcException ex) {
            final String message = ex.getMessage();
            if(message != null && message.startsWith("Exception when try to send configuration script request")){
                logger.warn(ex.getMessage());
            } else {
                logger.warn(String.format("Exception when try to send configuration script request [%s]: %s", script, ex.getMessage()));
            }
            if(ex.isRetryable()) {
                lastError = ex;
            } else {
                logger.warn(message, ex);
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
                    logger.info(String.format("Retrying configuration script [%s], retry [%s/%s]", script, (maxRetries-newRetries)+1, maxRetries));
                    response = executeScript(date, scriptName, parameters, newRetries, maxRetries);
                } else {
                    lastError = SvcException.permanent(ErrorCode.CLIENT, String.format("Properties script can not be sent to application [%s]", script));
                }
            }
            if(response == null && lastError != null) {
                // response still be null after retries
                logger.warn(lastError.getMessage(), lastError);
                throw lastError;
            }
        } else {
            if(debug) {
                logger.info(String.format("%s script [%s] executed - response [%s]", Svc.DEBUG, script, response instanceof JsonSource ? ((JsonSource) response).toJson() : response.toString()));
            }
        }
        return response;
    }
}
