package io.slingr.services.framework;

import io.slingr.services.services.Users;
import io.slingr.services.utils.Json;
import io.slingr.services.ws.exchange.FunctionRequest;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.framework.annotations.SlingrService;

/**
 * Interface that the PER USER {@link SlingrService} must be implemented
 *
 * <p>Created by lefunes on 12/03/18.
 */
public interface IPerUserService {

    /**
     * Returns the manager over all messages related to users
     *
     * @return users manager
     */
    Users users();

    /**
     * Allows to connect a user externally. For example the OAuth process could be done by an external app
     * and just pass the tokens to these methods.
     *
     * @param request the request that should contain the service's token in headers and the config in the body
     * @return the user configuration
     */
    Json defaultExternalConnectUser(WebServiceRequest request);

    /**
     * Allows to disconnect a user externally.
     *
     * @param request the request that should contain the service's token in headers
     */
    void defaultExternalDisconnectUser(WebServiceRequest request);

    /**
     * Connect user function: default implementation for the 'connect user' request processor.
     * Generates a 'user connected' or 'user disconnected' event as result
     *
     * @return the user configuration
     */
    Json defaultMethodConnectUsers(FunctionRequest request);

    /**
     * Disconnect user: default implementation for the 'disconnect user' request processor. Generates a
     * 'user disconnected' event as result
     */
    void defaultMethodDisconnectUsers(FunctionRequest request);
}