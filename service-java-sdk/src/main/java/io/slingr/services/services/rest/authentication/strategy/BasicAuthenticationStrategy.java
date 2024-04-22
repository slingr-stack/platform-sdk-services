package io.slingr.services.services.rest.authentication.strategy;

import io.slingr.services.services.rest.HttpRequest;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import java.util.Map;

public class BasicAuthenticationStrategy implements AuthenticationStrategy {
    private final String username;
    private final String password;

    public BasicAuthenticationStrategy(Map<String, String> params) {
        this.username = params.get("username");
        this.password = params.get("password");
    }

    @Override
    public void addAuthentication(Client client, WebTarget apiTarget, HttpRequest request) {
        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(username, password);
        client.register(feature);
    }

}