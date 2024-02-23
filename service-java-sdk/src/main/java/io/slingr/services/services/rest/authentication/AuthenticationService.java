package io.slingr.services.services.rest.authentication;

import io.slingr.services.services.rest.HttpRequest;
import io.slingr.services.services.rest.authentication.strategy.AuthenticationStrategy;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import java.util.Map;

/**
 * This class is responsible for setting up and adding authentication to a {@link Client} instance.
 */
public class AuthenticationService {
    private AuthenticationStrategy strategy;

    /**
     * Sets up the authentication strategy based on the specified authentication type and parameters.
     *
     * @param request The request containing the authorization information.
     */
    public void setupAuthentication(HttpRequest request) {
        Map<String, String> params = request.getAuthorization().toMapString();
        AuthenticationType authenticationType = AuthenticationType.fromType(params);
        strategy = AuthenticationFactory.createAuthenticationStrategy(authenticationType, params);
    }

    /**
     * Adds authentication to the specified {@link Client} instance using the configured authentication strategy.
     *
     * @param client The client to which authentication should be added.
     */
    public void addAuthentication(Client client, WebTarget apiTarget, HttpRequest request) {
        strategy.addAuthentication(client, apiTarget, request);
    }
}
