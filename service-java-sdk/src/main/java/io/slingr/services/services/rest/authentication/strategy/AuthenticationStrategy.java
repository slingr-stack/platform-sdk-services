package io.slingr.services.services.rest.authentication.strategy;

import javax.ws.rs.client.Client;

public interface AuthenticationStrategy {
    void addAuthentication(Client webTarget);
    
}
