package io.slingr.svcs.framework.annotations.classes;

import io.slingr.svcs.framework.annotations.ServiceFunction;
import io.slingr.svcs.framework.annotations.ServiceWebService;

/**
 * Type of parameters defined on a method defined as {@link ServiceFunction} or {@link ServiceWebService}
 *
 * <p>Created by lefunes on 11/11/16.
 */
public enum MethodParameterType {
    NONE,
    JSON,
    STRING,
    OBJECT,
    REQUEST
}
