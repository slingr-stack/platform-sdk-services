package io.slingr.svcs.framework.annotations.classes;

import io.slingr.svcs.framework.annotations.ServiceFunction;

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
