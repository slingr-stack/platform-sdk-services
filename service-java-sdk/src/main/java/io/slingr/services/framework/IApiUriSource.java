package io.slingr.services.framework;

/**
 * Interface to implement by the classes that are source of URI from an external service API
 *
 * <p>Created by lefunes on 09/04/18.
 */
public interface IApiUriSource {

    /**
     * Gets the base URI used to access to the external service API
     *
     * @return URI of the external service API
     */
    String getApiUri();
}
