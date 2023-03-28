package io.slingr.services.framework.annotations.classes;

/**
 * Interface to implement by elements that can be set on the service like properties, app loggers, etc
 *
 * <p>Created by lefunes on 19/12/16.
 */
public interface SettableProperty {
    String getAccessor();
    AccessorType getAccessorType();
}