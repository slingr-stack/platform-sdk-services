package io.slingr.services.services.rest.authentication;

import io.slingr.services.services.rest.authentication.strategy.AuthenticationStrategy;

import javax.ws.rs.client.Client;
import java.util.Map;

/**
 * This class is responsible for setting up and adding authentication to a {@link Client} instance.
 */
public class AuthenticationService {
    private AuthenticationStrategy strategy;

    /**
     * Sets up the authentication strategy based on the specified authentication type and parameters.
     *
     * @param type   The type of authentication.
     * @param params The parameters required for authentication.
     */
    public void setupAuthentication(AuthenticationType type, Map<String, String> params) {
        strategy = AuthenticationFactory.createAuthenticationStrategy(type, params);
    }

    /**
     * Adds authentication to the specified {@link Client} instance using the configured authentication strategy.
     *
     * @param client The client to which authentication should be added.
     */
    public void addAuthentication(Client client) {
        strategy.addAuthentication(client);
    }
}
