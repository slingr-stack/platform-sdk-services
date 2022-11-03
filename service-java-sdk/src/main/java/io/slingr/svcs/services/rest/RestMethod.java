package io.slingr.svcs.services.rest;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * <p>Valid request methods to perform in the RestClient
 *
 * <p>Created by lefunes on 14/05/15.
 */
public enum RestMethod {
    GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS;

    /**
     * Gets the initial chars of the rest method name
     *
     * @return initial chars of the rest method name
     */
    public String toInitials() {
        return name().substring(0,2);
    }

    /**
     * Returns the RestMethod enum constant with the specified method. The string must match exactly an identifier used
     * to declare an RestMethod enum constant. A null value is returned if this enum type has no constant with the
     * specified method name.
     *
     * @param method value to check
     * @return the RestMethod enum constant with the specified method, or null if this enum type has no constant with
     * the specified method name.
     */
    public static RestMethod fromString(String method) {
        if(StringUtils.isNotBlank(method)) {
            try {
                return valueOf(method.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Returns the RestMethod enum constant with the specified method. The string must match exactly an identifier used
     * to declare an RestMethod enum constant. GET value is returned if this enum type has no constant with the
     * specified method name.
     *
     * @param method value to check
     * @return the string of the specified method, or GET if this enum type has no constant with the specified name
     */
    public static String checkStringValue(String method) {
        RestMethod val = fromString(method);
        if(val == null){
            val = GET;
        }
        return val.name();
    }

    /**
     * Returns the list of methods.
     *
     * @return list of methods
     */
    public static List<RestMethod> all(){
        return Arrays.asList(values());
    }
}
