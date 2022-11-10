package io.slingr.svcs.utils.tests;

import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.utils.Json;

/**
 * Interface to be implemented by the tests that want to return custom responses when process events. The
 * behavior mimic an script on the application side.
 *
 * <p>Created by lefunes on 18/06/18.
 */
public interface MessageProcessor {

    /**
     * Process the message given
     *
     * @param message message to process
     * @return response
     * @throws SvcException if there is an issue with the processing
     */
    Object processMessage(Json message) throws SvcException;
}
