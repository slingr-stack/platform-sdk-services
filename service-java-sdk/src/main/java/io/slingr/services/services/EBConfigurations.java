package io.slingr.services.services;

import io.slingr.services.Service;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all messages related to configuration of the Extension Broker exchanged with the Extension Broker
 *
 */
public class EBConfigurations {
    private static final Logger logger = LoggerFactory.getLogger(EBConfigurations.class);

    private final ExtensionBrokerApi api;
    private final boolean debug;

    /**
     * Constructor only can be called by Extension Broker class
     *
     * @param api extension broker implementation
     * @param debug true if the service shows information useful for debug
     */
    public EBConfigurations(ExtensionBrokerApi api, boolean debug) {
        this.api = api;
        this.debug = debug;
    }

    /**
     * Receive the configuration about the Extension Broker .
     *
     * <p>This method is used by the service to know if it is working directly with Extension Broker or through a
     * Proxy service.
     *
     * @return a json map that contains configuration and metadata about the service
     * @throws ServiceException if there is an issue with the exchange
     */
    public Json get() {
        if(debug) {
            logger.info(String.format("%s getting Extension Broker configuration", Service.DEBUG));
        }
        try {
            final Json response = api.getConfiguration();
            if(debug) {
                logger.info(String.format("%s received Extension Broker configuration [%s]", Service.DEBUG, response != null ? response.toString() : "-"));
            }
            return response;
        } catch (ServiceException ex){
            logger.warn(String.format("Exception when get Extension Broker configuration: [%s]", ex.getMessage()), ex);
            throw ex;
        } catch (Exception ex){
            final String log = String.format("Exception when get Extension Broker configuration: [%s]", ex.getMessage());
            logger.warn(log, ex);
            throw ServiceException.retryable(ErrorCode.CLIENT, log, ex);
        }
    }
}