package io.slingr.services.services.rest.authentication.strategy;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import java.util.Map;

public class DigestAuthenticationStrategy implements AuthenticationStrategy {
    private String username;
    private String password;

    public DigestAuthenticationStrategy(Map<String, String> params) {
        this.username = params.get("username");
        this.password = params.get("password");
    }

    @Override
    public void addAuthentication(Client client) {
        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.digest(username, password);
        client.register(feature);
    }
}
