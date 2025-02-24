package io.slingr.services.services.rest.authentication.strategy;

import io.slingr.services.services.rest.HttpRequest;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import java.util.Map;

public class OAuth2AuthenticationStrategy implements AuthenticationStrategy {

    private final String accessToken;
    private final String headerPrefix;

    public OAuth2AuthenticationStrategy(Map<String, String> params) {
        this.accessToken = params.get("accessToken");
        this.headerPrefix  = params.get("headerPrefix");
    }

    @Override
    public void addAuthentication(Client client, WebTarget apiTarget, HttpRequest request) {
        client.register((ClientRequestFilter) requestContext -> requestContext.getHeaders().add("Authorization", headerPrefix +" " + accessToken));
    }
}