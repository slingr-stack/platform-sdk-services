package io.slingr.services.services.rest.authentication.strategy;

import javax.ws.rs.client.Client;

/**
 * The AuthenticationStrategy interface defines the contract for adding authentication to a {@link Client} instance.
 */
public interface AuthenticationStrategy {

    /**
     * Adds authentication to the specified {@link Client} instance.
     *
     * @param client The client to which authentication should be added.
     */
    void addAuthentication(Client client);

}
