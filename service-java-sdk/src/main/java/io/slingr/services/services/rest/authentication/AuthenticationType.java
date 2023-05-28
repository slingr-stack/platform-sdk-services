package io.slingr.services.services.rest.authentication;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public enum AuthenticationType {
    BASIC("basic"),
    OAUTH_TWITTER("oauthTwitter"),
    OAUTH2("oauth2"),
    OAUTH("oauth"),
    DIGEST("digest");

    private final String type;

    AuthenticationType(String type) {
        this.type = type;
    }

    public static AuthenticationType fromType(Map<String, String> params) {
        String type = Optional.ofNullable(params.remove("type"))
                .orElseThrow(() -> new IllegalArgumentException("Missing authentication type"));

        return Stream.of(AuthenticationType.values())
                .filter(authenticationType -> authenticationType.type.equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid authentication type: " + type));
    }
}