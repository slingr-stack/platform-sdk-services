package io.slingr.services.services.rest.authentication.strategy;

import io.slingr.services.services.rest.HttpRequest;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

/**
 * The AuthenticationStrategy interface defines the contract for adding authentication to a {@link Client} instance.
 */
public interface AuthenticationStrategy {

    /**
     * Adds authentication to the specified {@link Client} instance.
     *
     * @param client  The client to which authentication should be added.
     * @param request
     */
    void addAuthentication(Client client, WebTarget apiTarget, HttpRequest request);

}
