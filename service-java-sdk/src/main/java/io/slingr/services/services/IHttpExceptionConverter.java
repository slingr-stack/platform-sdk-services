package io.slingr.services.services;

import io.slingr.services.exceptions.ServiceException;

/**
 * Exception handlers
 *
 * <p>Created by lefunes on 10/04/18.
 */
public interface IHttpExceptionConverter {
    /**
     * Converts the exception to a generated exception when works with the external HTTP service to an {@link ServiceException}
     *
     * @param exception exception to process
     * @return equivalent service exception
     */
    ServiceException convertToServiceException(Exception exception);
}
