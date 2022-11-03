package io.slingr.svcs.framework.annotations.classes;

import io.slingr.svcs.framework.annotations.ServiceWebService;

/**
 * Type of web service response defined on a method defined as {@link ServiceWebService}
 *
 * <p>Created by lefunes on 11/11/16.
 */
public enum WebServiceResponseType {
    VOID,
    STRING,
    OBJECT,
    JSON,
    RESPONSE
}
