package io.slingr.services.framework.annotations.classes;

import io.slingr.services.framework.annotations.ServiceFunction;
import io.slingr.services.framework.annotations.ServiceWebService;

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
