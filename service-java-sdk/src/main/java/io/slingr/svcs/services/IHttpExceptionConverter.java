package io.slingr.svcs.services;

import io.slingr.svcs.exceptions.SvcException;

/**
 * Exception handlers
 *
 * <p>Created by lefunes on 10/04/18.
 */
public interface IHttpExceptionConverter {
    /**
     * Converts the exception to a generated exception when works with the external HTTP service to an {@link SvcException}
     *
     * @param exception exception to process
     * @return equivalent service exception
     */
    SvcException convertToSvcException(Exception exception);
}
