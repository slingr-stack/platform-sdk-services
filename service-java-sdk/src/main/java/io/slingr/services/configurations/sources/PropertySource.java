package io.slingr.services.configurations.sources;

import io.slingr.services.configurations.Configuration;

/**
 * Interface that must be implemented by the property sources used on
 * {@link Configuration}
 *
 * <p>Created by lefunes on 22/03/18.
 */
public interface PropertySource {
    /**
     * Finds in the source the property with the given property name
     *
     * @param propertyName name of the property
     * @return property value or null if this was not found
     */
    String findProperty(String propertyName);
}