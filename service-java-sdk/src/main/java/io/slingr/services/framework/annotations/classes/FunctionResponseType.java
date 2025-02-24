package io.slingr.services.framework.annotations.classes;

import io.slingr.services.framework.annotations.ServiceFunction;

/**
 * Type of function response defined on a method defined as {@link ServiceFunction}
 *
 * <p>Created by lefunes on 04/11/16.
 */
public enum FunctionResponseType {
    VOID,
    JSON,
    OTHER
}