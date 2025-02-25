package io.slingr.services.services.rest.authentication.strategy;

import io.slingr.services.services.rest.HttpRequest;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import java.net.URI;
import java.util.Map;

public class OAuthAuthenticationStrategy implements AuthenticationStrategy {

    private final String consumerKey;
    private final String consumerSecret;

    public OAuthAuthenticationStrategy(Map<String, String> params) {
        this.consumerKey = params.get("consumerKey");
        this.consumerSecret = params.get("consumerSecret");
    }

    @Override
    public void addAuthentication(Client client, WebTarget apiTarget, HttpRequest request) {
        client.register((ClientRequestFilter) requestContext -> {
            String url = requestContext.getUri().toString();
            OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
            try {
                String signedUrl = consumer.sign(url);
                requestContext.setUri(URI.create(signedUrl));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid authentication: " + e);
            }
        });
    }
}