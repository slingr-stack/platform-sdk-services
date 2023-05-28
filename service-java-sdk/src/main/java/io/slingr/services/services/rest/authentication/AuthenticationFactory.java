package io.slingr.services.services.rest.authentication;

import io.slingr.services.services.rest.authentication.strategy.*;

import java.util.Map;

public class AuthenticationFactory {
    public static AuthenticationStrategy createAuthenticationStrategy(AuthenticationType type, Map<String, String> params) {
        switch (type) {
            case BASIC:
                return new BasicAuthenticationStrategy(params);
            case OAUTH_TWITTER:
                return new OAuthTwitterAuthenticationStrategy(params);
            case OAUTH2:
                return new OAuth2AuthenticationStrategy(params);
            case OAUTH:
                return new OAuthAuthenticationStrategy(params);
            case DIGEST:
                return new DigestAuthenticationStrategy(params);
            default:
                throw new IllegalArgumentException("Invalid authentication type: " + type);
        }
    }
}
