package io.slingr.services.exceptions;

import io.slingr.services.utils.Json;

/**
 * This is the opposite of {@link RetryableException}. It is for errors that are permanent and the result won't change
 * it we try several times.
 *
 * <p>Created by dgaviola on 29/5/15.
 */
public class PermanentException extends ServiceException {

    PermanentException(ErrorCode code, String message, Json additionalInfo, Throwable cause) {
        super(code, message, additionalInfo, cause);
    }
}