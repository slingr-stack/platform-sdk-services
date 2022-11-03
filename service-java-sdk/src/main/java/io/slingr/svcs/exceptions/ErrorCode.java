package io.slingr.svcs.exceptions;

import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.converters.JsonSource;

/**
 * <p>Type of error that can happen in the Services
 *
 * <p>Created by lefunes on 23/07/15.
 */
public enum ErrorCode implements JsonSource {
    ARGUMENT("argumentException", "Argument invalid"),
    API("apiException", "API exception"),
    CONVERSION("conversionException", "Conversion exception"),
    CLIENT("clientException", "Client handling exception"),
    GENERAL("general", "General exception");

    private final String code;
    private final String name;

    ErrorCode(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * Gets the code of the error
     *
     * @return code of the error
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the name of the error
     *
     * @return name of the error
     */
    public String getName() {
        return name;
    }

    @Override
    public Json toJson() {
        Json json = Json.map();
        json.set("code", code);
        json.set("name", name);
        return json;
    }

    /**
     * Returns the error code object using the given code
     *
     * @param errorCode error code to find
     * @return error code object or null
     */
    public static ErrorCode fromString(String errorCode) {
        for (ErrorCode tmpErrorCode : values()) {
            if (tmpErrorCode.getCode().equalsIgnoreCase(errorCode)) {
                return tmpErrorCode;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, code);
    }
}