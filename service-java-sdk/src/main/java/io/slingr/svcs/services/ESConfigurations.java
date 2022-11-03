package io.slingr.svcs.services;

import io.slingr.svcs.Svc;
import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.exceptions.ErrorCode;
import io.slingr.svcs.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all messages related to configuration of the Extension Broker exchanged with the Extension Broker
 *
 * <p>Created by lefunes on 20/03/18.
 */
public class ESConfigurations {
    private static final Logger logger = LoggerFactory.getLogger(Scripts.class);

    private final ExtensionBrokerApi api;
    private final boolean debug;

    /**
     * Constructor only can be called by Extension Broker class
     *
     * @param api extension broker implementation
     * @param debug true if the service shows information useful for debug
     */
    public ESConfigurations(ExtensionBrokerApi api, boolean debug) {
        this.api = api;
        this.debug = debug;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Get configuration
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Receive the configuration about the Extension Broker .
     *
     * <p>This method is used by the service to know if it is working directly with Extension Broker or through a
     * Proxy
     * service.
     *
     * @return a json map that contains configuration and metadata about the service
     * @throws SvcException if there is an issue with the exchange
     */
    public Json get() {
        if(debug) {
            logger.info(String.format("%s getting Extension Broker configuration", Svc.DEBUG));
        }
        try {
            final Json response = api.getConfiguration();
            if(debug) {
                logger.info(String.format("%s received Extension Broker configuration [%s]", Svc.DEBUG, response != null ? response.toString() : "-"));
            }
            return response;
        } catch (SvcException ex){
            logger.warn(String.format("Exception when get Extension Broker configuration: %s", ex.getMessage()), ex);
            throw ex;
        } catch (Exception ex){
            final String log = String.format("Exception when get Extension Broker configuration: %s", ex.getMessage());
            logger.warn(log, ex);
            throw SvcException.retryable(ErrorCode.CLIENT, log, ex);
        }
    }

}
