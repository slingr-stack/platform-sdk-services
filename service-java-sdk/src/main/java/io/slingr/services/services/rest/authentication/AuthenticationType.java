package io.slingr.services.services.rest.authentication;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The AuthenticationType enum represents the types of authentication supported.
 */
public enum AuthenticationType {
    BASIC("basic"),
    OAUTH_TWITTER("oauthTwitter"),
    OAUTH2("oauth2"),
    OAUTH("oauth"),
    DIGEST("digest");

    private final String type;

    /**
     * Constructs an AuthenticationType with the specified type.
     *
     * @param type The type of authentication.
     */
    AuthenticationType(String type) {
        this.type = type;
    }

    /**
     * Returns the AuthenticationType based on the given parameters.
     *
     * @param params The parameters containing the authentication type.
     * @return The corresponding AuthenticationType.
     * @throws IllegalArgumentException if the authentication type is missing or invalid.
     */
    public static AuthenticationType fromType(Map<String, String> params) {
        String type = Optional.ofNullable(params.remove("type"))
                .orElseThrow(() -> new IllegalArgumentException("Missing authentication type"));

        return Stream.of(AuthenticationType.values())
                .filter(authenticationType -> authenticationType.type.equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid authentication type: " + type));
    }
}