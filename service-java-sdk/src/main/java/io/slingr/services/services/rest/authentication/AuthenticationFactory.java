package io.slingr.services.services.rest.authentication;

import io.slingr.services.services.rest.authentication.strategy.*;

import java.util.Map;

/**
 * Factory class for creating instances of {@link AuthenticationStrategy} based on the specified {@link AuthenticationType}.
 * <p>
 * This class also provides the ability to add a new authentication method to the system.
 * To add a new authentication method, follow these steps:
 * </p>
 * <ol>
 * <li>Create a new enum constant in {@link AuthenticationType} representing the new authentication method.</li>
 * <li>Create a new class and implement the corresponding {@link AuthenticationStrategy} for the new authentication method.</li>
 * <li>Update the {@code createAuthenticationStrategy} method to handle the new authentication type by adding a case statement for the new constant in the switch block.</li>
 * </ol>
 */
public class AuthenticationFactory {

    /**
     * Creates an instance of {@link AuthenticationStrategy} based on the specified {@link AuthenticationType}.
     *
     * @param type   The type of authentication.
     * @param params The parameters required for authentication.
     * @return The created {@link AuthenticationStrategy} instance.
     * @throws IllegalArgumentException If the specified authentication type is invalid.
     */
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
