package io.slingr.services.exceptions;

import io.slingr.services.utils.Json;

/**
 * This is an exception that can be retried. For example if the server returns a 503 it might means that it is not
 * available at the moment but will be later.
 *
 * <p>Created by dgaviola on 29/5/15.
 */
public class RetryableException extends ServiceException {

    RetryableException(ErrorCode code, String message, Json additionalInfo, Throwable cause) {
        super(code, message, additionalInfo, cause);
    }

    @Override
    public boolean isRetryable() {
        return true;
    }
}
