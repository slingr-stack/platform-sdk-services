package io.slingr.services.services.rest.authentication;

import io.slingr.services.services.rest.authentication.strategy.AuthenticationStrategy;

import javax.ws.rs.client.Client;
import java.util.Map;

public class AuthenticationService {
    private AuthenticationStrategy strategy;

    public void setupAuthentication(AuthenticationType type, Map<String, String> params) {
        strategy = AuthenticationFactory.createAuthenticationStrategy(type, params);
    }

    public void addAuthentication(Client webTarget) {
        strategy.addAuthentication(webTarget);
    }
}
